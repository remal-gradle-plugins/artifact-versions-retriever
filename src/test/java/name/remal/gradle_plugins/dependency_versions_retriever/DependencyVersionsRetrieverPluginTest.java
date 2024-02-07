package name.remal.gradle_plugins.dependency_versions_retriever;

import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.RequiredArgsConstructor;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class DependencyVersionsRetrieverPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(DependencyVersionsRetrieverPlugin.class);
    }

    @Test
    void test() {
        assertTrue(project.getPlugins().hasPlugin(DependencyVersionsRetrieverPlugin.class));
    }

}
