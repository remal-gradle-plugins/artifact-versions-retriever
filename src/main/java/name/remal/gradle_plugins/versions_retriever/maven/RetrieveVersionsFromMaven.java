package name.remal.gradle_plugins.versions_retriever.maven;

import java.io.File;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.UntrackedTask;

@UntrackedTask(because = "This task should always fetch from remote Maven repository")
public abstract class RetrieveVersionsFromMaven extends AbstractRetrieveFromMaven {

    @Input
    public abstract Property<String> getDependency();

    @Override
    protected void retrieveImpl(File resultPropertiesFile) {

    }

}
