package kirill.subtitlesmerger.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.merge_in_directory_tab.MergeInDirectoryTabController;
import kirill.subtitlesmerger.logic.AppContext;

public class MainController {
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

    public void init(Stage stage, AppContext appContext) {
        this.mergeSingleFilesTabController.init(stage, appContext);
        this.mergeInDirectoryTabController.init(stage, this, appContext);
        this.settingsTabController.init(stage, appContext);
    }

    public void openSettingsTab() {
        mainTabPane.getSelectionModel().select(settingsTab);
    }
}
