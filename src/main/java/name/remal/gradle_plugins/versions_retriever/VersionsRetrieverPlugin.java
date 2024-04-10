package name.remal.gradle_plugins.versions_retriever;

import static java.lang.String.join;
import static name.remal.gradle_plugins.toolkit.AttributeContainerUtils.javaRuntimeLibrary;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.versions_retriever.git.JGitDependencies.getJGitDependencies;
import static org.gradle.api.artifacts.ExcludeRule.GROUP_KEY;
import static org.gradle.api.artifacts.ExcludeRule.MODULE_KEY;

import com.google.common.collect.ImmutableMap;
import lombok.val;
import name.remal.gradle_plugins.versions_retriever.git.AbstractRetrieveVersionsFromGit;
import name.remal.gradle_plugins.versions_retriever.maven.AbstractRetrieveFromMaven;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

public abstract class VersionsRetrieverPlugin implements Plugin<Project> {

    public static final String VERSIONS_RETRIEVER_JGIT_CONFIGURATION_NAME = doNotInline("versionsRetrieverJGit");

    @Override
    public void apply(Project project) {
        configureJGitTasks(project);
        configureMavenTasks(project);
    }

    private static void configureJGitTasks(Project project) {
        val jgitConf = project.getConfigurations().create(VERSIONS_RETRIEVER_JGIT_CONFIGURATION_NAME, conf -> {
            conf.setCanBeResolved(true);
            conf.setCanBeConsumed(false);
            conf.defaultDependencies(deps -> {
                getJGitDependencies().values().stream()
                    .map(dep -> join(":", dep.getGroup(), dep.getName(), dep.getVersion()))
                    .map(project.getDependencies()::create)
                    .forEach(deps::add);
            });
            conf.exclude(ImmutableMap.of(GROUP_KEY, "org.slf4j", MODULE_KEY, "*"));
            conf.attributes(javaRuntimeLibrary(project.getObjects()));

        });
        project.getTasks().withType(AbstractRetrieveVersionsFromGit.class).configureEach(task -> {
            task.getClasspath().from(jgitConf);
        });
    }

    private static void configureMavenTasks(Project project) {
        project.getTasks().withType(AbstractRetrieveFromMaven.class).configureEach(task -> {
            task.repositories(repos -> {
                project.getRepositories().withType(MavenArtifactRepository.class).all(artifactRepo -> {
                    //repos.maven();
                });
            });
        });
    }

}
