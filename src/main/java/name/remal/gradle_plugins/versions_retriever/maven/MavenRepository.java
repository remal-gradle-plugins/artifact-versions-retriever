package name.remal.gradle_plugins.versions_retriever.maven;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

public abstract class MavenRepository {

    @Input
    public abstract Property<String> getUri();

    @Input
    public abstract Property<Boolean> getTrustAllCertificates();

    {
        getTrustAllCertificates().convention(false);
    }

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getUsername();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getPassword();

    @Input
    public abstract Property<Boolean> getWithReleases();

    {
        getWithReleases().convention(true);
    }

    @Input
    public abstract Property<Boolean> getWithSnapshots();

    {
        getWithSnapshots().convention(true);
    }

}
