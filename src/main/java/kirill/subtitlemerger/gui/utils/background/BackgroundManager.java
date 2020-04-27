package kirill.subtitlemerger.gui.utils.background;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.ProgressIndicator;

/**
 * This class provides interaction between the background task and the main thread - updates progress and a progress
 * message, cancellation availability and a cancellation text. It may seem unnecessary since JavaFX's tasks can interact
 * with the main thread the same way and have updateMessage and updateProgress methods. But the problem is that you are
 * able to use these methods only if you create child classes directly, they are unavailable when you use method
 * references.
 */
public class BackgroundManager {
    private ReadOnlyBooleanWrapper cancellationPossible;

    private ReadOnlyStringWrapper cancellationDescription;

    private HelperTask<?> task;

    BackgroundManager(HelperTask task) {
        cancellationPossible = new ReadOnlyBooleanWrapper(false);
        cancellationDescription = new ReadOnlyStringWrapper();
        this.task = task;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean isCancellationPossible() {
        return cancellationPossible.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyBooleanProperty cancellationPossibleProperty() {
        return cancellationPossible.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setCancellationPossible(boolean cancellationPossible) {
        /* The property is usually bound to some gui element so it should be updated in the javafx thread. */
        Platform.runLater(() -> this.cancellationPossible.set(cancellationPossible));
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getCancellationDescription() {
        return cancellationDescription.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyStringProperty cancellationDescriptionProperty() {
        return cancellationDescription.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setCancellationDescription(String cancellationDescription) {
        /* The property is usually bound to some gui element so it should be updated in the javafx thread. */
        Platform.runLater(() -> this.cancellationDescription.set(cancellationDescription));
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void updateMessage(String message) {
        task.updateMessage(message);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyStringProperty messageProperty() {
        return task.messageProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void updateProgress(long workDone, long max) {
        task.updateProgress(workDone, max);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
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

