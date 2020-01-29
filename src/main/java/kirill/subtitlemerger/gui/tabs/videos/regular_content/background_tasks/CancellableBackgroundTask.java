package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.control.ProgressIndicator;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public abstract class CancellableBackgroundTask<T> extends Task<T> {
    private BooleanProperty finished;

    @Setter
    private Runnable onFinished;

    public CancellableBackgroundTask(BooleanProperty cancelTaskPaneVisible) {
        super();

        setOnSucceeded(event -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });

        setOnFailed(this::taskFailed);

        setOnCancelled(e -> {
            Platform.runLater(() -> cancelTaskPaneVisible.set(false));
            updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
            updateMessage("waiting for the task to cancel");
        });

        finished = new SimpleBooleanProperty(false);
        finished.addListener((observable, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                if (onFinished != null) {
                    Platform.runLater(() -> onFinished.run());
                }
            }
        });
    }

    private void taskFailed(Event e) {
        log.error("task has failed, shouldn't happen");
        throw new IllegalStateException();
    }

    public boolean getFinished() {
        return finished.get();
    }

    public BooleanProperty finishedProperty() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished.set(finished);
    }
}
