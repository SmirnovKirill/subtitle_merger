package kirill.subtitlemerger.gui.core.background_tasks;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import lombok.extern.apachecommons.CommonsLog;

/*
 * To see the explanation on why this class has been created please read the comment in the BackgroundTask class
 * (basically just because updateMessage and updateProgress are protected and you can't call them without creating
 * this class).
 */
@CommonsLog
final class BackgroundTaskRunner<T> extends Task<Void> {
    private BackgroundTask<T> task;

    BackgroundTaskRunner(BackgroundTask<T> task) {
        super();

        this.task = task;

        /*
         * We don't call the setOnSuccess method because all post-actions will be performed by the onFinished method.
         */

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
