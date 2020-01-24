package kirill.subtitlemerger.gui.tabs.settings;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import lombok.Getter;

import java.io.IOException;

@Getter
public class MergeModeOption extends HBox {
    @FXML
    private RadioButton radioButton;

    @FXML
    private Tooltip tooltip;

    public MergeModeOption() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/gui/tabs/settings/mergeModeOption.fxml")
        );
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
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
