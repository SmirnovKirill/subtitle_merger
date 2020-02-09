package kirill.subtitlemerger.gui.background_tasks;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public abstract class BackgroundTask<T> {
    private final BackgroundTaskRunner<T> taskRunner;

    private BooleanProperty cancellationPossible;

    public BackgroundTask() {
        this.taskRunner = new BackgroundTaskRunner<>(this);
        this.cancellationPossible = new SimpleBooleanProperty(false);
    }

    public boolean isCancellationPossible() {
        return cancellationPossible.get();
    }

    public BooleanProperty cancellationPossibleProperty() {
        return cancellationPossible;
    }

    public void setCancellationPossible(boolean cancellationPossible) {
        /* Cancellation property is usually bound to some gui element so it should be updated in the javafx thread. */
        Platform.runLater(() -> {
            this.cancellationPossible.set(cancellationPossible);
        });
    }

    protected abstract T run();

    final void runAndFinish() {
        T result = run();

        Platform.runLater(() -> {
            setCancellationPossible(false);
            onFinish(result);
        });
    }

    protected abstract void onFinish(T result);

    public void start() {
        Thread thread = new Thread(taskRunner);
        thread.setDaemon(true);
        thread.start();
    }

    public void updateMessage(String message) {
        taskRunner.updateMessage(message);
    }

    public void updateProgress(long workDone, long max) {
        taskRunner.updateProgress(workDone, max);
    }

    public void updateProgress(double workDone, double max) {
        taskRunner.updateProgress(workDone, max);
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return taskRunner.progressProperty();
    }

    public ReadOnlyStringProperty messageProperty() {
        return taskRunner.messageProperty();
    }

    public void cancel() {
        taskRunner.cancel();
    }

    public boolean isCancelled() {
        return taskRunner.isCancelled();
    }
}
