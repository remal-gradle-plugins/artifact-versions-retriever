package name.remal.gradle_plugins.versions_retriever;

import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.PropertiesUtils.loadProperties;
import static name.remal.gradle_plugins.toolkit.PropertiesUtils.storeProperties;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.val;
import name.remal.gradle_plugins.toolkit.Version;

@Value
@Builder
public class VersionInfo implements Comparable<VersionInfo> {

    public static final String VERSION_PROPERTY = doNotInline("version");
    public static final String GIT_COMMIT_HASH_PROPERTY = doNotInline("git.commit.hash");
    public static final String GIT_TAG_PROPERTY = doNotInline("git.tag");


    @NonNull
    String version;

    @Nullable
    String gitCommitHash;

    @Nullable
    String gitTag;


    @Override
    public int compareTo(VersionInfo other) {
        return getParsedVersion().compareTo(other.getParsedVersion());
    }


    public static VersionInfo loadVersionInfo(Path path) {
        val properties = loadProperties(path);
        return VersionInfo.builder()
            .version(properties.getProperty(VERSION_PROPERTY))
            .gitCommitHash(properties.getProperty(GIT_COMMIT_HASH_PROPERTY))
            .gitTag(properties.getProperty(GIT_TAG_PROPERTY))
            .build();
    }

    public static VersionInfo loadVersionInfo(File file) {
        return loadVersionInfo(file.toPath());
    }


    public void store(Path path) {
        val properties = new Properties();
        properties.setProperty(VERSION_PROPERTY, version);
        if (gitCommitHash != null) {
            properties.setProperty(GIT_COMMIT_HASH_PROPERTY, gitCommitHash);
        }
        if (gitTag != null) {
            properties.setProperty(GIT_COMMIT_HASH_PROPERTY, gitTag);
        }
        storeProperties(properties, path);
    }

    public void store(File file) {
        store(file.toPath());
    }


    //#region: Internals

    @ToString
    @FieldDefaults(level = PRIVATE)
    private static class Internals {

        private volatile Version parsedVersion;

    }

    @Getter(NONE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Internals internals = new Internals();

    private Version getParsedVersion() {
        if (internals.parsedVersion == null) {
            synchronized (this) {
                if (internals.parsedVersion == null) {
                    internals.parsedVersion = Version.parse(version);
                }
            }
        }

        return internals.parsedVersion;
    }

    //#endregion

}
