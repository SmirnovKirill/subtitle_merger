package kirill.subtitlemerger.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.settings_tab.SettingsTabController;
import kirill.subtitlemerger.gui.subtitle_files_tab.SubtitleFilesTabController;
import kirill.subtitlemerger.gui.videos_tab.VideosTabController;

public class MainPaneController {
    @FXML
    private TabPane tabPane;

    @FXML
    private Tab settingsTab;

    @FXML
    private SubtitleFilesTabController subtitleFilesTabController;

    @FXML
    private VideosTabController videosTabController;

    @FXML
    private SettingsTabController settingsTabController;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.subtitleFilesTabController.initialize(stage, guiContext);
        this.videosTabController.initialize(this, stage, guiContext);
        this.settingsTabController.initialize(stage, guiContext);
    }

    public void openSettingsTab() {
        tabPane.getSelectionModel().select(settingsTab);
    }
}
