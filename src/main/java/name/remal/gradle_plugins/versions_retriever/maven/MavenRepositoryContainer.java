package name.remal.gradle_plugins.versions_retriever.maven;

import org.gradle.api.Action;

public interface MavenRepositoryContainer {

    String GRADLE_PLUGIN_PORTAL_REPOSITORY_URI = "https://plugins.gradle.org/m2";
    String MAVEN_CENTRAL_REPOSITORY_URI = "https://repo.maven.apache.org/maven2/";
    String OSS_SONATYPE_RELEASES_REPOSITORY_URI = "https://oss.sonatype.org/service/local/repositories/releases/content/";
    String OSS_SONATYPE_SNAPSHOTS_REPOSITORY_URI = "https://s01.oss.sonatype.org/content/repositories/snapshots/";
    String GOOGLE_REPOSITORY_URI = "https://dl.google.com/dl/android/maven2/";
    String JCENTER_REPOSITORY_URI = "https://jcenter.bintray.com/";

    void maven(Action<MavenRepository> action);

    default void gradlePluginPortal() {
        maven(repo -> {
            repo.getUri().set(GRADLE_PLUGIN_PORTAL_REPOSITORY_URI);
            repo.getWithSnapshots().set(false);
        });
    }

    default void mavenCentral() {
        maven(repo -> {
            repo.getUri().set(MAVEN_CENTRAL_REPOSITORY_URI);
            repo.getWithSnapshots().set(false);
        });
        maven(repo -> {
            repo.getUri().set(OSS_SONATYPE_RELEASES_REPOSITORY_URI);
            repo.getWithSnapshots().set(false);
        });
        maven(repo -> {
            repo.getUri().set(OSS_SONATYPE_SNAPSHOTS_REPOSITORY_URI);
            repo.getWithReleases().set(false);
        });
    }

    default void google() {
        maven(repo -> {
            repo.getUri().set(GOOGLE_REPOSITORY_URI);
            repo.getWithSnapshots().set(false);
        });
    }

    /**
     * @deprecated JFrog announced JCenter's <a href="https://blog.gradle.org/jcenter-shutdown">sunset</a> in February 2021.
     *     Use {@link #mavenCentral()} instead.
     */
    @Deprecated
    default void jcenter() {
        maven(repo -> {
            repo.getUri().set(JCENTER_REPOSITORY_URI);
            repo.getWithSnapshots().set(false);
        });
    }

}
