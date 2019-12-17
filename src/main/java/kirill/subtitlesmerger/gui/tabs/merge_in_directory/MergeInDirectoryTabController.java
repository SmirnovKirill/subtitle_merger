package kirill.subtitlesmerger.gui.tabs.merge_in_directory;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.tabs.TabController;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.GuiPreferences;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@CommonsLog
public class MergeInDirectoryTabController {
    private TabController tabController;

    private GuiContext guiContext;

    @FXML
    private MissingSettingsController missingSettingsController;

    @FXML
    private RegularContentController regularContentController;

    public void initialize(Stage stage, TabController tabController, GuiContext guiContext) {
        this.tabController = tabController;
        this.guiContext = guiContext;

        this.missingSettingsController.init(this);
        this.regularContentController.init(stage, guiContext);

        updateView();
    }

    private void updateView() {
        List<String> missingSettings = getMissingSettings(guiContext.getConfig());
        if (!CollectionUtils.isEmpty(missingSettings)) {
            regularContentController.hide();
            missingSettingsController.setMissingSettings(missingSettings);
            missingSettingsController.show();
        } else {
            missingSettingsController.hide();
            regularContentController.show();
        }
    }

    private static List<String> getMissingSettings(GuiPreferences config) {
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
        tabController.openSettingsTab();
    }
}
