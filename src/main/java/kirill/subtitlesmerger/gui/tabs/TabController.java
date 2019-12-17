package kirill.subtitlesmerger.gui.tabs;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.tabs.merge_in_directory.MergeInDirectoryTabController;
import kirill.subtitlesmerger.gui.tabs.merge_single_files.MergeSingleFilesTabController;
import kirill.subtitlesmerger.gui.tabs.settings.SettingsTabController;

public class TabController {
    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab settingsTab;

    @FXML
    private MergeSingleFilesTabController mergeSingleFilesTabController;

    @FXML
    private MergeInDirectoryTabController mergeInDirectoryTabController;

    @FXML
    private SettingsTabController settingsTabController;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.mergeSingleFilesTabController.initialize(stage, guiContext);
        this.mergeInDirectoryTabController.initialize(stage, this, guiContext);
        this.settingsTabController.initialize(stage, guiContext);
    }

    public void openSettingsTab() {
        mainTabPane.getSelectionModel().select(settingsTab);
    }
}
