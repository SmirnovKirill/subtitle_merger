package kirill.subtitlemerger.gui.tabs.settings;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class LabelWithQuestionMark extends HBox {
    @FXML
    private Label label;

    @FXML
    private Tooltip tooltip;

    public LabelWithQuestionMark() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/gui/tabs/settings/labelWithQuestionMark.fxml")
        );
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
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
