package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import kirill.subtitlesmerger.logic.data.Config;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

@CommonsLog
public class MergeInVideosTabController implements TabController{
    private MergeInVideosTabView tabView;

    private Config config;

    private GuiLauncher guiLauncher;

    private List<File> files;

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

        tabView.setDirectoryPathLabel(directory.getAbsolutePath());

        try {
            config.saveLastDirectoryWithVideos(directory.getAbsolutePath());
        } catch (Config.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't be possible");
            throw new IllegalStateException();
        }
        tabView.setDirectoryChooserInitialDirectory(directory);

        files = getAllFiles(directory);
        if (CollectionUtils.isEmpty(files)) {
            tabView.showDirectoryErrorMessage("directory is empty");
            return;
        }

        tabView.showTableWithFiles();
    }

    private static List<File> getAllFiles(File directory) {
        File[] allFilesUnprocessed = directory.listFiles();
        if (allFilesUnprocessed == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(allFilesUnprocessed)
                .filter(file -> !file.isDirectory())
                .collect(toList());
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
