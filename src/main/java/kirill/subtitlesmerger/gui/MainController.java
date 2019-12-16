package kirill.subtitlesmerger.gui;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.merge_single_files_tab.MergeSingleFilesTabController;
import kirill.subtitlesmerger.logic.AppContext;

public class MainController implements Controller {
    private Stage stage;

    private AppContext appContext;

    @FXML
    private MergeSingleFilesTabController mergeSingleFilesTabController;

    @Override
    public void init(Stage stage, AppContext appContext) {
        this.stage = stage;
        this.appContext = appContext;
        this.mergeSingleFilesTabController.init(stage, appContext);
    }
}
