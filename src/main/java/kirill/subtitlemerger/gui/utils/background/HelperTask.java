package kirill.subtitlemerger.gui.utils.background;

import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * This class was created basically just to give access to the updateProgress and updateMessage methods for the
 * background manager.
 */
@CommonsLog
class HelperTask<T> extends Task<Void> {
    @Getter
    private BackgroundManager manager;

    private BackgroundRunner<T> runner;

    private BackgroundCallback<T> callback;

    HelperTask(BackgroundRunner<T> runner, BackgroundCallback<T> callback) {
        manager = new BackgroundManager(this);
        this.runner = runner;
        this.callback = callback;

        setOnFailed(event -> {
            Throwable e = event.getSource().getException();
            log.error("task has failed, most likely a bug: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        });

        setOnCancelled(e -> {
            Platform.runLater(() -> manager.setCancellationPossible(false));
            manager.setIndeterminateProgress();
            manager.updateMessage("Waiting for the task to cancel...");
        });
    }

    @Override
    protected Void call() {
        T result = runner.run(manager);

        Platform.runLater(() -> {
            manager.setCancellationPossible(false);
            callback.run(result);
        });

        return null;
    }

    /* We need to override this method to give access to the manager. */
    @Override
    protected void updateMessage(String message) {
        super.updateMessage(message);
    }

    /* We need to override this method to give access to the manager. */
    @Override
    protected void updateProgress(long workDone, long max) {
        super.updateProgress(workDone, max);
    }

    /* We need to override this method to give access to the manager. */
    @Override
    protected void updateProgress(double workDone, double max) {
        super.updateProgress(workDone, max);
    }
}
