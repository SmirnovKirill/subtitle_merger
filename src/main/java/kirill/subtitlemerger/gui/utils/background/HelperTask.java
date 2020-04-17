package kirill.subtitlemerger.gui.utils.background;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

/*
 * This class was created basically just to give access to the updateProgress and updateMessage methods. Otherwise
 * we could have just used an anonymous class in the AbstractController.
 */
@CommonsLog
public class HelperTask<T> extends Task<Void> {
    @Getter
    private BackgroundManager backgroundManager;

    private BackgroundRunner<T> backgroundRunner;

    private BackgroundCallback<T> callback;

    private Runnable stopProgressRunnable;

    public HelperTask(
            BackgroundRunner<T> backgroundRunner,
            BackgroundCallback<T> callback,
            Runnable stopProgressRunnable
    ) {
        this.backgroundManager = new BackgroundManager(this);
        this.backgroundRunner = backgroundRunner;
        this.callback = callback;
        this.stopProgressRunnable = stopProgressRunnable;

        setOnFailed(event -> {
            Throwable e = event.getSource().getException();
            log.error("task has failed, shouldn't happen: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        });

        setOnCancelled(e -> {
            Platform.runLater(() -> backgroundManager.setCancellationPossible(false));
            updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
            updateMessage("Waiting for the task to cancel...");
        });
    }

    @Override
    protected Void call() {
        T result = backgroundRunner.run(backgroundManager);

        Platform.runLater(() -> {
            backgroundManager.setCancellationPossible(false);
            stopProgressRunnable.run();
            callback.run(result);
        });

        return null;
    }

    /* We need to override this method to give access to the manager. */
    public void updateMessage(String message) {
        super.updateMessage(message);
    }

    /* We need to override this method to give access to the manager. */
    public void updateProgress(long workDone, long max) {
        super.updateProgress(workDone, max);
    }

    /* We need to override this method to give access to the manager. */
    protected void updateProgress(double workDone, double max) {
        super.updateProgress(workDone, max);
    }
}
