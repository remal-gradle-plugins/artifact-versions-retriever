package name.remal.gradle_plugins.versions_retriever.git;

import static java.lang.Math.floor;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import javax.annotation.Nullable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.gradle.api.logging.LogLevel;
import org.gradle.initialization.BuildCancellationToken;

@CustomLog
@RequiredArgsConstructor
class GradleProgressMonitor implements ProgressMonitor {

    @Nullable
    private final BuildCancellationToken buildCancellationToken;

    private final LogLevel logLevel;

    public GradleProgressMonitor(@Nullable BuildCancellationToken buildCancellationToken) {
        this(buildCancellationToken, LogLevel.INFO);
    }


    @Nullable
    private volatile ProgressMonitorTask task;

    @Override
    public void start(int totalTasks) {
        // do nothing
    }

    @Override
    public void beginTask(String title, int totalWork) {
        task = new ProgressMonitorTask(title, totalWork);
    }

    @Override
    public void update(int completedWork) {
        val task = this.task;
        if (task != null) {
            task.update(completedWork);
        }
    }

    @Override
    public void endTask() {
        val task = this.task;
        if (task != null) {
            task.end();
        }
        this.task = null;
    }

    @Override
    public boolean isCancelled() {
        if (buildCancellationToken != null) {
            return buildCancellationToken.isCancellationRequested();
        }
        return false;
    }

    @Override
    public void showDuration(boolean enabled) {
        // do nothing
    }

    @RequiredArgsConstructor
    private class ProgressMonitorTask {

        private final String title;

        private final int totalWork;

        private int lastPercent = -1;

        private long lastLogNanos = nanoTime();

        public void update(int completedWork) {
            final int percent;
            if (completedWork == totalWork) {
                percent = 100;
            } else if (completedWork == 0) {
                percent = 0;
            } else {
                percent = (int) floor(completedWork * 100.0 / totalWork);
            }

            if (percent == lastPercent) {
                return;
            } else {
                lastPercent = percent;
            }

            long nowNanos = nanoTime();
            long secondsSinceLastLog = NANOSECONDS.toSeconds(nowNanos - lastLogNanos);
            boolean shouldLog = percent >= 100 || secondsSinceLastLog >= 1;
            if (shouldLog) {
                logger.log(logLevel, "{}: {}%", title, percent);
                lastLogNanos = nowNanos;
            }
        }

        public void end() {
            update(totalWork);
        }

    }

}
