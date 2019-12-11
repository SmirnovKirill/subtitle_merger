package kirill.subtitlesmerger.gui.merge_in_directory_tab;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiLauncher;
import kirill.subtitlesmerger.gui.TabController;
import kirill.subtitlesmerger.gui.TabView;
import kirill.subtitlesmerger.logic.AppContext;
import kirill.subtitlesmerger.logic.Config;
import kirill.subtitlesmerger.logic.Constants;
import kirill.subtitlesmerger.logic.work_with_files.FileInfoGetter;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class MergeInDirectoryTabController implements TabController {
    private Stage stage;

    private MergeInDirectoryTabView tabView;

    private AppContext appContext;

    private GuiLauncher guiLauncher;

    private List<FileInfo> filesInfo;

    public MergeInDirectoryTabController(
            Stage stage,
            MergeInDirectoryTabView tabView,
            AppContext appContext,
            GuiLauncher guiLauncher
    ) {
        this.stage = stage;
        this.tabView = tabView;
        this.appContext = appContext;
        this.guiLauncher = guiLauncher;
    }

    @Override
    public void initialize() {
        tabView.getMissingSettingsPane().setGoToSettingsLinkHandler(this::goToSettingsLinkClicked);
        tabView.getRegularContentPane().setDirectoryChooseButtonHandler(this::directoryButtonClicked);
        tabView.getRegularContentPane().setHideUnavailableCheckBoxChangeListener(this::showOnlyValidCheckBoxChanged);

        updateView();
    }

    private void goToSettingsLinkClicked(ActionEvent event) {
        guiLauncher.openSettingsTab();
    }

    private void directoryButtonClicked(ActionEvent event) {
        File directory = tabView.getRegularContentPane().getChosenDirectory(stage).orElse(null);
        if (directory == null) {
            return;
        }

        tabView.showProgressIndicator();

        tabView.getRegularContentPane().setDirectoryPathLabel(directory.getAbsolutePath());

        try {
            appContext.getConfig().saveLastDirectoryWithVideos(directory.getAbsolutePath());
        } catch (Config.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't be possible");
            throw new IllegalStateException();
        }
        tabView.getRegularContentPane().setDirectoryChooserInitialDirectory(directory);

        filesInfo = getBriefFilesInfo(directory.listFiles(), appContext.getFfprobe());

        tabView.getRegularContentPane().showFiles(filesInfo);

        tabView.hideProgressIndicator();
    }

    private static List<FileInfo> getBriefFilesInfo(File[] files, Ffprobe ffprobe) {
        List<FileInfo> result = new ArrayList<>();

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            result.add(
                    FileInfoGetter.getFileInfoWithoutSubtitles(
                            file,
                            Constants.ALLOWED_VIDEO_EXTENSIONS,
                            Constants.ALLOWED_VIDEO_MIME_TYPES,
                            ffprobe
                    )
            );
        }

        return result;
    }

    private void showOnlyValidCheckBoxChanged(
            ObservableValue<? extends Boolean> observable,
            Boolean oldValue,
            Boolean newValue
    ) {
        tabView.showProgressIndicator();
        if (Boolean.TRUE.equals(newValue)) {
            tabView.getRegularContentPane().showFiles(
                    filesInfo.stream()
                    .filter(file -> file.getUnavailabilityReason() == null)
                    .collect(Collectors.toList())
            );
        } else {
            tabView.getRegularContentPane().showFiles(filesInfo);
        }
        tabView.hideProgressIndicator();
    }

    private void updateView() {
        List<String> missingSettings = getMissingSettings(appContext.getConfig());
        if (!CollectionUtils.isEmpty(missingSettings)) {
            tabView.getRegularContentPane().hide();
            tabView.getMissingSettingsPane().setMissingSettings(missingSettings);
            tabView.getMissingSettingsPane().show();
        } else {
            tabView.getMissingSettingsPane().hide();
            tabView.getRegularContentPane().show();
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
