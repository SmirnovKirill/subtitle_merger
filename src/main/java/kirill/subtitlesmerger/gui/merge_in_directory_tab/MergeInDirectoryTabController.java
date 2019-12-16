package kirill.subtitlesmerger.gui.merge_in_directory_tab;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.MainController;
import kirill.subtitlesmerger.logic.AppContext;
import kirill.subtitlesmerger.logic.Config;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@CommonsLog
public class MergeInDirectoryTabController {
    private MainController mainController;

    private AppContext appContext;

    @FXML
    private MissingSettingsController missingSettingsController;

    @FXML
    private RegularContentController regularContentController;

    public void init(Stage stage, MainController mainController, AppContext appContext) {
        this.mainController = mainController;
        this.appContext = appContext;

        this.missingSettingsController.init(this);
        this.regularContentController.init(stage, appContext);

        updateView();
    }

    private void updateView() {
        List<String> missingSettings = getMissingSettings(appContext.getConfig());
        if (!CollectionUtils.isEmpty(missingSettings)) {
            regularContentController.hide();
            missingSettingsController.setMissingSettings(missingSettings);
            missingSettingsController.show();
        } else {
            missingSettingsController.hide();
            regularContentController.show();
        }
    }

    private static List<String> getMissingSettings(Config config) {
        List<String> result = new ArrayList<>();

        if (config.getFfprobeFile() == null) {
            result.add("path to ffprobe");
        }

        if (config.getFfmpegFile() == null) {
            result.add("path to ffmpeg");
        }

        if (config.getUpperLanguage() == null) {
            result.add("preferred language for upper subtitles");
        }

        if (config.getLowerLanguage() == null) {
            result.add("preferred language for lower subtitles");
        }

        return result;
    }

    void openSettingsTab() {
        mainController.openSettingsTab();
    }
}
