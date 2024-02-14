package name.remal.gradle_plugins.versions_retriever;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static name.remal.gradle_plugins.toolkit.PathUtils.createParentDirectories;
import static name.remal.gradle_plugins.toolkit.PathUtils.deleteRecursively;
import static name.remal.gradle_plugins.toolkit.PathUtils.normalizePath;

import com.google.errorprone.annotations.ForOverride;
import javax.inject.Inject;
import lombok.val;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class AbstractRetrievePreviousVersion extends DefaultTask {

    @OutputFile
    public abstract RegularFileProperty getResultPropertiesFile();

    {
        val defaultResultPropertiesFileName = LOWER_CAMEL.to(LOWER_HYPHEN, getName()) + ".properties";
        getResultPropertiesFile().convention(
            getProjectLayout().getBuildDirectory().file(defaultResultPropertiesFileName)
        );
    }

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    @ForOverride
    protected abstract void retrieveImpl();

    @TaskAction
    public final void retrieve() {
        val resultPropertiesPath = normalizePath(getResultPropertiesFile().get().getAsFile().toPath());
        deleteRecursively(resultPropertiesPath);
        createParentDirectories(resultPropertiesPath);
        retrieveImpl();
        setDidWork(true);
    }

}
