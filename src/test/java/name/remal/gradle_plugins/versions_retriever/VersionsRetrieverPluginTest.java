package name.remal.gradle_plugins.versions_retriever;

import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.RequiredArgsConstructor;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class VersionsRetrieverPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(VersionsRetrieverPlugin.class);
    }

    @Test
    void test() {
        assertTrue(project.getPlugins().hasPlugin(VersionsRetrieverPlugin.class));
    }

}
