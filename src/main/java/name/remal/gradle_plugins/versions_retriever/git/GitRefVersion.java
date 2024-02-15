package name.remal.gradle_plugins.versions_retriever.git;

import lombok.NonNull;
import lombok.Value;

@Value
class GitRefVersion {
    @NonNull String version;
    @NonNull String objectId;
}
