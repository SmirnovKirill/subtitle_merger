package kirill.subtitlesmerger.gui.merge_in_directory_tab;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiLauncher;
import kirill.subtitlesmerger.gui.TabController;
import kirill.subtitlesmerger.gui.TabView;
import kirill.subtitlesmerger.logic.Constants;
import kirill.subtitlesmerger.logic.work_with_files.entities.BriefFileInfo;
import kirill.subtitlesmerger.logic.Config;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static kirill.subtitlesmerger.logic.work_with_files.entities.BriefFileInfo.UnavailabilityReason.*;

@CommonsLog
public class MergeInDirectoryTabController implements TabController {
    private Stage stage;

    private MergeInDirectoryTabView tabView;

    private Config config;

    private GuiLauncher guiLauncher;

    private List<BriefFileInfo> briefFilesInfo;

    public MergeInDirectoryTabController(
            Stage stage,
            MergeInDirectoryTabView tabView,
            Config config,
            GuiLauncher guiLauncher
    ) {
        this.stage = stage;
        this.tabView = tabView;
        this.config = config;
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
            config.saveLastDirectoryWithVideos(directory.getAbsolutePath());
        } catch (Config.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't be possible");
            throw new IllegalStateException();
        }
        tabView.getRegularContentPane().setDirectoryChooserInitialDirectory(directory);

        briefFilesInfo = getBriefFilesInfo(directory.listFiles());

        tabView.getRegularContentPane().setFiles(briefFilesInfo);

        tabView.hideProgressIndicator();
    }

    private static List<BriefFileInfo> getBriefFilesInfo(File[] files) {
        List<BriefFileInfo> result = new ArrayList<>();

        if (files == null) {
            return result;
        }

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String extension = FilenameUtils.getExtension(file.getName());
            if (StringUtils.isBlank(extension)) {
                result.add(new BriefFileInfo(file, NO_EXTENSION, null, null));
                continue;
            }

            if (!Constants.ALLOWED_VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
                result.add(new BriefFileInfo(file, NOT_ALLOWED_EXTENSION, null, null));
                continue;
            }

            String mimeType;
            try {
                mimeType = Files.probeContentType(file.toPath());
            } catch (IOException e) {
                result.add(new BriefFileInfo(file, FAILED_TO_GET_MIME_TYPE, null, null));
                continue;
            }

            if (!Constants.ALLOWED_VIDEO_MIME_TYPES.contains(mimeType)) {
                result.add(new BriefFileInfo(file, NOT_ALLOWED_MIME_TYPE, null, null));
                continue;
            }

            result.add(
                    new BriefFileInfo(file, null, null, null)
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
            tabView.getRegularContentPane().setFiles(
                    briefFilesInfo.stream()
                    .filter(file -> file.getUnavailabilityReason() == null)
                    .collect(Collectors.toList())
            );
        } else {
            tabView.getRegularContentPane().setFiles(briefFilesInfo);
        }
        tabView.hideProgressIndicator();
    }

    private void updateView() {
        List<String> missingSettings = getMissingSettings(config);
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
