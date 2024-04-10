package name.remal.gradle_plugins.versions_retriever.git;

import lombok.NonNull;
import lombok.Value;
import name.remal.gradle_plugins.toolkit.Version;
import org.eclipse.jgit.lib.ObjectId;

@Value
class GitRefVersion implements Comparable<GitRefVersion> {

    @NonNull
    Version version;

    @NonNull
    ObjectId objectId;


    @Override
    public int compareTo(GitRefVersion other) {
        return version.compareTo(other.version);
    }

}
