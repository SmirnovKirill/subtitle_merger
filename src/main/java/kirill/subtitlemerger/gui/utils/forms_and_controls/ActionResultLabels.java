package kirill.subtitlemerger.gui.utils.forms_and_controls;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.entities.ActionResult;
import org.apache.commons.lang3.StringUtils;

/**
 * Special class to display data from the ActionResults class.
 * @see ActionResult
 */
public class ActionResultLabels extends HBox {
    @FXML
    private Label successLabel;

    @FXML
    private Label warnLabel;

    @FXML
    private Label errorLabel;

    private BooleanProperty wrapText;

    private BooleanProperty alwaysManaged;

    private BooleanProperty empty;

    public ActionResultLabels() {
        wrapText = new SimpleBooleanProperty(false);
        alwaysManaged = new SimpleBooleanProperty(false);
        empty = new SimpleBooleanProperty(true);

        GuiUtils.initializeCustomControl("/gui/javafx/forms_and_controls/action_result_labels.fxml", this);

        setVisible(false);
        managedProperty().bind(alwaysManaged.or(Bindings.not(empty)));

        successLabel.wrapTextProperty().bind(wrapText);
        warnLabel.wrapTextProperty().bind(wrapText);
        errorLabel.wrapTextProperty().bind(wrapText);
    }

    public boolean isWrapText() {
        return wrapText.get();
    }

    public BooleanProperty wrapTextProperty() {
        return wrapText;
    }

    public void setWrapText(boolean wrapText) {
        this.wrapText.set(wrapText);
    }

    public boolean isAlwaysManaged() {
        return alwaysManaged.get();
    }

    public BooleanProperty alwaysManagedProperty() {
        return alwaysManaged;
    }

    public void setAlwaysManaged(boolean alwaysManaged) {
        this.alwaysManaged.set(alwaysManaged);
    }

    public boolean isEmpty() {
        return empty.get();
    }

    public BooleanProperty emptyProperty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty.set(empty);
    }

    public void set(ActionResult multiPartResult) {
        String success = null;
        String warn = null;
        String error = null;

        boolean empty = true;

        if (!StringUtils.isBlank(multiPartResult.getSuccess())) {
            empty = false;

            success = multiPartResult.getSuccess();
            if (!StringUtils.isBlank(multiPartResult.getWarn()) || !StringUtils.isBlank(multiPartResult.getError())) {
                success += ", ";
            }
        }

        if (!StringUtils.isBlank(multiPartResult.getWarn())) {
            empty = false;

            warn = multiPartResult.getWarn();
            if (!StringUtils.isBlank(multiPartResult.getError())) {
                warn += ", ";
            }
        }

        if (!StringUtils.isBlank(multiPartResult.getError())) {
            empty = false;

            error = multiPartResult.getError();
        }

        successLabel.setText(success);
        warnLabel.setText(warn);
        errorLabel.setText(error);

        setEmpty(empty);
        setVisible(!empty);
    }

    public void clear() {
        set(ActionResult.NO_RESULT);
    }

    public void setOnlySuccess(String text) {
        set(ActionResult.onlySuccess(text));
    }

    public void setOnlyWarn(String text) {
        set(ActionResult.onlyWarn(text));
    }

    public void setOnlyError(String text) {
        set(ActionResult.onlyError(text));
    }
}
