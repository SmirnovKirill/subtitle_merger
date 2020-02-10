package kirill.subtitlemerger.gui.core.background_tasks;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
//todo comment why to create this class (basically just updateMessage/updateProgress)
final class BackgroundTaskRunner<T> extends Task<Void> {
    private BackgroundTask<T> task;

    BackgroundTaskRunner(BackgroundTask<T> task) {
        super();

        this.task = task;

        /*
         * We set empty body here because all post-actions will be performed by the onFinished method.
         */
        setOnSucceeded(event -> {});

        setOnFailed(event -> {
            log.error("task has failed, shouldn't happen");
            throw new IllegalStateException();
        });

        setOnCancelled(e -> {
            Platform.runLater(() -> task.setCancellationPossible(false));
            updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
            updateMessage("waiting for the task to cancel");
        });
    }

    @Override
    protected Void call() {
        task.runAndFinish();

        return null;
    }

    @Override
    public void updateMessage(String message) {
        super.updateMessage(message);
    }

    @Override
    protected void updateProgress(long workDone, long max) {
        super.updateProgress(workDone, max);
    }

    @Override
    protected void updateProgress(double workDone, double max) {
        super.updateProgress(workDone, max);
    }
}
