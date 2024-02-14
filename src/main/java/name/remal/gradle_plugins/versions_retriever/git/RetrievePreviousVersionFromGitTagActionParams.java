package name.remal.gradle_plugins.versions_retriever.git;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.workers.WorkParameters;

interface RetrievePreviousVersionFromGitTagActionParams extends WorkParameters {

    DirectoryProperty getProjectDirectory();

    ListProperty<String> getTagPatterns();

    RegularFileProperty getResultPropertiesFile();

}
