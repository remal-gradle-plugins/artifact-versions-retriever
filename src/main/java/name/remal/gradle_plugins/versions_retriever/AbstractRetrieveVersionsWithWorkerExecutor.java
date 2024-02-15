package name.remal.gradle_plugins.versions_retriever;

import static name.remal.gradle_plugins.toolkit.JavaLauncherUtils.getJavaLauncherProviderFor;
import static name.remal.gradle_plugins.versions_retriever.VersionsRetrieverForkOptions.IS_FORK_ENABLED_DEFAULT;

import com.google.errorprone.annotations.ForOverride;
import javax.inject.Inject;
import lombok.val;
import org.gradle.api.JavaVersion;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

public abstract class AbstractRetrieveVersionsWithWorkerExecutor extends AbstractRetrieveVersions {

    @Classpath
    @InputFiles
    public abstract ConfigurableFileCollection getClasspath();

    @Nested
    @org.gradle.api.tasks.Optional
    public abstract Property<VersionsRetrieverForkOptions> getForkOptions();

    @Nested
    @org.gradle.api.tasks.Optional
    public abstract Property<JavaLauncher> getJavaLauncher();

    {
        getJavaLauncher().convention(getJavaLauncherProviderFor(getProject(), spec -> {
            val minSupportedJavaLanguageVersion = JavaLanguageVersion.of(
                getMinSupportedJavaVersion().getMajorVersion()
            );
            val javaMajorVersion = spec.getLanguageVersion()
                .orElse(JavaLanguageVersion.of(JavaVersion.current().getMajorVersion()))
                .map(JavaLanguageVersion::asInt)
                .get();
            if (javaMajorVersion < minSupportedJavaLanguageVersion.asInt()) {
                spec.getLanguageVersion().set(minSupportedJavaLanguageVersion);
            }
        }));
    }

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @Internal
    @ForOverride
    protected JavaVersion getMinSupportedJavaVersion() {
        return JavaVersion.VERSION_1_8;
    }

    protected final WorkQueue createWorkQueue() {
        boolean isForkEnabled = getForkOptions()
            .flatMap(VersionsRetrieverForkOptions::getEnabled)
            .getOrElse(IS_FORK_ENABLED_DEFAULT);
        val minSupportedJavaVersion = getMinSupportedJavaVersion();
        if (!isForkEnabled && JavaVersion.current().compareTo(minSupportedJavaVersion) < 0) {
            getLogger().warn(
                "The current Java version ({}) is less than {}, enabling forking for task {}",
                JavaVersion.current().getMajorVersion(),
                minSupportedJavaVersion.getMajorVersion(),
                getPath()
            );
            isForkEnabled = true;
        }

        if (isForkEnabled) {
            return getWorkerExecutor().processIsolation(spec -> {
                spec.getForkOptions().setExecutable(
                    getJavaLauncher().get()
                        .getExecutablePath()
                        .getAsFile()
                        .getAbsolutePath()
                );
                spec.getForkOptions().setMaxHeapSize(
                    getForkOptions()
                        .flatMap(VersionsRetrieverForkOptions::getMaxHeapSize)
                        .getOrNull()
                );
                spec.getClasspath().from(getClasspath());
            });

        } else {
            return getWorkerExecutor().classLoaderIsolation(spec -> {
                spec.getClasspath().from(getClasspath());
            });
        }
    }

}
