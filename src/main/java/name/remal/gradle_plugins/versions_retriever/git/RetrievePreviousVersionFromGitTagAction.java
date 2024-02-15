package name.remal.gradle_plugins.versions_retriever.git;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PUBLIC;
import static name.remal.gradle_plugins.toolkit.PathUtils.normalizePath;
import static name.remal.gradle_plugins.toolkit.PropertiesUtils.storeProperties;
import static name.remal.gradle_plugins.toolkit.git.GitUtils.findGitRepositoryRootFor;
import static name.remal.gradle_plugins.versions_retriever.AbstractRetrieveVersions.RETRIEVED_COMMIT_HASH_PROPERTY;
import static name.remal.gradle_plugins.versions_retriever.AbstractRetrieveVersions.RETRIEVED_VERSION_PROPERTY;
import static name.remal.gradle_plugins.versions_retriever.git.GitUtils.GIT_ERROR_LOG_LEVEL;

import java.util.Properties;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.val;
import org.gradle.api.GradleException;
import org.gradle.workers.WorkAction;

@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
@CustomLog
@SuppressWarnings("java:S3776")
abstract class RetrievePreviousVersionFromGitTagAction
    implements WorkAction<RetrievePreviousVersionFromGitTagActionParams> {

    @Override
    public void execute() {
        val params = getParameters();
        val projectDir = params.getProjectDirectory().getAsFile().get();
        val repositoryPath = findGitRepositoryRootFor(projectDir.toPath());
        if (repositoryPath == null) {
            logger.log(
                GIT_ERROR_LOG_LEVEL,
                "Git repository root dir couldn't be found for {}",
                projectDir
            );
            return;
        }

        val tagPatterns = params.getTagPatterns().get().stream()
            .map(Pattern::compile)
            .collect(toList());
        if (tagPatterns.isEmpty()) {
            throw new GradleException("Tag patterns can't be empty");
        }

        val resultPropertiesPath = normalizePath(params.getResultPropertiesFile().get().getAsFile().toPath());

        val retriever = new RetrievePreviousVersionFromGitTagActionRetriever(null);
        val refVersion = retriever.retrieve(repositoryPath, tagPatterns);

        val resultProperties = new Properties();
        if (refVersion != null) {
            resultProperties.setProperty(RETRIEVED_VERSION_PROPERTY, refVersion.getVersion());
            resultProperties.setProperty(RETRIEVED_COMMIT_HASH_PROPERTY, refVersion.getObjectId());
        }
        storeProperties(resultProperties, resultPropertiesPath);
    }


}
