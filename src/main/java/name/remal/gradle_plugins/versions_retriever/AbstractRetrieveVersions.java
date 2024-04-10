package name.remal.gradle_plugins.versions_retriever;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static name.remal.gradle_plugins.toolkit.FileUtils.normalizeFile;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.PathUtils.createParentDirectories;
import static name.remal.gradle_plugins.toolkit.PathUtils.deleteRecursively;
import static name.remal.gradle_plugins.toolkit.PropertyUtils.getFinalized;

import com.google.errorprone.annotations.ForOverride;
import java.io.File;
import javax.inject.Inject;
import lombok.val;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class AbstractRetrieveVersions extends DefaultTask {

    public static final String RETRIEVED_VERSION_PROPERTY = doNotInline("version");
    public static final String RETRIEVED_VERSIONS_PROPERTY = doNotInline("versions");
    public static final String RETRIEVED_COMMIT_HASH_PROPERTY = doNotInline("commit.hash");


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
    protected abstract void retrieveImpl(File resultPropertiesFile);

    @TaskAction
    public final void retrieve() {
        val resultPropertiesFile = normalizeFile(getFinalized(getResultPropertiesFile()).getAsFile());
        deleteRecursively(resultPropertiesFile.toPath());
        createParentDirectories(resultPropertiesFile.toPath());
        retrieveImpl(resultPropertiesFile);
        setDidWork(true);
    }

}
