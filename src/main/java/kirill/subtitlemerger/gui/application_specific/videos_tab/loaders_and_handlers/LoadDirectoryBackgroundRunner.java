package kirill.subtitlemerger.gui.application_specific.videos_tab.loaders_and_handlers;

import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.utils.GuiUtils;
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
public class LoadDirectoryBackgroundRunner implements BackgroundRunner<LoadDirectoryBackgroundRunner.Result> {
    private File directory;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext guiContext;

    public LoadDirectoryBackgroundRunner(
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
        List<TableFileInfo> allTableFilesInfo = tableFilesInfoFrom(
                filesInfo,
                false,
                false,
                runnerManager,
                guiContext.getSettings()
        );

        boolean hideUnavailable = shouldHideUnavailable(filesInfo, runnerManager);
        List<TableFileInfo> tableFilesToShowInfo = getFilesToShowInfo(
                allTableFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                runnerManager
        );

        return new Result(filesInfo, allTableFilesInfo, tableFilesToShowInfo, hideUnavailable);
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

    private static List<FileInfo> getFilesInfo(List<File> files, Ffprobe ffprobe, BackgroundRunnerManager runnerManager) {
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

    private static List<TableFileInfo> tableFilesInfoFrom(
            List<FileInfo> filesInfo,
            boolean showFullPath,
            boolean selectByDefault,
            BackgroundRunnerManager runnerManager,
            GuiSettings guiSettings
    ) {
        runnerManager.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);

        List<TableFileInfo> result = new ArrayList<>();

        for (FileInfo fileInfo : filesInfo) {
            runnerManager.updateMessage("creating object for " + fileInfo.getFile().getName() + "...");

            result.add(
                    tableFileInfoFrom(
                            fileInfo,
                            showFullPath,
                            selectByDefault && fileInfo.getUnavailabilityReason() == null,
                            guiSettings
                    )
            );
        }

        return result;
    }

    private static TableFileInfo tableFileInfoFrom(
            FileInfo fileInfo,
            boolean showFullPath,
            boolean selected,
            GuiSettings guiSettings
    ) {
        String pathToDisplay = showFullPath ? fileInfo.getFile().getAbsolutePath() : fileInfo.getFile().getName();

        List<TableSubtitleOption> subtitleOptions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
            subtitleOptions = fileInfo.getFfmpegSubtitleStreams().stream()
                    .map(subtitleStream -> tableSubtitleOptionFrom(subtitleStream, guiSettings))
                    .collect(Collectors.toList());
        }

        for (int i = 0; i < 2; i++) {
            subtitleOptions.add(
                    new TableSubtitleOption(
                            null,
                            null,
                            false,
                            true,
                            true,
                            TableSubtitleOption.UNKNOWN_SIZE,
                            null,
                            null,
                            false,
                            false
                    )
            );
        }

        return new TableFileInfo(
                fileInfo.getFile().getAbsolutePath(),
                selected,
                pathToDisplay,
                fileInfo.getSize(),
                fileInfo.getLastModified(),
                tableUnavailabilityReasonFrom(fileInfo.getUnavailabilityReason()),
                subtitleOptions,
                true,
                null
        );
    }

    private static TableSubtitleOption tableSubtitleOptionFrom(
            FfmpegSubtitleStream subtitleStream,
            GuiSettings guiSettings
    ) {
        return new TableSubtitleOption(
                subtitleStream.getId(),
                getFfmpegStreamTitle(subtitleStream),
                isHideable(subtitleStream, guiSettings),
                false,
                false,
                subtitleStream.getSubtitles() != null
                        ? subtitleStream.getSubtitles().getSize()
                        : TableSubtitleOption.UNKNOWN_SIZE,
                null,
                tableUnavailabilityReasonFrom(subtitleStream.getUnavailabilityReason()),
                false,
                false
        );
    }

    private static String getFfmpegStreamTitle(FfmpegSubtitleStream stream) {
        String result = GuiUtils.languageToString(stream.getLanguage()).toUpperCase();

        if (!StringUtils.isBlank(stream.getTitle())) {
            result += " (" + stream.getTitle() + ")";
        }

        return result;
    }

    private static boolean isHideable(FfmpegSubtitleStream subtitleStream, GuiSettings guiSettings) {
        return subtitleStream.getLanguage() != guiSettings.getUpperLanguage()
                && subtitleStream.getLanguage() != guiSettings.getLowerLanguage();
    }

    private static String tableUnavailabilityReasonFrom(FfmpegSubtitleStream.UnavailabilityReason reason) {
        if (reason == null) {
            return null;
        }

        if (reason == FfmpegSubtitleStream.UnavailabilityReason.NOT_ALLOWED_CODEC) {
            return "subtitle has a not allowed type";
        }

        throw new IllegalStateException();
    }

    private static String tableUnavailabilityReasonFrom(FileInfo.UnavailabilityReason reason) {
        if (reason == null) {
            return null;
        }

        switch (reason) {
            case NO_EXTENSION:
                return "file has no extension";
            case NOT_ALLOWED_EXTENSION:
                return "file has a not allowed extension";
            case FAILED_TO_GET_MIME_TYPE:
                return "failed to get the mime type";
            case NOT_ALLOWED_MIME_TYPE:
                return "file has a mime type that is not allowed";
            case FAILED_TO_GET_FFPROBE_INFO:
                return "failed to get video info with the ffprobe";
            case NOT_ALLOWED_CONTAINER:
                return "video has a format that is not allowed";
            default:
                throw new IllegalStateException();
        }
    }

    /*
     * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
     * not be checked because the user will see just an empty file list which isn't very user friendly.
     */
    private static boolean shouldHideUnavailable(List<FileInfo> filesInfo, BackgroundRunnerManager runnerManager) {
        runnerManager.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        runnerManager.updateMessage("calculating whether to hide unavailable files by default...");

        return filesInfo.stream().anyMatch(fileInfo -> fileInfo.getUnavailabilityReason() == null);
    }

    private static List<TableFileInfo> getFilesToShowInfo(
            List<TableFileInfo> allFilesInfo,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);

        List<TableFileInfo> result = new ArrayList<>(allFilesInfo);
        if (hideUnavailable) {
            runnerManager.updateMessage("filtering unavailable...");
            result.removeIf(fileInfo -> !StringUtils.isBlank(fileInfo.getUnavailabilityReason()));
        }

        runnerManager.updateMessage("sorting file list...");

        Comparator<TableFileInfo> comparator;
        switch (sortBy) {
            case NAME:
                comparator = Comparator.comparing(TableFileInfo::getFilePath);
                break;
            case MODIFICATION_TIME:
                comparator = Comparator.comparing(TableFileInfo::getLastModified);
                break;
            case SIZE:
                comparator = Comparator.comparing(TableFileInfo::getSize);
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

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<FileInfo> filesInfo;

        private List<TableFileInfo> allTableFilesInfo;

        private List<TableFileInfo> tableFilesToShowInfo;

        private boolean hideUnavailable;
    }
}
