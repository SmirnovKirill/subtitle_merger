package kirill.subtitlesmerger.gui.tabs.merge_in_directory;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

import java.util.List;

public class MissingSettingsController {
    @FXML
    private Pane pane;

    private MergeInDirectoryTabController mergeInDirectoryTabController;

    @FXML
    private Pane missingSettingLabelsPane;

    public void init(MergeInDirectoryTabController mergeInDirectoryTabController) {
        this.mergeInDirectoryTabController = mergeInDirectoryTabController;
    }

    void show() {
        pane.setVisible(true);
    }

    void hide() {
        pane.setVisible(false);
    }

    void setMissingSettings(List<String> missingSettings) {
        missingSettingLabelsPane.getChildren().clear();

        //todo separate control
        for (String setting : missingSettings) {
            missingSettingLabelsPane.getChildren().add(new Label("\u2022 " + setting));
        }
    }

    @FXML
    private void goToSettingsLinkClicked() {
        mergeInDirectoryTabController.openSettingsTab();
    }
}
