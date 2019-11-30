package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import kirill.subtitlesmerger.logic.data.Config;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@CommonsLog
public class MergeInVideosTabController implements TabController{
    private MergeInVideosTabView tabView;

    private Config config;

    MergeInVideosTabController(MergeInVideosTabView tabView, Config config) {
        this.tabView = tabView;
        this.config = config;
    }

    @Override
    public void initialize() {
        tabView.setGoToSettingsLinkHandler(this::goToSettingsLinkClicked);

        updateView();
    }

    private void goToSettingsLinkClicked(ActionEvent event) {

    }

    private void updateView() {
        List<String> missingSettings = getMissingSettings(config);
        if (!CollectionUtils.isEmpty(missingSettings)) {
            tabView.showMissingSettings(missingSettings);
        } else {
            tabView.showRegularContent();
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

    @Override
    public TabView getTabView() {
        return tabView;
    }

    @Override
    public void tabClicked() {
        updateView();
    }
}
