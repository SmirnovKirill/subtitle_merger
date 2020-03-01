package kirill.subtitlemerger.gui.utils.background;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

@CommonsLog
public class HelperTask<T> extends Task<Void> {
    @Getter
    private BackgroundRunnerManager backgroundRunnerManager;

    private BackgroundRunner<T> backgroundRunner;

    private BackgroundRunnerCallback<T> callback;

    private Runnable stopProgressRunnable;

    public HelperTask(
            BackgroundRunner<T> backgroundRunner,
            BackgroundRunnerCallback<T> callback,
            Runnable stopProgressRunnable
    ) {
        this.backgroundRunnerManager = new BackgroundRunnerManager(this);
        this.backgroundRunner = backgroundRunner;
        this.callback = callback;
        this.stopProgressRunnable = stopProgressRunnable;

        setOnFailed(event -> {
            log.error(
                    "task has failed, shouldn't happen: "
                            + ExceptionUtils.getStackTrace(event.getSource().getException())
            );
            throw new IllegalStateException();
        });

        setOnCancelled(e -> {
            Platform.runLater(() -> backgroundRunnerManager.setCancellationPossible(false));
            updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
            updateMessage("waiting for the task to cancel");
        });
    }

    @Override
    protected Void call() {
        T result = backgroundRunner.run(backgroundRunnerManager);

        Platform.runLater(() -> {
            backgroundRunnerManager.setCancellationPossible(false);
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
