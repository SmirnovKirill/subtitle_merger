package kirill.subtitlemerger.gui.utils.entities;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import kirill.subtitlemerger.gui.utils.background_tasks.BackgroundTask;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public abstract class ControllerWithProgress {
    @FXML
    protected Pane mainPane;

    @FXML
    private Pane progressPane;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label progressLabel;

    @FXML
    private Pane cancelTaskPane;

    private BackgroundTask<?> currentTask;

    protected void startBackgroundTask(BackgroundTask<?> task) {
        mainPane.setDisable(true);
        progressPane.setVisible(true);
        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
        if (cancelTaskPane != null) {
            cancelTaskPane.visibleProperty().bind(task.cancellationPossibleProperty());
        }

        currentTask = task;
        task.start();
    }

    protected void stopProgress() {
        progressPane.setVisible(false);
        mainPane.setDisable(false);
    }

    @FXML
    private void cancelTaskClicked() {
        if (currentTask == null) {
            log.error("task is null, that shouldn't happen");
            return;
        }

        currentTask.cancel();
    }
}
