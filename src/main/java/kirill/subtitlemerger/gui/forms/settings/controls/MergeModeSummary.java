package kirill.subtitlemerger.gui.forms.settings.controls;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import kirill.subtitlemerger.gui.utils.GuiUtils;

public class MergeModeSummary extends GridPane {
    @SuppressWarnings("unused")
    @FXML
    private Label convenienceLabel;

    @SuppressWarnings("unused")
    @FXML
    private Label safetyLabel;

    @SuppressWarnings("unused")
    @FXML
    private Label diskUsageLabel;

    public MergeModeSummary() {
        GuiUtils.initializeControl(this, "/gui/javafx/forms/settings/controls/merge_mode_summary.fxml");
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty convenienceTextProperty() {
        return convenienceLabel.textProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setConvenienceText(String text) {
        convenienceTextProperty().set(text);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getConvenienceText() {
        return convenienceTextProperty().get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObservableList<String> getConvenienceStyleClass() {
        return convenienceLabel.getStyleClass();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty safetyTextProperty() {
        return safetyLabel.textProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setSafetyText(String text) {
        safetyTextProperty().set(text);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getSafetyText() {
        return safetyTextProperty().get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObservableList<String> getSafetyStyleClass() {
        return safetyLabel.getStyleClass();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty diskUsageTextProperty() {
        return diskUsageLabel.textProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setDiskUsageText(String text) {
        diskUsageTextProperty().set(text);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getDiskUsageText() {
        return diskUsageTextProperty().get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObservableList<String> getDiskUsageStyleClass() {
        return diskUsageLabel.getStyleClass();
    }
}
