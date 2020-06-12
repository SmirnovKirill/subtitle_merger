package kirill.subtitlemerger.gui.common_controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

/**
 * A special label that displays the text in one of three colors - green in case of success, orange for warnings and red
 * for errors. Also this label is invisible and not managed (by default) if there is no text. Because this class extends
 * a regular label it has all the label's benefits - you can choose whether to wrap the text and the text can be clipped
 * with ellipsis at the end.
 * Note that there is also a similar class MultiPartActionResultPane for multi-color texts. It uses another approach and
 * uses texts instead of labels and thus is not so convenient because texts can't be clipped with ellipsis.
 *
 * @see ActionResult
 * @see MultiPartActionResultPane
 */
@CommonsLog
public class ActionResultLabel extends Label {
    private BooleanProperty alwaysManaged;

    private ObjectProperty<ActionResult> actionResult;

    @SuppressWarnings("unused")
    public ActionResultLabel() {
        this(ActionResult.EMPTY);
    }

    @SuppressWarnings("WeakerAccess")
    public ActionResultLabel(ActionResult actionResult) {
        alwaysManaged = new SimpleBooleanProperty(false);
        this.actionResult = new SimpleObjectProperty<>(actionResult);

        this.actionResult.addListener(observable -> updateScene(getActionResult()));
        alwaysManaged.addListener(observable -> updateScene(getActionResult()));
        updateScene(actionResult);
    }

    private void updateScene(ActionResult actionResult) {
        setText(actionResult.getText());

        getStyleClass().clear();
        switch (actionResult.getType()) {
            case SUCCESS:
                getStyleClass().add(GuiConstants.LABEL_SUCCESS_CLASS);
                break;
            case WARNING:
                getStyleClass().add(GuiConstants.LABEL_WARNING_CLASS);
                break;
            case ERROR:
                getStyleClass().add(GuiConstants.LABEL_ERROR_CLASS);
                break;
            default:
                log.error("unexpected action result type: " + actionResult.getType() + ", most likely a bug");
                throw new IllegalStateException();
        }

        setVisible(!StringUtils.isBlank(actionResult.getText()));
        setManaged(isAlwaysManaged() || !StringUtils.isBlank(actionResult.getText()));
    }

    public void clear() {
        setActionResult(ActionResult.EMPTY);
    }

    public void setSuccess(String text) {
        setActionResult(ActionResult.success(text));
    }

    public void setWarning(String text) {
        setActionResult(ActionResult.warning(text));
    }

    public void setError(String text) {
        setActionResult(ActionResult.error(text));
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
