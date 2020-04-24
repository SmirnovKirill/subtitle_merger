package kirill.subtitlemerger.gui.forms.common;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import kirill.subtitlemerger.gui.utils.background.Background;
import kirill.subtitlemerger.gui.utils.background.BackgroundCallback;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

/**
 * A parent class for controllers of the forms that can run background tasks. It has a helper method runInBackground
 * that takes care of the pane management (manages the main and the progress panes), task cancellation and so forth.
 */
@CommonsLog
public abstract class BackgroundTaskFormController {
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

    private BackgroundManager backgroundManager;

    protected <T> void runInBackground(BackgroundRunner<T> runner, BackgroundCallback<T> callback) {
        mainPane.setDisable(true);
        progressPane.setVisible(true);

        BackgroundCallback<T> extendedCallback = (result) -> {
            progressPane.setVisible(false);
            mainPane.setDisable(false);

            callback.run(result);
            backgroundManager = null;
        };
        backgroundManager = Background.run(runner, extendedCallback);

        progressIndicator.progressProperty().bind(backgroundManager.progressProperty());
        progressLabel.textProperty().bind(backgroundManager.messageProperty());
        if (cancelTaskPane != null) {
            cancelTaskPane.visibleProperty().bind(backgroundManager.cancellationPossibleProperty());

            /*
             * Besides the description that can be set through the manager there is always a text after this description
             * offering to click the link to cancel the task. So if the description is set through the manager there
             * should be a space after that description so that texts won't be too close.
             */
            StringBinding descriptionBinding = Bindings.createStringBinding(
                    () -> {
                        String description = backgroundManager.getCancellationDescription();
                        return StringUtils.isBlank(description) ? "" : description + " ";
                    },
                    backgroundManager.cancellationDescriptionProperty()
            );
            cancelDescriptionLabel.textProperty().bind(descriptionBinding);
        }
    }

    @FXML
    private void cancelTaskClicked() {
        if (backgroundManager == null) {
            log.error("background manager is null, that shouldn't happen");
            return;
        }

        backgroundManager.cancel();
    }
}
