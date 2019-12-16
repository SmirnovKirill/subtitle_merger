package kirill.subtitlesmerger.gui;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import kirill.subtitlesmerger.logic.AppContext;

public class MainController implements Controller {
    private Stage stage;

    private AppContext appContext;

    @FXML
    private MergeSingleFilesTabController mergeSingleFilesTabController;

    @FXML
    private SettingsTabController settingsTabController;

    @Override
    public void init(Stage stage, AppContext appContext) {
        this.stage = stage;
        this.appContext = appContext;
        this.mergeSingleFilesTabController.init(stage, appContext);
        this.settingsTabController.init(stage, appContext);
    }
}
