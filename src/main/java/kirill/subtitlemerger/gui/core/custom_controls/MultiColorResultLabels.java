package kirill.subtitlemerger.gui.core.custom_controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.core.entities.MultiPartResult;
import org.apache.commons.lang3.StringUtils;


public class MultiColorResultLabels extends HBox {
    private Label successLabel;

    private Label warnLabel;

    private Label errorLabel;

    private BooleanProperty wrapText;

    public MultiColorResultLabels() {
        wrapText = new SimpleBooleanProperty(false);
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

    public void update(MultiPartResult multiPartResult) {
        String success = null;
        String warn = null;
        String error = null;

        if (!StringUtils.isBlank(multiPartResult.getSuccess())) {
            success = multiPartResult.getSuccess();
            if (!StringUtils.isBlank(multiPartResult.getWarn()) || !StringUtils.isBlank(multiPartResult.getError())) {
                success += ", ";
            }
        }

        if (!StringUtils.isBlank(multiPartResult.getWarn())) {
            warn = multiPartResult.getWarn();
            if (!StringUtils.isBlank(multiPartResult.getError())) {
                warn += ", ";
            }
        }

        if (!StringUtils.isBlank(multiPartResult.getError())) {
            error = multiPartResult.getError();
        }

        successLabel.setText(success);
        warnLabel.setText(warn);
        errorLabel.setText(error);
    }

    public void clear() {
        update(MultiPartResult.EMPTY);
    }

    public void setOnlySuccess(String text) {
        update(MultiPartResult.onlySuccess(text));
    }

    public void setOnlyWarn(String text) {
        update(MultiPartResult.onlyWarn(text));
    }

    public void setOnlyError(String text) {
        update(MultiPartResult.onlyError(text));
    }
}
