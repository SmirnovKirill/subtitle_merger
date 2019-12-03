package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import kirill.subtitlesmerger.logic.data.Config;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommonsLog
public class MergeInVideosTabController implements TabController{
    private MergeInVideosTabView tabView;

    private Config config;

    private GuiLauncher guiLauncher;

    private File[] files;

    MergeInVideosTabController(MergeInVideosTabView tabView, Config config, GuiLauncher guiLauncher) {
        this.tabView = tabView;
        this.config = config;
        this.guiLauncher = guiLauncher;
    }

    @Override
    public void initialize() {
        tabView.setGoToSettingsLinkHandler(this::goToSettingsLinkClicked);
        tabView.setDirectoryChooseButtonHandler(this::directoryButtonClicked);

        updateView();
    }

    private void goToSettingsLinkClicked(ActionEvent event) {
        guiLauncher.openSettingsTab();
    }

    private void directoryButtonClicked(ActionEvent event) {
        File directory = tabView.getChosenDirectory().orElse(null);
        if (directory == null) {
            return;
        }

        files = directory.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            tabView.showDirectoryErrorMessage("directory is empty");
            return;
        }

        tabView.setDirectoryPathLabel(directory.getAbsolutePath());
        tabView.showTableWithFiles();

        try {
            config.saveLastDirectoryWithVideos(directory.getAbsolutePath());
        } catch (Config.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't be possible");
            throw new IllegalStateException();
        }
        tabView.setDirectoryChooserInitialDirectory(directory);
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
