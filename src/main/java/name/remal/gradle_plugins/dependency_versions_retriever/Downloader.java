package name.remal.gradle_plugins.dependency_versions_retriever;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isEmpty;
import static name.remal.gradle_plugins.toolkit.PathUtils.normalizePath;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import javax.annotation.Nullable;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
@CustomLog
abstract class Downloader {

    public static void downloadToFile(String url, File targetFile, @Nullable File metadataFile) {
        downloadToFile(url, targetFile.toPath(), metadataFile != null ? metadataFile.toPath() : null);
    }

    @SneakyThrows
    public static void downloadToFile(String url, Path targetPath, @Nullable Path metadataPath) {
        downloadToFile(new URL(url), targetPath, metadataPath);
    }

    public static void downloadToFile(URL url, File targetFile, @Nullable File metadataFile) {
        downloadToFile(url, targetFile.toPath(), metadataFile != null ? metadataFile.toPath() : null);
    }

    public static void downloadToFile(URL url, Path targetPath, @Nullable Path metadataPath) {
        val protocol = url.getProtocol();
        if (isEmpty(protocol)) {
            throw new IllegalArgumentException("URL must be absolute");
        }

        targetPath = normalizePath(targetPath);

        if (metadataPath == null) {
            logger.debug("Downloading {} into {} (no metadata will be saved)", url, targetPath);
        } else {
            metadataPath = normalizePath(metadataPath);
            logger.debug("Downloading {} into {} (metadata will be saved into {})", url, targetPath, metadataPath);
        }


    }

}
