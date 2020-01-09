package kirill.subtitlemerger.gui.tabs;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.tabs.videos.VideosTabController;
import kirill.subtitlemerger.gui.tabs.subtitle_files.SubtitleFilesTabController;
import kirill.subtitlemerger.gui.tabs.settings.SettingsTabController;

public class TabPaneController {
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
        this.videosTabController.initialize(stage, this, guiContext);
        this.settingsTabController.initialize(stage, guiContext);
    }

    public void openSettingsTab() {
        tabPane.getSelectionModel().select(settingsTab);
    }
}
