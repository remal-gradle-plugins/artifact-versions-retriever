package name.remal.gradle_plugins.versions_retriever.git;

import lombok.Value;
import name.remal.gradle_plugins.toolkit.Version;
import org.eclipse.jgit.lib.ObjectId;

@Value
class GitRefVersion {
    Version version;
    ObjectId objectId;
}
