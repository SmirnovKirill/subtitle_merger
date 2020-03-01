package kirill.subtitlemerger.gui.application_specific;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerCallback;
import kirill.subtitlemerger.gui.utils.background.HelperTask;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public abstract class AbstractController {
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

    private HelperTask<?> currentTask;

    protected <T> void runInBackground(
            BackgroundRunner<T> backgroundTask,
            BackgroundRunnerCallback<T> taskCallback
    ) {
        HelperTask<T> task = new HelperTask<>(backgroundTask, taskCallback, this::stopProgress);

        mainPane.setDisable(true);
        progressPane.setVisible(true);
        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
        if (cancelTaskPane != null) {
            cancelTaskPane.visibleProperty().bind(task.getBackgroundRunnerManager().cancellationPossibleProperty());
        }

        currentTask = task;

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void stopProgress() {
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
