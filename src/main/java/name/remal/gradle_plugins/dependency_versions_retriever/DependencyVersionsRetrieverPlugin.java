package name.remal.gradle_plugins.dependency_versions_retriever;

import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class DependencyVersionsRetrieverPlugin implements Plugin<Project> {

    public static final String DEPENDENCY_VERSIONS_RETRIEVER_EXTENSION_NAME =
        doNotInline("dependencyVersionsRetriever");

    @Override
    public void apply(Project project) {
        val extension = project.getExtensions().create(
            DEPENDENCY_VERSIONS_RETRIEVER_EXTENSION_NAME,
            DependencyVersionsRetrieverExtension.class
        );
    }

}
