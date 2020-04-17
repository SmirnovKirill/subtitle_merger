package kirill.subtitlemerger.gui.utils.entities;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundCallback;
import kirill.subtitlemerger.gui.utils.background.HelperTask;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

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

    @FXML
    private Label cancelDescriptionLabel;

    private HelperTask<?> currentTask;

    protected <T> void runInBackground(BackgroundRunner<T> backgroundTask, BackgroundCallback<T> taskCallback) {
        HelperTask<T> task = new HelperTask<>(backgroundTask, taskCallback, this::stopProgress);
        currentTask = task;

        mainPane.setDisable(true);
        progressPane.setVisible(true);
        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
        if (cancelTaskPane != null) {
            cancelDescriptionLabel.textProperty().bind(
                    Bindings.createStringBinding(
                            () -> {
                                String description = task.getBackgroundManager().getCancellationDescription();
                                return StringUtils.isBlank(description) ? "" : description + " ";
                            },
                            task.getBackgroundManager().cancellationDescriptionProperty()
                    )
            );
            cancelTaskPane.visibleProperty().bind(task.getBackgroundManager().cancellationPossibleProperty());
        }

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
