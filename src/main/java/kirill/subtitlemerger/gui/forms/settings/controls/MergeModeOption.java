package kirill.subtitlemerger.gui.forms.settings.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import kirill.subtitlemerger.gui.utils.GuiUtils;

public class MergeModeOption extends HBox {
    @FXML
    private RadioButton radioButton;

    @FXML
    private Tooltip tooltip;

    public MergeModeOption() {
        GuiUtils.initializeControl(this, "/gui/javafx/forms/settings/controls/merge_mode_option.fxml");
    }

    public StringProperty textProperty() {
        return radioButton.textProperty();
    }

    public void setText(String text) {
        textProperty().set(text);
    }

    public String getText() {
        return textProperty().get();
    }

    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return radioButton.toggleGroupProperty();
    }

    public void setToggleGroup(ToggleGroup toggleGroup) {
        toggleGroupProperty().set(toggleGroup);
    }

    public ToggleGroup getToggleGroup() {
        return toggleGroupProperty().get();
    }

    public StringProperty tooltipProperty() {
        return tooltip.textProperty();
    }

    public void setTooltip(String text) {
        tooltipProperty().set(text);
    }

    public String getTooltip() {
        return tooltipProperty().get();
    }
}
