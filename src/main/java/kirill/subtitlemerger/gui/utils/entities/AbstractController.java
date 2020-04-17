package kirill.subtitlemerger.gui.utils.entities;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
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

    private HelperTask<?> task;

    protected <T> void runInBackground(BackgroundRunner<T> runner, BackgroundCallback<T> callback) {
        task = new HelperTask<>(runner, callback, this::stopProgress);

        mainPane.setDisable(true);
        progressPane.setVisible(true);
        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
        if (cancelTaskPane != null) {
            cancelTaskPane.visibleProperty().bind(task.getManager().cancellationPossibleProperty());

            /*
             * Besides the description that can be set through the manager there is always a text after this description
             * offering to click the link to cancel the task. So if the description is set through the manager there
             * should be a space after that description so that texts won't be too close.
             */
            StringBinding descriptionBinding = Bindings.createStringBinding(
                    () -> {
                        String description = task.getManager().getCancellationDescription();
                        return StringUtils.isBlank(description) ? "" : description + " ";
                    },
                    task.getManager().cancellationDescriptionProperty()
            );
            cancelDescriptionLabel.textProperty().bind(descriptionBinding);
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
        if (task == null) {
            log.error("task is null, that shouldn't happen");
            return;
        }

        task.cancel();
    }
}
