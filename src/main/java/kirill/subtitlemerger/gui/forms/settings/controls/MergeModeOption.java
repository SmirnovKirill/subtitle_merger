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
    @SuppressWarnings("unused")
    @FXML
    private RadioButton radioButton;

    @SuppressWarnings("unused")
    @FXML
    private Tooltip tooltip;

    public MergeModeOption() {
        GuiUtils.initializeControl(this, "/gui/javafx/forms/settings/controls/merge_mode_option.fxml");
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty textProperty() {
        return radioButton.textProperty();
    }
    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})

    public void setText(String text) {
        textProperty().set(text);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getText() {
        return textProperty().get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return radioButton.toggleGroupProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setToggleGroup(ToggleGroup toggleGroup) {
        toggleGroupProperty().set(toggleGroup);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ToggleGroup getToggleGroup() {
        return toggleGroupProperty().get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty tooltipProperty() {
        return tooltip.textProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setTooltip(String text) {
        tooltipProperty().set(text);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getTooltip() {
        return tooltipProperty().get();
    }
}
