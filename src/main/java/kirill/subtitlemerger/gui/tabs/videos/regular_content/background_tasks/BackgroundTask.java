package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.FilePanes;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiExternalSubtitleFile;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStream;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.work_with_files.FileInfoGetter;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@CommonsLog
public abstract class BackgroundTask<T> extends Task<T> {
    BackgroundTask() {
        setOnFailed(this::taskFailed);
        setOnCancelled(this::taskCancelled);
    }

    private void taskFailed(Event e) {
        log.error("task has failed, shouldn't happen");
        throw new IllegalStateException();
    }

    private void taskCancelled(Event e) {
        log.error("task has been cancelled, shouldn't happen");
        throw new IllegalStateException();
    }

    static List<GuiFileInfo> getFilesInfoToShow(
            List<GuiFileInfo> allFilesInfo,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            BackgroundTask<?> task
    ) {
        List<GuiFileInfo> result = new ArrayList<>(allFilesInfo);
        if (hideUnavailable) {
            task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
            task.updateMessage("filtering unavailable...");
            result.removeIf(fileInfo -> !StringUtils.isBlank(fileInfo.getUnavailabilityReason()));
        }

        task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        task.updateMessage("sorting file list...");

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

    static List<FileInfo> getFilesInfo(List<File> files, Ffprobe ffprobe, BackgroundTask<?> task) {
        List<FileInfo> result = new ArrayList<>();

        task.updateProgress(0, files.size());

        int i = 0;
        for (File file : files) {
            task.updateMessage("getting file info " + file.getName() + "...");

            if (!file.isDirectory() && file.exists()) {
                result.add(
                        FileInfoGetter.getFileInfoWithoutSubtitles(
                                file,
                                LogicConstants.ALLOWED_VIDEO_EXTENSIONS,
                                LogicConstants.ALLOWED_VIDEO_MIME_TYPES,
                                ffprobe
                        )
                );
            }

            task.updateProgress(i + 1, files.size());
            i++;
        }

        return result;
    }

    /*
     * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
     * not be checked because the user will see just an empty file list which isn't very user friendly.
     */
    static boolean shouldHideUnavailable(List<FileInfo> filesInfo, BackgroundTask<?> task) {
        task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        task.updateMessage("calculating whether to hide unavailable files by default...");

        return filesInfo.stream().anyMatch(fileInfo -> fileInfo.getUnavailabilityReason() == null);
    }

    static List<GuiFileInfo> convert(
            List<FileInfo> filesInfo,
            boolean showFullFileName,
            boolean selectByDefault,
            BackgroundTask<?> task,
            GuiSettings guiSettings
    ) {
        List<GuiFileInfo> result = new ArrayList<>();

        task.updateProgress(0, filesInfo.size());

        int i = 0;
        for (FileInfo fileInfo : filesInfo) {
            task.updateMessage("creating object for " + fileInfo.getFile().getName() + "...");

            result.add(
                    from(
                            fileInfo,
                            showFullFileName,
                            selectByDefault,
                            guiSettings
                    )
            );

            task.updateProgress(i + 1, filesInfo.size());
            i++;
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
        if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
            subtitleStreams = fileInfo.getSubtitleStreams().stream()
                    .map(subtitleStream -> from(subtitleStream, guiSettings))
                    .collect(Collectors.toList());
        }

        GuiFileInfo result = new GuiFileInfo(
                pathToDisplay,
                fileInfo.getFile().getAbsolutePath(),
                false,
                selected,
                fileInfo.getLastModified(),
                fileInfo.getSize(),
                guiTextFrom(fileInfo.getUnavailabilityReason()),
                "",
                RegularContentController.haveSubtitlesToLoad(fileInfo),
                RegularContentController.getSubtitleCanBeHiddenCount(fileInfo, guiSettings),
                RegularContentController.getSubtitleCanBeHiddenCount(fileInfo, guiSettings) != 0,
                subtitleStreams
        );

        //todo refactor ugly
        for (GuiSubtitleStream stream : result.getSubtitleStreams()) {
            stream.selectedAsUpperProperty().addListener((observableValue, oldValue, newValue) -> {
                if (!Boolean.TRUE.equals(newValue)) {
                    return;
                }

                for (GuiSubtitleStream currentStream : result.getSubtitleStreams()) {
                    if (currentStream.getFfmpegIndex() == stream.getFfmpegIndex()) {
                        currentStream.setSelectedAsLower(false);
                    } else {
                        currentStream.setSelectedAsUpper(false);
                    }
                }
                for (GuiExternalSubtitleFile externalSubtitleFile : result.getExternalSubtitleFiles()) {
                    externalSubtitleFile.setSelectedAsUpper(false);
                }
            });

            stream.selectedAsLowerProperty().addListener((observableValue, oldValue, newValue) -> {
                if (!Boolean.TRUE.equals(newValue)) {
                    return;
                }

                for (GuiSubtitleStream currentStream : result.getSubtitleStreams()) {
                    if (currentStream.getFfmpegIndex() == stream.getFfmpegIndex()) {
                        currentStream.setSelectedAsUpper(false);
                    } else {
                        currentStream.setSelectedAsLower(false);
                    }
                }
                for (GuiExternalSubtitleFile externalSubtitleFile : result.getExternalSubtitleFiles()) {
                    externalSubtitleFile.setSelectedAsLower(false);
                }
            });
        }

        for (GuiExternalSubtitleFile externalSubtitleFile : result.getExternalSubtitleFiles()) {
            externalSubtitleFile.selectedAsUpperProperty().addListener((observableValue, oldValue, newValue) -> {
                if (!Boolean.TRUE.equals(newValue)) {
                    return;
                }

                for (GuiSubtitleStream currentStream : result.getSubtitleStreams()) {
                    currentStream.setSelectedAsUpper(false);
                }

                for (GuiExternalSubtitleFile currentSubtitleFile : result.getExternalSubtitleFiles()) {
                    if (Objects.equals(currentSubtitleFile.getFileName(), externalSubtitleFile.getFileName())) {
                        currentSubtitleFile.setSelectedAsLower(false);
                    } else {
                        currentSubtitleFile.setSelectedAsUpper(false);
                    }
                }
            });

            externalSubtitleFile.selectedAsLowerProperty().addListener((observableValue, oldValue, newValue) -> {
                if (!Boolean.TRUE.equals(newValue)) {
                    return;
                }

                for (GuiSubtitleStream currentStream : result.getSubtitleStreams()) {
                    currentStream.setSelectedAsLower(false);
                }

                for (GuiExternalSubtitleFile currentSubtitleFile : result.getExternalSubtitleFiles()) {
                    if (Objects.equals(currentSubtitleFile.getFileName(), externalSubtitleFile.getFileName())) {
                        currentSubtitleFile.setSelectedAsUpper(false);
                    } else {
                        currentSubtitleFile.setSelectedAsLower(false);
                    }
                }
            });
        }

        return result;
    }

    private static GuiSubtitleStream from(SubtitleStream subtitleStream, GuiSettings guiSettings) {
        return new GuiSubtitleStream(
                subtitleStream.getFfmpegIndex(),
                guiTextFrom(subtitleStream.getUnavailabilityReason()),
                null,
                subtitleStream.getLanguage() != null ? subtitleStream.getLanguage().toString() : "unknown",
                subtitleStream.getTitle(),
                RegularContentController.isExtra(subtitleStream, guiSettings),
                subtitleStream.getSubtitleSize(),
                false,
                false
        );
    }

    private static String guiTextFrom(SubtitleStream.UnavailabilityReason unavailabilityReason) {
        if (unavailabilityReason == null) {
            return "";
        }

        if (unavailabilityReason == SubtitleStream.UnavailabilityReason.NOT_ALLOWED_CODEC) {
            return "subtitle has a not allowed type";
        }

        throw new IllegalStateException();
    }

    private static String guiTextFrom(FileInfo.UnavailabilityReason unavailabilityReason) {
        if (unavailabilityReason == null) {
            return "";
        }

        switch (unavailabilityReason) {
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

    public static void clearState(List<GuiFileInfo> filesInfo, BackgroundTask<?> task) {
        task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        task.updateMessage("clearing state...");

        for (GuiFileInfo fileInfo : filesInfo) {
            fileInfo.setErrorBorder(false);
        }
    }

    public static Map<String, FilePanes> generateFilesPanes(
            List<GuiFileInfo> filesInfo,
            LongProperty selected,
            BooleanProperty allSelected,
            IntegerProperty allAvailableCount,
            FilePanes.AllFileSubtitleSizesLoader allFileSubtitleSizesLoader,
            FilePanes.SingleFileSubtitleSizeLoader singleFileSubtitleSizeLoader,
            FilePanes.AddExternalSubtitleFileHandler addExternalSubtitleFileHandler,
            FilePanes.RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler,
            BackgroundTask<?> task
    ) {
        Map<String, FilePanes> result = new HashMap<>();

        task.updateProgress(0, filesInfo.size());

        int i = 0;
        for (GuiFileInfo fileInfo : filesInfo) {
            task.updateMessage("creating gui objects for " + fileInfo.getPathToDisplay() + "...");

            result.put(
                    fileInfo.getFullPath(),
                    new FilePanes(
                            fileInfo,
                            selected,
                            allSelected,
                            allAvailableCount,
                            allFileSubtitleSizesLoader,
                            singleFileSubtitleSizeLoader,
                            addExternalSubtitleFileHandler,
                            removeExternalSubtitleFileHandler
                    )
            );

            task.updateProgress(i + 1, filesInfo.size());
            i++;
        }

        return result;
    }
}
