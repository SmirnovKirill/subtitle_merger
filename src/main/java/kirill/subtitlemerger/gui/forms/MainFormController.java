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
    private SubtitleFilesFormController subtitleFilesFormController;

    @FXML
    private VideosFormController videosFormController;

    @FXML
    private SettingsFormController settingsFormController;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.subtitleFilesFormController.initialize(stage, guiContext);
        this.videosFormController.initialize(this, stage, guiContext);
        this.settingsFormController.initialize(guiContext);
    }

    public void openSettingsTab() {
        tabPane.getSelectionModel().select(settingsTab);
    }
}
