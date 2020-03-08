package kirill.subtitlemerger.gui.utils.background;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class BackgroundRunnerManager {
    private BooleanProperty cancellationPossible;

    private HelperTask task;

    BackgroundRunnerManager(HelperTask task) {
        this.cancellationPossible = new SimpleBooleanProperty(false);
        this.task = task;
    }

    public boolean isCancellationPossible() {
        return cancellationPossible.get();
    }

    public BooleanProperty cancellationPossibleProperty() {
        return cancellationPossible;
    }

    public void setCancellationPossible(boolean cancellationPossible) {
        /* Cancellation property is usually bound to some gui element so it should be updated in the javafx thread. */
        Platform.runLater(() -> this.cancellationPossible.set(cancellationPossible));
    }

    public void updateMessage(String message) {
        task.updateMessage(message);
    }

    public void updateProgress(long workDone, long max) {
        task.updateProgress(workDone, max);
    }

    public void updateProgress(double workDone, double max) {
        task.updateProgress(workDone, max);
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return task.progressProperty();
    }

    public ReadOnlyStringProperty messageProperty() {
        return task.messageProperty();
    }

    void cancel() {
        task.cancel();
    }

    public boolean isCancelled() {
        return task.isCancelled();
    }
}

