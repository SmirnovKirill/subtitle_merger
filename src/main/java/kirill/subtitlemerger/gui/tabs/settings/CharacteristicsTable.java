package kirill.subtitlemerger.gui.tabs.settings;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import lombok.Getter;

import java.io.IOException;

@Getter
public class CharacteristicsTable extends GridPane {
    @FXML
    private Label convenienceLabel;

    @FXML
    private Label safetyLabel;

    @FXML
    private Label diskUsageLabel;

    public CharacteristicsTable() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/gui/tabs/settings/characteristicsTable.fxml")
        );
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    public StringProperty convenienceTextProperty() {
        return convenienceLabel.textProperty();
    }

    public void setConvenienceText(String text) {
        convenienceTextProperty().set(text);
    }

    public String getConvenienceText() {
        return convenienceTextProperty().get();
    }

    public ObservableList<String> getConvenienceStyleClass() {
        return convenienceLabel.getStyleClass();
    }

    public StringProperty safetyTextProperty() {
        return safetyLabel.textProperty();
    }

    public void setSafetyText(String text) {
        safetyTextProperty().set(text);
    }

    public String getSafetyText() {
        return safetyTextProperty().get();
    }

    public ObservableList<String> getSafetyStyleClass() {
        return safetyLabel.getStyleClass();
    }

    public StringProperty diskUsageTextProperty() {
        return diskUsageLabel.textProperty();
    }

    public void setDiskUsageText(String text) {
        diskUsageTextProperty().set(text);
    }

    public String getDiskUsageText() {
        return diskUsageTextProperty().get();
    }

    public ObservableList<String> getDiskUsageStyleClass() {
        return diskUsageLabel.getStyleClass();
    }
}
