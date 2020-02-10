package kirill.subtitlemerger.gui.core.custom_controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.core.entities.MultiPartResult;
import org.apache.commons.lang3.StringUtils;


public class MultiColorResultLabels extends HBox {
    private StringProperty success;

    private StringProperty warn;

    private StringProperty error;

    public MultiColorResultLabels() {
        success = new SimpleStringProperty();
        warn = new SimpleStringProperty();
        error = new SimpleStringProperty();

        Label successLabel = new Label();
        successLabel.textProperty().bind(success);
        successLabel.getStyleClass().add(GuiConstants.LABEL_SUCCESS_CLASS);

        Label warnLabel = new Label();
        warnLabel.textProperty().bind(warn);
        warnLabel.getStyleClass().add(GuiConstants.LABEL_WARN_CLASS);

        Label errorLabel = new Label();
        errorLabel.textProperty().bind(error);
        errorLabel.getStyleClass().add(GuiConstants.LABEL_ERROR_CLASS);

        getChildren().addAll(successLabel, warnLabel, errorLabel);
    }

    public String getSuccess() {
        return success.get();
    }

    public StringProperty successProperty() {
        return success;
    }

    public void setSuccess(String success) {
        this.success.set(success);
    }

    public String getWarn() {
        return warn.get();
    }

    public StringProperty warnProperty() {
        return warn;
    }

    public void setWarn(String warn) {
        this.warn.set(warn);
    }

    public String getError() {
        return error.get();
    }

    public StringProperty errorProperty() {
        return error;
    }

    public void setError(String error) {
        this.error.set(error);
    }

    public void clear() {
        setSuccess(null);
        setWarn(null);
        setError(null);
    }

    public void update(MultiPartResult multiPartResult) {
        clear();

        if (!StringUtils.isBlank(multiPartResult.getSuccess())) {
            setSuccess(multiPartResult.getSuccess());

            if (!StringUtils.isBlank(multiPartResult.getWarn()) || !StringUtils.isBlank(multiPartResult.getError())) {
                setSuccess(getSuccess() + ", ");
            }
        }

        if (!StringUtils.isBlank(multiPartResult.getWarn())) {
            setWarn(multiPartResult.getWarn());

            if (!StringUtils.isBlank(multiPartResult.getError())) {
                setWarn(getWarn() + ", ");
            }
        }

        if (!StringUtils.isBlank(multiPartResult.getError())) {
            setError(multiPartResult.getError());
        }
    }
}
