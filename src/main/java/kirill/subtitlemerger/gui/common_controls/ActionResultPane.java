package kirill.subtitlemerger.gui.common_controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import org.apache.commons.lang3.StringUtils;

/**
 * A control to display data from the ActionResult class in three different colors - the error part in red, the part
 * with warnings in orange and the part with success in green.
 *
 * @see ActionResult
 */
public class ActionResultPane extends HBox {
    @SuppressWarnings("unused")
    @FXML
    private Label successLabel;

    @SuppressWarnings("unused")
    @FXML
    private Label warnLabel;

    @SuppressWarnings("unused")
    @FXML
    private Label errorLabel;

    private BooleanProperty wrapText;

    private BooleanProperty alwaysManaged;

    private ObjectProperty<ActionResult> actionResult;

    public ActionResultPane() {
        this(ActionResult.NO_RESULT);
    }

    public ActionResultPane(ActionResult actionResult) {
        GuiUtils.initializeControl(this, "/gui/javafx/common_controls/action_result_pane.fxml");

        wrapText = new SimpleBooleanProperty(false);
        alwaysManaged = new SimpleBooleanProperty(false);
        this.actionResult = new SimpleObjectProperty<>(actionResult);

        successLabel.wrapTextProperty().bind(wrapText);
        warnLabel.wrapTextProperty().bind(wrapText);
        errorLabel.wrapTextProperty().bind(wrapText);

        actionResultProperty().addListener(observable -> updateScene(getActionResult()));
        alwaysManaged.addListener(observable -> updateScene(getActionResult()));
        updateScene(actionResult);
    }

    private void updateScene(ActionResult actionResult) {
        String success = null;
        String warn = null;
        String error = null;

        boolean empty = true;

        if (actionResult != null) {
            if (!StringUtils.isBlank(actionResult.getSuccess())) {
                empty = false;

                success = actionResult.getSuccess();
                if (!StringUtils.isBlank(actionResult.getWarn()) || !StringUtils.isBlank(actionResult.getError())) {
                    success += ", ";
                }
            }

            if (!StringUtils.isBlank(actionResult.getWarn())) {
                empty = false;

                warn = actionResult.getWarn();
                if (!StringUtils.isBlank(actionResult.getError())) {
                    warn += ", ";
                }
            }

            if (!StringUtils.isBlank(actionResult.getError())) {
                empty = false;

                error = actionResult.getError();
            }
        }

        successLabel.setText(success);
        warnLabel.setText(warn);
        errorLabel.setText(error);

        setVisible(!empty);
        setManaged(isAlwaysManaged() || !empty);
    }

    public void clear() {
        setActionResult(ActionResult.NO_RESULT);
    }

    public void setOnlySuccess(String text) {
        setActionResult(ActionResult.onlySuccess(text));
    }

    public void setOnlyWarn(String text) {
        setActionResult(ActionResult.onlyWarn(text));
    }

    public void setOnlyError(String text) {
        setActionResult(ActionResult.onlyError(text));
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean isWrapText() {
        return wrapText.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty wrapTextProperty() {
        return wrapText;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setWrapText(boolean wrapText) {
        this.wrapText.set(wrapText);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean isAlwaysManaged() {
        return alwaysManaged.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty alwaysManagedProperty() {
        return alwaysManaged;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setAlwaysManaged(boolean alwaysManaged) {
        this.alwaysManaged.set(alwaysManaged);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ActionResult getActionResult() {
        return actionResult.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<ActionResult> actionResultProperty() {
        return actionResult;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setActionResult(ActionResult actionResult) {
        this.actionResult.set(actionResult);
    }
}
