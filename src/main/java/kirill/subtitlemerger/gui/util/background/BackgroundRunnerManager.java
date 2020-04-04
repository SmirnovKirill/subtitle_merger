package kirill.subtitlemerger.gui.util.background;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.ProgressIndicator;

public class BackgroundRunnerManager {
    private BooleanProperty cancellationPossible;

    private StringProperty cancellationDescription;

    private HelperTask task;

    BackgroundRunnerManager(HelperTask task) {
        this.cancellationPossible = new SimpleBooleanProperty(false);
        this.cancellationDescription = new SimpleStringProperty();
        this.task = task;
    }

    public boolean isCancellationPossible() {
        return cancellationPossible.get();
    }

    public BooleanProperty cancellationPossibleProperty() {
        return cancellationPossible;
    }

    public void setCancellationPossible(boolean cancellationPossible) {
        /* Property is usually bound to some gui element so it should be updated in the javafx thread. */
        Platform.runLater(() -> this.cancellationPossible.set(cancellationPossible));
    }

    public String getCancellationDescription() {
        return cancellationDescription.get();
    }

    public StringProperty cancellationDescriptionProperty() {
        return cancellationDescription;
    }

    public void setCancellationDescription(String cancellationDescription) {
        /* Property is usually bound to some gui element so it should be updated in the javafx thread. */
        Platform.runLater(() -> this.cancellationDescription.set(cancellationDescription));
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

    public void setIndeterminateProgress() {
        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
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
}

