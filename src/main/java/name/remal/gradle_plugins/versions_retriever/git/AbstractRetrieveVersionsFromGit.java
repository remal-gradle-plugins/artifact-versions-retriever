package name.remal.gradle_plugins.versions_retriever.git;

import name.remal.gradle_plugins.versions_retriever.AbstractRetrieveVersionsWithWorkerExecutor;
import org.gradle.api.JavaVersion;

public abstract class AbstractRetrieveVersionsFromGit extends AbstractRetrieveVersionsWithWorkerExecutor {

    {
        getOutputs().cacheIf("This task should always fetch from remote Git repository", __ -> false);
    }

    @Override
    protected JavaVersion getMinSupportedJavaVersion() {
        return JavaVersion.VERSION_11;
    }

}
