package name.remal.gradle_plugins.versions_retriever.git;

import static java.lang.String.format;
import static java.nio.file.Files.write;
import static name.remal.gradle_plugins.toolkit.PathUtils.deleteRecursively;

import java.nio.file.Path;
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
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitOperationsIntegrationTest {

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

        serverHost = connector.getHost();
        serverPort = connector.getLocalPort();
    }

    @AfterEach
    void afterEach() throws Throwable {
        server.stop();
        serverRepository.close();
    }

    @Test
    void test() {
        addSimpleCommitsToServerRepository(3);

        withClonedRepository(git -> {
            val repository = git.getRepository();

            try (val revWalk = new RevWalk(repository)) {
                val ref = repository.getRefDatabase().exactRef("HEAD");
                revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
                int count = 0;
                for (val commit : revWalk) {
                    System.out.println("Commit: " + commit);
                    count++;
                }
                System.out.println("Had " + count + " commits");
            }
        });
    }

    int simpleCommitsInServerRepository;

    void addSimpleCommitsToServerRepository(int count) {
        withServerRepository(git -> {
            for (int n = 1; n <= count; ++n) {
                val number = ++simpleCommitsInServerRepository;
                write(serverRepositoryPath.resolve("simple-" + number), new byte[0]);
                git.add().addFilepattern("*").call();
                git.commit().setMessage("Simple " + number).setNoVerify(false).call();
            }
        });
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

    void withClonedRepository(SneakyThrowsConsumer<Git> action) {
        withClonedRepository(null, action);
    }

    @SneakyThrows
    void withClonedRepository(@Nullable Integer depth, SneakyThrowsConsumer<Git> action) {
        deleteRecursively(repositoryPath);
        val clone = new CloneCommand()
            .setURI(format("http://%s:%d/repo", serverHost, serverPort))
            .setDirectory(repositoryPath.toFile());
        if (depth != null) {
            clone.setDepth(depth);
        }
        try (val git = clone.call()) {
            configureRepositoryConfig(git.getRepository());
            action.accept(git);
        }
    }

    static void configureRepositoryConfig(Repository repository) {
        val config = repository.getConfig();
        config.setString("user", null, "name", "Test");
        config.setString("user", null, "email", "test@example.com");
        config.setString("http", null, "receivepack", "true");
        config.setString("core", null, "hooksPath", ".git/hooks");
        config.setString("advice", null, "ignoredHook", "false");
    }

}
