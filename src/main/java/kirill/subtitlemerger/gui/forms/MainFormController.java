package kirill.subtitlemerger.gui.forms;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.forms.settings.SettingsFormController;
import kirill.subtitlemerger.gui.forms.subtitle_files.SubtitleFilesFormController;
import kirill.subtitlemerger.gui.forms.videos.VideosFormController;

public class MainFormController {
    @FXML
    private TabPane tabPane;

    @FXML
    private Tab settingsTab;

    @FXML
    private SubtitleFilesFormController subtitleFilesTabController;

    @FXML
    private VideosFormController videosTabController;

    @FXML
    private SettingsFormController settingsTabController;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.subtitleFilesTabController.initialize(stage, guiContext);
        this.videosTabController.initialize(this, stage, guiContext);
        this.settingsTabController.initialize(guiContext);
    }

    public void openSettingsTab() {
        tabPane.getSelectionModel().select(settingsTab);
    }
}
