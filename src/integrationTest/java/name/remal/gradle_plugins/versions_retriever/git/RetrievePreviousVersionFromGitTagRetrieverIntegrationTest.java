package name.remal.gradle_plugins.versions_retriever.git;

import static java.lang.String.format;
import static java.nio.file.Files.write;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.defaultValue;
import static name.remal.gradle_plugins.toolkit.PathUtils.deleteRecursively;
import static name.remal.gradle_plugins.versions_retriever.Assertions.assertThat;
import static org.eclipse.jgit.api.MergeCommand.FastForwardMode.NO_FF;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Repository.shortenRefName;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.toolkit.SneakyThrowUtils.SneakyThrowsConsumer;
import name.remal.gradle_plugins.toolkit.testkit.MinSupportedJavaVersion;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@MinSupportedJavaVersion(11)
@SuppressWarnings("WriteOnlyObject")
class RetrievePreviousVersionFromGitTagRetrieverIntegrationTest {

    final RetrievePreviousVersionFromGitTagRetriever retriever =
        RetrievePreviousVersionFromGitTagRetriever.builder()
            .tagPattern(Pattern.compile("ver-(?<version>\\d+)"))
            .build();

    @TempDir
    Path repositoryPath;

    @TempDir
    Path serverRepositoryPath;
    Repository serverRepository;
    String serverRepositoryDefaultBranch;
    Server server;
    String serverHost;
    int serverPort;

    @BeforeEach
    void beforeEach() throws Throwable {
        serverRepository = FileRepositoryBuilder.create(serverRepositoryPath.resolve(".git").toFile());
        serverRepository.create();
        configureRepositoryConfig(serverRepository);
        withServerRepository(git -> {
            serverRepositoryDefaultBranch = git.getRepository().getBranch();
            addSimpleCommit(git);
        });

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
            verCommit1.set(addSimpleCommit(git, "ver-1"));
            verCommit2.set(addSimpleCommit(git, "ver-2"));
            verCommitQwerty.set(addSimpleCommit(git, "ver-qwerty"));
            addSimpleCommit(git);
        });

        cloneRepository();

        val refVersion = retriever.retrieve(repositoryPath);

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("2")
            .hasGitCommitHash(verCommit2.get().getId().getName())
            .hasGitTag("ver-2");
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
            addSimpleCommit(git);
        });

        cloneRepository();

        // add tags after clone:
        withServerRepository(git -> {
            addTag(git, verCommit1.get(), "ver-1");
            addTag(git, verCommit2.get(), "ver-2");
            addTag(git, verCommitQwerty.get(), "ver-qwerty");
        });

        val refVersion = retriever.retrieve(repositoryPath);

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("2")
            .hasGitCommitHash(verCommit2.get().getId().getName())
            .hasGitTag("ver-2");
    }

    @Test
    void shallowClone() {
        val verCommit1 = new AtomicReference<RevCommit>();
        val verCommit2 = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommit1.set(addSimpleCommit(git, "ver-1"));
            verCommit2.set(addSimpleCommit(git, "ver-2"));
            addSimpleCommit(git);
            addSimpleCommit(git);
        });

        cloneRepositoryPartially(1);

        val refVersion = retriever.retrieve(repositoryPath);

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("2")
            .hasGitCommitHash(verCommit2.get().getId().getName())
            .hasGitTag("ver-2");
    }

    @Test
    void currentCommitIsSkipped() {
        val verCommit1 = new AtomicReference<RevCommit>();
        val verCommit2 = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommit1.set(addSimpleCommit(git, "ver-1"));
            verCommit2.set(addSimpleCommit(git, "ver-2"));
        });

        cloneRepository();

        val refVersion = retriever.retrieve(repositoryPath);

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("1")
            .hasGitCommitHash(verCommit1.get().getId().getName())
            .hasGitTag("ver-1");
    }

    @Test
    void mergeCommitPre1Feature2() {
        val verCommitPre = new AtomicReference<RevCommit>();
        val verCommitFeature = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommitPre.set(addSimpleCommit(git, "ver-1"));

            checkout(git, "feature");
            addSimpleCommit(git);
            verCommitFeature.set(addSimpleCommit(git, "ver-2"));
            val lastFeatureCommit = addSimpleCommit(git);

            checkout(git, serverRepositoryDefaultBranch);
            git.merge().include(lastFeatureCommit).setFastForward(NO_FF).setCommit(true).call();
            addSimpleCommit(git);
        });

        cloneRepository();

        val refVersion = retriever.retrieve(repositoryPath);

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("2")
            .hasGitCommitHash(verCommitFeature.get().getId().getName())
            .hasGitTag("ver-2");
    }

    @Test
    void mergeCommitPre2() {
        val verCommitPre = new AtomicReference<RevCommit>();
        val verCommitFeature = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommitPre.set(addSimpleCommit(git, "ver-2"));

            checkout(git, "feature");
            addSimpleCommit(git);
            verCommitFeature.set(addSimpleCommit(git));
            val lastFeatureCommit = addSimpleCommit(git);

            checkout(git, serverRepositoryDefaultBranch);
            git.merge().include(lastFeatureCommit).setFastForward(NO_FF).setCommit(true).call();
            addSimpleCommit(git);
        });

        cloneRepository();

        val refVersion = retriever.retrieve(repositoryPath);

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("2")
            .hasGitCommitHash(verCommitPre.get().getId().getName())
            .hasGitTag("ver-2");
    }

    @Test
    void mergeCommitFeature2() {
        val verCommitPre = new AtomicReference<RevCommit>();
        val verCommitFeature = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommitPre.set(addSimpleCommit(git));

            checkout(git, "feature");
            addSimpleCommit(git);
            verCommitFeature.set(addSimpleCommit(git, "ver-2"));
            val lastFeatureCommit = addSimpleCommit(git);

            checkout(git, serverRepositoryDefaultBranch);
            git.merge().include(lastFeatureCommit).setFastForward(NO_FF).setCommit(true).call();
            addSimpleCommit(git);
        });

        cloneRepository();

        val refVersion = retriever.retrieve(repositoryPath);

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("2")
            .hasGitCommitHash(verCommitFeature.get().getId().getName())
            .hasGitTag("ver-2");
    }

    @Test
    void mergeCommitPre3Pre2Feature1() {
        val verCommitPre1 = new AtomicReference<RevCommit>();
        val verCommitPre2 = new AtomicReference<RevCommit>();
        val verCommitFeature = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommitPre1.set(addSimpleCommit(git, "ver-3"));
            verCommitPre2.set(addSimpleCommit(git, "ver-2"));

            checkout(git, "feature");
            addSimpleCommit(git);
            verCommitFeature.set(addSimpleCommit(git, "ver-1"));
            val lastFeatureCommit = addSimpleCommit(git);

            checkout(git, serverRepositoryDefaultBranch);
            git.merge().include(lastFeatureCommit).setFastForward(NO_FF).setCommit(true).call();
            addSimpleCommit(git);
        });

        cloneRepository();

        val refVersion = retriever.retrieve(repositoryPath);

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("2")
            .hasGitCommitHash(verCommitPre2.get().getId().getName())
            .hasGitTag("ver-2");
    }

    @Test
    void mergeCommitPre2Feature1() {
        val verCommitPre = new AtomicReference<RevCommit>();
        val verCommitFeature = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommitPre.set(addSimpleCommit(git, "ver-2"));

            checkout(git, "feature");
            addSimpleCommit(git);
            verCommitFeature.set(addSimpleCommit(git, "ver-1"));
            val lastFeatureCommit = addSimpleCommit(git);

            checkout(git, serverRepositoryDefaultBranch);
            git.merge().include(lastFeatureCommit).setFastForward(NO_FF).setCommit(true).call();
            addSimpleCommit(git);
        });

        cloneRepository();

        val refVersion = retriever.retrieve(
            repositoryPath
        );

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("2")
            .hasGitCommitHash(verCommitPre.get().getId().getName())
            .hasGitTag("ver-2");
    }

    @Test
    void mergeCommitPre3Feature1Post2() {
        val verCommitPre = new AtomicReference<RevCommit>();
        val verCommitFeature = new AtomicReference<RevCommit>();
        val verCommitPost = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommitPre.set(addSimpleCommit(git, "ver-3"));

            checkout(git, "feature");
            addSimpleCommit(git);
            verCommitFeature.set(addSimpleCommit(git, "ver-1"));
            val lastFeatureCommit = addSimpleCommit(git);

            checkout(git, serverRepositoryDefaultBranch);
            verCommitPost.set(addSimpleCommit(git, "ver-2"));
            git.merge().include(lastFeatureCommit).setFastForward(NO_FF).setCommit(true).call();
            addSimpleCommit(git);
        });

        cloneRepository();

        val refVersion = retriever.retrieve(repositoryPath);

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("2")
            .hasGitCommitHash(verCommitPost.get().getId().getName())
            .hasGitTag("ver-2");
    }

    @Test
    void mergeCommitPre3Feature2Post1() {
        val verCommitPre = new AtomicReference<RevCommit>();
        val verCommitFeature = new AtomicReference<RevCommit>();
        val verCommitPost = new AtomicReference<RevCommit>();
        withServerRepository(git -> {
            verCommitPre.set(addSimpleCommit(git, "ver-3"));

            checkout(git, "feature");
            addSimpleCommit(git);
            verCommitFeature.set(addSimpleCommit(git, "ver-2"));
            val lastFeatureCommit = addSimpleCommit(git);

            checkout(git, serverRepositoryDefaultBranch);
            verCommitPost.set(addSimpleCommit(git, "ver-1"));
            git.merge().include(lastFeatureCommit).setFastForward(NO_FF).setCommit(true).call();
            addSimpleCommit(git);
        });

        cloneRepository();

        val refVersion = retriever.retrieve(repositoryPath);

        assertNotNull(refVersion);
        assertThat(refVersion)
            .hasVersion("2")
            .hasGitCommitHash(verCommitFeature.get().getId().getName())
            .hasGitTag("ver-2");
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

    RevCommit addSimpleCommit(Git git, String tagName) {
        val commit = addSimpleCommit(git);
        addTag(git, commit, tagName);
        return commit;
    }

    @SneakyThrows
    RevCommit addSimpleCommit(Git git) {
        val number = simpleCommitsInServerRepository.incrementAndGet();
        write(serverRepositoryPath.resolve("simple-" + number), new byte[0]);
        git.add().addFilepattern("*").call();
        return git.commit().setMessage("Simple " + number).setNoVerify(true).call();
    }

    @SneakyThrows
    void addTag(Git git, RevCommit commit, String tagName) {
        git.tag().setObjectId(commit).setName(tagName).call();
    }

    @SneakyThrows
    void checkout(Git git, String branchName) {
        val branchRefs = git.getRepository().getRefDatabase().getRefsByPrefix(R_HEADS);
        for (val branchRef : branchRefs) {
            val branchRefName = shortenRefName(branchRef.getName());
            if (branchRefName.equals(branchName)) {
                git.checkout().setName(branchName).call();
                return;
            }
        }

        git.checkout().setName(branchName).setCreateBranch(true).call();
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
