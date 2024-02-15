package name.remal.gradle_plugins.versions_retriever.git;

import static java.lang.String.format;
import static java.nio.file.Files.write;
import static java.util.Collections.singletonList;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.defaultValue;
import static name.remal.gradle_plugins.toolkit.PathUtils.deleteRecursively;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.toolkit.SneakyThrowUtils.SneakyThrowsConsumer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RetrievePreviousVersionFromGitTagActionRetrieverJava11IntegrationTest {

    final RetrievePreviousVersionFromGitTagActionRetriever retriever =
        new RetrievePreviousVersionFromGitTagActionRetriever(null);

    @TempDir
    Path repositoryPath;

    @TempDir
    Path serverRepositoryPath;
    Repository serverRepository;
    Server server;
    String serverHost;
    int serverPort;

    @BeforeEach
    void beforeEach() throws Throwable {
        serverRepository = FileRepositoryBuilder.create(serverRepositoryPath.resolve(".git").toFile());
        serverRepository.create();
        configureRepositoryConfig(serverRepository);

        server = new Server();

        val connector = new ServerConnector(server);
        connector.setHost("localhost");
        connector.setPort(0);
        server.addConnector(connector);

        val gs = new GitServlet();
        gs.setRepositoryResolver((req, name) -> {
            serverRepository.incrementOpen();
            return serverRepository;
        });

        val handler = new ServletHandler();
        handler.addServletWithMapping(new ServletHolder(gs), "/*");
        server.setHandler(handler);

        server.start();

        serverHost = defaultValue(connector.getHost(), "localhost");
        serverPort = connector.getLocalPort();
    }

    @AfterEach
    void afterEach() throws Throwable {
        server.stop();
        serverRepository.close();
    }

    @Test
    void simpleFullClone() {
        val verCommit1 = new AtomicReference<RevCommit>();
        val verCommit2 = new AtomicReference<RevCommit>();
        val verCommitQwerty = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommit1.set(addSimpleCommit(git));
            git.tag().setObjectId(verCommit1.get()).setName("ver-1").call();

            verCommit2.set(addSimpleCommit(git));
            git.tag().setObjectId(verCommit2.get()).setName("ver-2").call();

            verCommitQwerty.set(addSimpleCommit(git));
            git.tag().setObjectId(verCommitQwerty.get()).setName("ver-qwerty").call();
        });

        cloneRepository();

        val refVersion = retriever.retrieve(
            repositoryPath,
            singletonList(Pattern.compile("ver-(?<version>\\d+)"))
        );
        assertThat(refVersion).as("refVersion").isNotNull();
        assertThat(refVersion.getVersion()).as("version")
            .isEqualTo("2");
        assertThat(refVersion.getObjectId()).as("objectId")
            .isEqualTo(verCommit2.get().getId().getName());
    }

    @Test
    void cloneWithoutTags() {
        val verCommit1 = new AtomicReference<RevCommit>();
        val verCommit2 = new AtomicReference<RevCommit>();
        val verCommitQwerty = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommit1.set(addSimpleCommit(git));
            verCommit2.set(addSimpleCommit(git));
            verCommitQwerty.set(addSimpleCommit(git));
        });

        cloneRepository();

        // add tags after clone:
        withServerRepository(git -> {
            git.tag().setObjectId(verCommit1.get()).setName("ver-1").call();
            git.tag().setObjectId(verCommit2.get()).setName("ver-2").call();
            git.tag().setObjectId(verCommitQwerty.get()).setName("ver-qwerty").call();
        });

        val refVersion = retriever.retrieve(
            repositoryPath,
            singletonList(Pattern.compile("ver-(?<version>\\d+)"))
        );
        assertThat(refVersion).as("refVersion").isNotNull();
        assertThat(refVersion.getVersion()).as("version")
            .isEqualTo("2");
        assertThat(refVersion.getObjectId()).as("objectId")
            .isEqualTo(verCommit2.get().getId().getName());
    }

    @Test
    void shallowClone() {
        val verCommit1 = new AtomicReference<RevCommit>();
        val verCommit2 = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommit1.set(addSimpleCommit(git));
            git.tag().setObjectId(verCommit1.get()).setName("ver-1").call();

            verCommit2.set(addSimpleCommit(git));
            git.tag().setObjectId(verCommit2.get()).setName("ver-2").call();

            addSimpleCommit(git);
            addSimpleCommit(git);
        });

        cloneRepositoryPartially(1);

        val refVersion = retriever.retrieve(
            repositoryPath,
            singletonList(Pattern.compile("ver-(?<version>\\d+)"))
        );
        assertThat(refVersion).as("refVersion").isNotNull();
        assertThat(refVersion.getVersion()).as("version")
            .isEqualTo("2");
        assertThat(refVersion.getObjectId()).as("objectId")
            .isEqualTo(verCommit2.get().getId().getName());
    }

    @Test
    void mergeCommit() {
        val verCommit1 = new AtomicReference<RevCommit>();
        val verCommit2 = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommit1.set(addSimpleCommit(git));
            git.tag().setObjectId(verCommit1.get()).setName("ver-1").call();

            val defaultBranch = git.getRepository().getBranch();
            git.checkout().setName("feature").setCreateBranch(true).call();

            addSimpleCommit(git);

            verCommit2.set(addSimpleCommit(git));
            git.tag().setObjectId(verCommit2.get()).setName("ver-2").call();

            val lastFeatureCommit = addSimpleCommit(git);

            git.checkout().setName(defaultBranch).call();

            git.merge().include(lastFeatureCommit).setFastForward(FastForwardMode.NO_FF).setCommit(true).call();
        });

        cloneRepository();

        val refVersion = retriever.retrieve(
            repositoryPath,
            singletonList(Pattern.compile("ver-(?<version>\\d+)"))
        );
        assertThat(refVersion).as("refVersion").isNotNull();
        assertThat(refVersion.getVersion()).as("version")
            .isEqualTo("2");
        assertThat(refVersion.getObjectId()).as("objectId")
            .isEqualTo(verCommit2.get().getId().getName());
    }


    @SneakyThrows
    void withServerRepository(SneakyThrowsConsumer<Git> action) {
        serverRepository.incrementOpen();
        try (val git = new Git(serverRepository)) {
            action.accept(git);
        } finally {
            serverRepository.close();
        }
    }


    final AtomicInteger simpleCommitsInServerRepository = new AtomicInteger();

    @SneakyThrows
    RevCommit addSimpleCommit(Git git) {
        val number = simpleCommitsInServerRepository.incrementAndGet();
        write(serverRepositoryPath.resolve("simple-" + number), new byte[0]);
        git.add().addFilepattern("*").call();
        return git.commit().setMessage("Simple " + number).setNoVerify(false).call();
    }


    void cloneRepository() {
        cloneRepositoryPartially(null);
    }

    @SneakyThrows
    @SuppressWarnings("HttpUrlsUsage")
    void cloneRepositoryPartially(@Nullable Integer depth) {
        deleteRecursively(repositoryPath);
        val clone = new CloneCommand()
            .setURI(format("http://%s:%d/repo", serverHost, serverPort))
            .setDirectory(repositoryPath.toFile());
        if (depth != null) {
            clone.setDepth(depth);
        }
        try (val git = clone.call()) {
            configureRepositoryConfig(git.getRepository());
        }
    }

    static void configureRepositoryConfig(Repository repository) {
        val config = repository.getConfig();
        config.setString("user", null, "name", "Test");
        config.setString("user", null, "email", "test@example.com");
        config.setString("http", null, "receivepack", "true");
        config.setString("core", null, "hooksPath", ".git/hooks");
        config.setString("diff", null, "algorithm", "myers");
        config.setString("advice", null, "ignoredHook", "false");
    }

}
