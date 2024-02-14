package name.remal.gradle_plugins.versions_retriever.git;

import name.remal.gradle_plugins.versions_retriever.AbstractRetrievePreviousVersionWithWorkerExecutor;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.UntrackedTask;

@UntrackedTask(because = "This task should always fetch from remote Git repository")
public abstract class RetrievePreviousVersionFromGitTag extends AbstractRetrievePreviousVersionWithWorkerExecutor {

    {
        getOutputs().cacheIf("This task should always fetch from remote Git repository", __ -> false);
    }

    @Input
    public abstract ListProperty<String> getTagPatterns();

    protected void retrieveImpl() {
        createWorkQueue().submit(RetrievePreviousVersionFromGitTagAction.class, params -> {
            params.getProjectDirectory().set(getProjectLayout().getProjectDirectory());
            params.getTagPatterns().set(getTagPatterns());
            params.getResultPropertiesFile().set(getResultPropertiesFile());
        });
    }

}
