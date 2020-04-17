package kirill.subtitlemerger.gui.utils.background;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.ProgressIndicator;

/**
 * This class provides interaction between the background task and the main thread - updates progress and progress
 * message, cancellation availability and cancellation text. It may seem unnecessary since JavaFX's tasks can interact
 * with the main thread the same way and have updateMessage and updateProgress message. But the problem is that you are
 * able to use these methods only if you create child classes directly, they are unavailable when you use method
 * references.
 */
public class BackgroundManager {
    private ReadOnlyBooleanWrapper cancellationPossible;

    private ReadOnlyStringWrapper cancellationDescription;

    private HelperTask<?> task;

    BackgroundManager(HelperTask task) {
        this.cancellationPossible = new ReadOnlyBooleanWrapper(false);
        this.cancellationDescription = new ReadOnlyStringWrapper();
        this.task = task;
    }

    public boolean isCancellationPossible() {
        return cancellationPossible.get();
    }

    public ReadOnlyBooleanProperty cancellationPossibleProperty() {
        return cancellationPossible.getReadOnlyProperty();
    }

    public void setCancellationPossible(boolean cancellationPossible) {
        /* Property is usually bound to some gui element so it should be updated in the javafx thread. */
        Platform.runLater(() -> this.cancellationPossible.set(cancellationPossible));
    }

    public String getCancellationDescription() {
        return cancellationDescription.get();
    }

    public ReadOnlyStringProperty cancellationDescriptionProperty() {
        return cancellationDescription.getReadOnlyProperty();
    }

    public void setCancellationDescription(String cancellationDescription) {
        /* Property is usually bound to some gui element so it should be updated in the javafx thread. */
        Platform.runLater(() -> this.cancellationDescription.set(cancellationDescription));
    }

    public void updateMessage(String message) {
        task.updateMessage(message);
    }

    public ReadOnlyStringProperty messageProperty() {
        return task.messageProperty();
    }

    public void updateProgress(long workDone, long max) {
        task.updateProgress(workDone, max);
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return task.progressProperty();
    }

    public void setIndeterminateProgress() {
        task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
    }

    public void cancel() {
        task.cancel();
    }
}

