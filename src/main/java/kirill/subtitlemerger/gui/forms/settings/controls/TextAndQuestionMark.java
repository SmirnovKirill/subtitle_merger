package kirill.subtitlemerger.gui.forms.settings.controls;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import kirill.subtitlemerger.gui.utils.GuiUtils;

public class TextAndQuestionMark extends HBox {
    @SuppressWarnings("unused")
    @FXML
    private Label label;

    @SuppressWarnings("unused")
    @FXML
    private Tooltip tooltip;

    public TextAndQuestionMark() {
        GuiUtils.initializeControl(
                this,
                "/gui/javafx/forms/settings/controls/text_and_question_mark.fxml"
        );
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty labelTextProperty() {
        return label.textProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setLabelText(String text) {
        labelTextProperty().set(text);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getLabelText() {
        return labelTextProperty().get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty tooltipTextProperty() {
        return tooltip.textProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setTooltipText(String text) {
        tooltipTextProperty().set(text);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getTooltipText() {
        return tooltipTextProperty().get();
    }
}
