package name.remal.gradle_plugins.artifact_versions_retriever;

import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.RequiredArgsConstructor;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class ArtifactVersionsRetrieverPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(ArtifactVersionsRetrieverPlugin.class);
    }

    @Test
    void test() {
        assertTrue(project.getPlugins().hasPlugin(ArtifactVersionsRetrieverPlugin.class));
    }

}
