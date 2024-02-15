package name.remal.gradle_plugins.versions_retriever.git;

import name.remal.gradle_plugins.versions_retriever.AbstractRetrieveVersionsWithWorkerExecutor;

public abstract class AbstractRetrieveVersionsFromGit extends AbstractRetrieveVersionsWithWorkerExecutor {

    {
        getOutputs().cacheIf("This task should always fetch from remote Git repository", __ -> false);
    }

}
