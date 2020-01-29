package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ProgressIndicator;

public abstract class CancellableBackgroundTask<T> extends BackgroundTask<T> {
    private BooleanProperty cancelFinished;

    private Runnable onCancelFinished;

    public CancellableBackgroundTask() {
        super();

        setOnCancelled(e -> {
            updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
            updateMessage("waiting for the task to cancel");
        });

        cancelFinished = new SimpleBooleanProperty(false);
        cancelFinished.addListener((observable, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                if (onCancelFinished != null) {
                    Platform.runLater(() -> onCancelFinished.run());
                }
            }
        });
    }

    public boolean getCancelFinished() {
        return cancelFinished.get();
    }

    public BooleanProperty cancelFinishedProperty() {
        return cancelFinished;
    }

    public void setCancelFinished(boolean cancelFinished) {
        this.cancelFinished.set(cancelFinished);
    }

    public void setOnCancelFinished(Runnable onCancelFinished) {
        this.onCancelFinished = onCancelFinished;
    }
}
