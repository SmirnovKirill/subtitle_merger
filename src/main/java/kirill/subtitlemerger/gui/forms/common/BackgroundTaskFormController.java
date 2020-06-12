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
 * that takes care of pane management (manages the main and the progress panes), task canceling and so forth.
 */
@CommonsLog
public abstract class BackgroundTaskFormController {
    @FXML
    protected Pane mainPane;

    @FXML
    protected ProgressPane progressPane;

    private BackgroundManager backgroundManager;

    protected <T> void runInBackground(BackgroundRunner<T> runner, BackgroundCallback<T> callback) {
        BackgroundCallback<T> extendedCallback = (result) -> {
            progressPane.setVisible(false);
            mainPane.setDisable(false);
            backgroundManager = null;

            callback.run(result);
        };
        backgroundManager = Background.run(runner, extendedCallback);

        progressPane.progressProperty().bind(backgroundManager.progressProperty());
        progressPane.messageProperty().bind(backgroundManager.messageProperty());
        progressPane.cancelPossibleProperty().bind(backgroundManager.cancelPossibleProperty());
        progressPane.cancelDescriptionProperty().bind(backgroundManager.cancelDescriptionProperty());
        progressPane.setOnCancelAction(event -> cancelTaskClicked());

        mainPane.setDisable(true);
        progressPane.setVisible(true);
    }

    @FXML
    private void cancelTaskClicked() {
        if (backgroundManager == null) {
            log.error("background manager is null, most likely a bug");
            throw new IllegalStateException();
        }
        if (!backgroundManager.getCancelPossible()) {
            log.error("canceling is not allowed, most likely a bug");
            throw new IllegalStateException();
        }

        backgroundManager.cancel();
    }
}
