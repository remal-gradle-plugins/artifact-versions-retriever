package name.remal.gradle_plugins.versions_retriever.git;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.ObjectUtils.max;

import java.time.Duration;
import lombok.NoArgsConstructor;
import org.gradle.api.logging.LogLevel;

@NoArgsConstructor(access = PRIVATE)
abstract class GitUtils {

    public static final Duration FETCH_TIMEOUT = Duration.ofHours(1);

    public static final LogLevel GIT_DEFAULT_LOG_LEVEL = LogLevel.INFO;
    public static final LogLevel GIT_WARN_LOG_LEVEL = max(LogLevel.WARN, GIT_DEFAULT_LOG_LEVEL);
    public static final LogLevel GIT_ERROR_LOG_LEVEL = max(LogLevel.ERROR, GIT_DEFAULT_LOG_LEVEL);

}
