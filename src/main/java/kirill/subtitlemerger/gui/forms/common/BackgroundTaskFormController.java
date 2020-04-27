package kirill.subtitlemerger.gui.forms.common;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import kirill.subtitlemerger.gui.common_controls.ProgressPane;
import kirill.subtitlemerger.gui.utils.background.Background;
import kirill.subtitlemerger.gui.utils.background.BackgroundCallback;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import lombok.extern.apachecommons.CommonsLog;

/**
 * A parent class for controllers of the forms that can run background tasks. It has a helper method runInBackground
 * that takes care of the pane management (manages the main and the progress panes), task cancellation and so forth.
 */
@CommonsLog
public abstract class BackgroundTaskFormController {
    @FXML
    protected Pane mainPane;

    @FXML
    protected ProgressPane progressPane;

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

        progressPane.progressProperty().bind(backgroundManager.progressProperty());
        progressPane.messageProperty().bind(backgroundManager.messageProperty());
        progressPane.cancellationPossibleProperty().bind(backgroundManager.cancellationPossibleProperty());
        progressPane.cancellationDescriptionProperty().bind(backgroundManager.cancellationDescriptionProperty());
        progressPane.setOnCancelAction(event -> cancelTaskClicked());
    }

    @FXML
    private void cancelTaskClicked() {
        if (backgroundManager == null) {
            log.error("background manager is null, most likely a bug");
            return;
        }

        backgroundManager.cancel();
    }
}
