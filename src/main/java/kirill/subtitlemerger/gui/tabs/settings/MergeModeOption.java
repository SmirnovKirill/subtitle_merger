package kirill.subtitlemerger.gui.tabs.settings;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
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

    public StringProperty radioButtonTextProperty() {
        return radioButton.textProperty();
    }

    public void setRadioButtonText(String text) {
        radioButtonTextProperty().set(text);
    }

    public String getRadioButtonText() {
        return radioButtonTextProperty().get();
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
