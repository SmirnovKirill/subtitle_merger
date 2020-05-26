package kirill.subtitlemerger.gui.utils.background;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.ProgressIndicator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Stack;

/**
 * This class provides interaction between the background task and the main thread - updates progress and a progress
 * message, cancellation availability and a cancellation text. It may seem unnecessary since JavaFX's tasks can interact
 * with the main thread the same way and have updateMessage and updateProgress methods. But the problem is that you are
 * able to use these methods only if you create child classes directly, they are unavailable when you use method
 * references.
 * The class also handles the task's cancellation - once the task is canceled an appropriate progress message is set and
 * all further modifying requests to the main thread are ignored.
 */
@CommonsLog
public class BackgroundManager {
    private boolean canceled;

    private ReadOnlyBooleanWrapper cancelPossible;

    private ReadOnlyStringWrapper cancelDescription;

    private HelperTask<?> task;

    /*
     * We have to keep the current state and update it each time a modifying method is invoked because otherwise we
     * won't be able to get the values when we'll need them since they are available only from the main JavaFX thread.
     */
    private TaskState currentState;

    /*
     * It should be a stack and not just one value because the save/restore calls can be nested.
     */
    private Stack<TaskState> savedStates;

    BackgroundManager(HelperTask task) {
        cancelPossible = new ReadOnlyBooleanWrapper(false);
        cancelDescription = new ReadOnlyStringWrapper();
        this.task = task;
        savedStates = new Stack<>();
        currentState = new TaskState(
                false,
                null,
                null,
                ProgressIndicator.INDETERMINATE_PROGRESS,
                ProgressIndicator.INDETERMINATE_PROGRESS
        );
    }

    /**
     * This method is very helpful for writing background utility methods that are unaware of what's going on outside.
     * They should save the initial state, change the state according to their needs and at the end return everything
     * to the initial state
     */
    public void saveCurrentTaskState() {
        savedStates.push(currentState);
    }

    /**
     * This method is very helpful for writing background utility methods that are unaware of what's going on outside.
     * They should save the initial state, change the state according to their needs and at the end return everything
     * to the initial state
     */
    public void restoreSavedTaskState() {
        if (savedStates.empty()) {
            log.error("there are no saved task states, most likely a bug");
            throw new IllegalStateException();
        }

        TaskState savedState = savedStates.pop();

        setCancelPossible(savedState.isCancelPossible());
        setCancelDescription(savedState.getCancelDescription());
        updateMessage(savedState.getProgressMessage());
        updateProgress(savedState.getWorkDone(), savedState.getTotalWork());
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean getCancelPossible() {
        return cancelPossible.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyBooleanProperty cancelPossibleProperty() {
        return cancelPossible.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setCancelPossible(boolean cancelPossible) {
        if (canceled) {
            log.info("setCancelPossible is ignored since the task has been canceled, value: " + cancelPossible);
            return;
        }

        /* The property is usually bound to some gui element so it should be updated in the javafx thread. */
        Platform.runLater(() -> this.cancelPossible.set(cancelPossible));
        currentState.setCancelPossible(cancelPossible);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getCancelDescription() {
        return cancelDescription.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyStringProperty cancelDescriptionProperty() {
        return cancelDescription.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setCancelDescription(String cancelDescription) {
        if (canceled) {
            log.info("setCancelDescription is ignored since the task has been canceled, value: " + cancelDescription);
            return;
        }

        /* The property is usually bound to some gui element so it should be updated in the javafx thread. */
        Platform.runLater(() -> this.cancelDescription.set(cancelDescription));
        currentState.setCancelDescription(cancelDescription);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void updateProgress(double workDone, double max) {
        if (canceled) {
            log.info("updateProgress is ignored since the task has been canceled, value: " + workDone + "/" + max);
            return;
        }

        task.updateProgress(workDone, max);
        currentState.setWorkDone(workDone);
        currentState.setTotalWork(max);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyDoubleProperty progressProperty() {
        return task.progressProperty();
    }

    public void setIndeterminateProgress() {
        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void updateMessage(String message) {
        if (canceled) {
            log.info("updateMessage is ignored since the task has been canceled, value: " + message);
            return;
        }

        task.updateMessage(message);
        currentState.setProgressMessage(message);
    }

    /**
     * Cancels the task. This method should be called from the main JavaFX thread only.
     */
    public void cancel() {
        if (!Platform.isFxApplicationThread()) {
            log.error("task should be cancelled only from the main thread, most likely a bug");
            throw new IllegalStateException();
        }
        if (canceled) {
            log.error("the task has already been canceled, most likely a bug");
            throw new IllegalStateException();
        }

        task.cancel();

        setCancelPossible(false);
        setIndeterminateProgress();
        updateMessage("Waiting for the task to cancel...");

        canceled = true;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyStringProperty messageProperty() {
        return task.messageProperty();
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class TaskState {
        private boolean cancelPossible;

        private String cancelDescription;

        private String progressMessage;

        private double workDone;

        private double totalWork;
    }
}

