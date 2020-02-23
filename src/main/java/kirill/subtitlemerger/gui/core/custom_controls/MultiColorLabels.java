package kirill.subtitlemerger.gui.core.custom_controls;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.core.entities.MultiPartResult;
import org.apache.commons.lang3.StringUtils;


public class MultiColorLabels extends HBox {
    private Label successLabel;

    private Label warnLabel;

    private Label errorLabel;

    private BooleanProperty wrapText;

    private BooleanProperty alwaysManaged;

    private BooleanProperty empty;

    public MultiColorLabels() {
        wrapText = new SimpleBooleanProperty(false);
        alwaysManaged = new SimpleBooleanProperty(false);
        empty = new SimpleBooleanProperty(true);

        setVisible(false);
        managedProperty().bind(alwaysManaged.or(Bindings.not(empty)));

        successLabel = new Label();
        successLabel.getStyleClass().add(GuiConstants.LABEL_SUCCESS_CLASS);
        successLabel.wrapTextProperty().bind(wrapText);

        warnLabel = new Label();
        warnLabel.getStyleClass().add(GuiConstants.LABEL_WARN_CLASS);
        warnLabel.wrapTextProperty().bind(wrapText);

        errorLabel = new Label();
        errorLabel.getStyleClass().add(GuiConstants.LABEL_ERROR_CLASS);
        errorLabel.wrapTextProperty().bind(wrapText);

        getChildren().addAll(successLabel, warnLabel, errorLabel);
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

    public void set(MultiPartResult multiPartResult) {
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
        set(MultiPartResult.EMPTY);
    }

    public void setOnlySuccess(String text) {
        set(MultiPartResult.onlySuccess(text));
    }

    public void setOnlyWarn(String text) {
        set(MultiPartResult.onlyWarn(text));
    }

    public void setOnlyError(String text) {
        set(MultiPartResult.onlyError(text));
    }
}
