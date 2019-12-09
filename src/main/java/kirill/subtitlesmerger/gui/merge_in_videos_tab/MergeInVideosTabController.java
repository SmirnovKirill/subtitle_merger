package kirill.subtitlesmerger.gui.merge_in_videos_tab;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import kirill.subtitlesmerger.gui.GuiLauncher;
import kirill.subtitlesmerger.gui.TabController;
import kirill.subtitlesmerger.gui.TabView;
import kirill.subtitlesmerger.logic.Constants;
import kirill.subtitlesmerger.logic.data.BriefFileInfo;
import kirill.subtitlesmerger.logic.data.Config;
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

import static kirill.subtitlesmerger.logic.data.BriefFileInfo.UnavailabilityReason.*;

@CommonsLog
public class MergeInVideosTabController implements TabController {
    private MergeInVideosTabView tabView;

    private Config config;

    private GuiLauncher guiLauncher;

    private List<BriefFileInfo> briefFilesInfo;

    public MergeInVideosTabController(MergeInVideosTabView tabView, Config config, GuiLauncher guiLauncher) {
        this.tabView = tabView;
        this.config = config;
        this.guiLauncher = guiLauncher;
    }

    @Override
    public void initialize() {
        tabView.setGoToSettingsLinkHandler(this::goToSettingsLinkClicked);
        tabView.setDirectoryChooseButtonHandler(this::directoryButtonClicked);
        tabView.setShowOnlyValidCheckBoxChangeListener(this::showOnlyValidCheckBoxChanged);

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

        briefFilesInfo = getBriefFilesInfo(directory.listFiles());
        if (CollectionUtils.isEmpty(briefFilesInfo)) {
            tabView.showDirectoryErrorMessage("directory is empty");
            return;
        }

        tabView.showTableWithFiles(briefFilesInfo);
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
        if (Boolean.TRUE.equals(newValue)) {
            tabView.showTableWithFiles(
                    briefFilesInfo.stream()
                    .filter(file -> file.getUnavailabilityReason() == null)
                    .collect(Collectors.toList())
            );
        } else {
            tabView.showTableWithFiles(briefFilesInfo);
        }
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
