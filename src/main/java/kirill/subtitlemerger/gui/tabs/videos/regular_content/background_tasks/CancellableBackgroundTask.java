package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ProgressIndicator;

import java.util.function.Consumer;

public abstract class CancellableBackgroundTask<T> extends BackgroundTask<T> {
    private BooleanProperty finished;

    public CancellableBackgroundTask(Consumer<CancellableBackgroundTask> onFinished) {
        super();

        setOnSucceeded(event -> onFinished.accept(this));

        setOnCancelled(e -> {
            updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
            updateMessage("waiting for the task to cancel");
        });

        finished = new SimpleBooleanProperty(false);
        finished.addListener((observable, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                if (onFinished != null) {
                    Platform.runLater(() -> onFinished.accept(this));
                }
            }
        });
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
