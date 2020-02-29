package kirill.subtitlemerger.gui.application_specific.settings_tab;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import kirill.subtitlemerger.gui.utils.GuiUtils;

public class LabelWithQuestionMark extends HBox {
    @FXML
    private Label label;

    @FXML
    private Tooltip tooltip;

    public LabelWithQuestionMark() {
        GuiUtils.initializeCustomControl("/gui/application_specific/settings_tab/labelWithQuestionMark.fxml", this);
    }

    public StringProperty labelTextProperty() {
        return label.textProperty();
    }

    public void setLabelText(String text) {
        labelTextProperty().set(text);
    }

    public String getLabelText() {
        return labelTextProperty().get();
    }

    public StringProperty tooltipTextProperty() {
        return tooltip.textProperty();
    }

    public void setTooltipText(String text) {
        tooltipTextProperty().set(text);
    }

    public String getTooltipText() {
        return tooltipTextProperty().get();
    }
}
