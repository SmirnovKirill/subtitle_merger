package kirill.subtitlemerger.gui.application_specific.videos_tab.loaders_and_handlers;

import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.ContentPaneController;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiExternalSubtitleStream;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFfmpegSubtitleStream;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.work_with_files.FileInfoGetter;
import kirill.subtitlemerger.logic.work_with_files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class LoadDirectoryFilesTask implements BackgroundRunner<LoadDirectoryFilesTask.Result> {
    private File directory;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext guiContext;

    public LoadDirectoryFilesTask(
            File directory,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            GuiContext guiContext
    ) {
        this.directory = directory;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.guiContext = guiContext;
    }

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        List<File> files = getDirectoryFiles(directory, runnerManager);
        List<FileInfo> filesInfo = getFilesInfo(files, guiContext.getFfprobe(), runnerManager);
        List<GuiFileInfo> allGuiFilesInfo = convert(
                filesInfo,
                false,
                false,
                runnerManager,
                guiContext.getSettings()
        );

        boolean hideUnavailable = shouldHideUnavailable(filesInfo, runnerManager);
        List<GuiFileInfo> guiFilesToShowInfo = getFilesInfoToShow(
                allGuiFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                runnerManager
        );

        return new Result(filesInfo, allGuiFilesInfo, guiFilesToShowInfo, hideUnavailable);
    }

    private static List<File> getDirectoryFiles(File directory, BackgroundRunnerManager runnerManager) {
        runnerManager.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        runnerManager.updateMessage("getting file list...");

        File[] directoryFiles = directory.listFiles();

        if (directoryFiles == null) {
            log.warn("failed to get directory files, directory " + directory.getAbsolutePath());
            return new ArrayList<>();
        } else {
            return Arrays.asList(directoryFiles);
        }
    }

    static List<GuiFileInfo> getFilesInfoToShow(
            List<GuiFileInfo> allFilesInfo,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);

        List<GuiFileInfo> result = new ArrayList<>(allFilesInfo);
        if (hideUnavailable) {
            runnerManager.updateMessage("filtering unavailable...");
            result.removeIf(fileInfo -> !StringUtils.isBlank(fileInfo.getUnavailabilityReason()));
        }

        runnerManager.updateMessage("sorting file list...");

        Comparator<GuiFileInfo> comparator;
        switch (sortBy) {
            case NAME:
                comparator = Comparator.comparing(GuiFileInfo::getPathToDisplay);
                break;
            case MODIFICATION_TIME:
                comparator = Comparator.comparing(GuiFileInfo::getLastModified);
                break;
            case SIZE:
                comparator = Comparator.comparing(GuiFileInfo::getSize);
                break;
            default:
                throw new IllegalStateException();
        }

        if (sortDirection == GuiSettings.SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        result.sort(comparator);

        return result;
    }

    static List<FileInfo> getFilesInfo(List<File> files, Ffprobe ffprobe, BackgroundRunnerManager runnerManager) {
        List<FileInfo> result = new ArrayList<>();

        runnerManager.updateProgress(0, files.size());

        int i = 0;
        for (File file : files) {
            runnerManager.updateMessage("getting file info " + file.getName() + "...");

            if (file.isFile() && file.exists()) {
                result.add(
                        FileInfoGetter.getFileInfoWithoutSubtitles(
                                file,
                                LogicConstants.ALLOWED_VIDEO_EXTENSIONS,
                                LogicConstants.ALLOWED_VIDEO_MIME_TYPES,
                                ffprobe
                        )
                );
            }

            runnerManager.updateProgress(i + 1, files.size());
            i++;
        }

        return result;
    }

    /*
     * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
     * not be checked because the user will see just an empty file list which isn't very user friendly.
     */
    static boolean shouldHideUnavailable(List<FileInfo> filesInfo, BackgroundRunnerManager runnerManager) {
        runnerManager.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        runnerManager.updateMessage("calculating whether to hide unavailable files by default...");

        return filesInfo.stream().anyMatch(fileInfo -> fileInfo.getUnavailabilityReason() == null);
    }

    static List<GuiFileInfo> convert(
            List<FileInfo> filesInfo,
            boolean showFullFileName,
            boolean selectByDefault,
            BackgroundRunnerManager runnerManager,
            GuiSettings guiSettings
    ) {
        runnerManager.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);

        List<GuiFileInfo> result = new ArrayList<>();

        for (FileInfo fileInfo : filesInfo) {
            runnerManager.updateMessage("creating object for " + fileInfo.getFile().getName() + "...");

            result.add(
                    from(
                            fileInfo,
                            showFullFileName,
                            selectByDefault && fileInfo.getUnavailabilityReason() == null,
                            guiSettings
                    )
            );
        }

        return result;
    }

    private static GuiFileInfo from(
            FileInfo fileInfo,
            boolean showFullFileName,
            boolean selected,
            GuiSettings guiSettings
    ) {
        String pathToDisplay = showFullFileName ? fileInfo.getFile().getAbsolutePath() : fileInfo.getFile().getName();

        List<GuiSubtitleStream> subtitleStreams = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
            subtitleStreams = fileInfo.getFfmpegSubtitleStreams().stream()
                    .map(subtitleStream -> from(subtitleStream, guiSettings))
                    .collect(Collectors.toList());
        }

        for (int i = 0; i < 2; i++) {
            subtitleStreams.add(new GuiExternalSubtitleStream(null, i));
        }

        GuiFileInfo result = new GuiFileInfo(
                pathToDisplay,
                fileInfo.getFile().getAbsolutePath(),
                selected,
                fileInfo.getLastModified(),
                fileInfo.getSize(),
                guiTextFrom(fileInfo.getUnavailabilityReason()),
                fileInfo.haveSubtitlesToLoad(),
                ContentPaneController.getSubtitleCanBeHiddenCount(fileInfo, guiSettings),
                ContentPaneController.getSubtitleCanBeHiddenCount(fileInfo, guiSettings) != 0,
                subtitleStreams
        );

        return result;
    }

    private static GuiFfmpegSubtitleStream from(FfmpegSubtitleStream subtitleStream, GuiSettings guiSettings) {
        return new GuiFfmpegSubtitleStream(
                subtitleStream.getId(),
                subtitleStream.getSubtitles() != null ? subtitleStream.getSubtitles().getSize() : null,
                false,
                false,
                guiTextFrom(subtitleStream.getUnavailabilityReason()),
                null,
                subtitleStream.getLanguage() != null ? subtitleStream.getLanguage().toString() : "unknown",
                subtitleStream.getTitle(),
                ContentPaneController.isExtra(subtitleStream, guiSettings)
        );
    }

    private static String guiTextFrom(FfmpegSubtitleStream.UnavailabilityReason unavailabilityReason) {
        if (unavailabilityReason == null) {
            return "";
        }

        if (unavailabilityReason == FfmpegSubtitleStream.UnavailabilityReason.NOT_ALLOWED_CODEC) {
            return "subtitle has a not allowed type";
        }

        throw new IllegalStateException();
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<FileInfo> filesInfo;

        private List<GuiFileInfo> allGuiFilesInfo;

        private List<GuiFileInfo> guiFilesToShowInfo;

        private boolean hideUnavailable;
    }
}
