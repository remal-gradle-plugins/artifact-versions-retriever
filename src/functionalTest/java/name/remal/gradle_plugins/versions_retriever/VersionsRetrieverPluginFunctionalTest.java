package name.remal.gradle_plugins.versions_retriever;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import name.remal.gradle_plugins.versions_retriever.git.RetrievePreviousVersionFromGitTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class VersionsRetrieverPluginFunctionalTest {

    final GradleProject project;

    @BeforeEach
    void applyPlugin() {
        project.forBuildFile(build -> {
            build.applyPlugin("name.remal.versions-retriever");
            build.addMavenCentralRepository();
        });
    }

    @Test
    void retrievePreviousVersionFromGitTag() throws Throwable {
        project.forBuildFile(build -> {
            build.addImport(RetrievePreviousVersionFromGitTag.class.getName());
            build.appendBlock("tasks.register('retrieveFromTag', RetrievePreviousVersionFromGitTag)", block -> {
                block.append("    tagPatterns.set([/ver-(?<version>\\d+)/])");
            });
            build.registerDefaultTask("retrieveFromTag");
        });

        project.assertBuildSuccessfully();

        String content = project.readTextFile("build/retrieve-from-tag.properties");
        assertThat(content).isNotNull();
    }

}
