package name.remal.gradle_plugins.versions_retriever.git;

import static java.nio.file.Files.write;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PUBLIC;
import static name.remal.gradle_plugins.toolkit.PathUtils.createParentDirectories;
import static name.remal.gradle_plugins.toolkit.PathUtils.normalizePath;
import static name.remal.gradle_plugins.toolkit.git.GitUtils.findGitRepositoryRootFor;
import static name.remal.gradle_plugins.versions_retriever.git.GitUtils.GIT_ERROR_LOG_LEVEL;

import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.gradle.api.GradleException;
import org.gradle.workers.WorkAction;

@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
@CustomLog
@SuppressWarnings("java:S3776")
abstract class RetrievePreviousVersionFromGitTagAction
    implements WorkAction<RetrievePreviousVersionFromGitTagActionParams> {

    @Override
    @SneakyThrows
    public void execute() {
        val params = getParameters();
        val resultPropertiesPath = normalizePath(params.getResultPropertiesFile().get().getAsFile().toPath());
        createParentDirectories(resultPropertiesPath);

        val projectDir = params.getProjectDirectory().getAsFile().get();
        val repositoryPath = findGitRepositoryRootFor(projectDir.toPath());
        if (repositoryPath == null) {
            logger.log(
                GIT_ERROR_LOG_LEVEL,
                "Git repository root dir couldn't be found for {}",
                projectDir
            );
            write(resultPropertiesPath, new byte[0]);
            return;
        }

        val tagPatterns = params.getTagPatterns().get().stream()
            .map(Pattern::compile)
            .collect(toList());
        if (tagPatterns.isEmpty()) {
            throw new GradleException("Tag patterns can't be empty");
        }

        val retriever = RetrievePreviousVersionFromGitTagRetriever.builder()
            .tagPatterns(tagPatterns)
            .build();
        val refVersion = retriever.retrieve(repositoryPath);

        if (refVersion == null) {
            write(resultPropertiesPath, new byte[0]);
            return;
        }

        refVersion.store(resultPropertiesPath);
    }


}
