package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStreamInfo;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.work_with_files.FileInfoGetter;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStreamInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public abstract class BackgroundTask<T> extends Task<T> {
    BackgroundTask() {
        setFailedCancelledCallbacks();
    }

    private void setFailedCancelledCallbacks() {
        setOnFailed(BackgroundTask::taskFailed);
        setOnCancelled(BackgroundTask::taskCancelled);
    }

    private static void taskFailed(Event e) {
        log.error("task has failed, shouldn't happen");
        throw new IllegalStateException();
    }

    private static void taskCancelled(Event e) {
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
                comparator = Comparator.comparing(GuiFileInfo::getPath);
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
            task.updateMessage("processing " + file.getName() + "...");

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
            BackgroundTask<?> task
    ) {
        List<GuiFileInfo> result = new ArrayList<>();

        task.updateProgress(0, filesInfo.size());

        int i = 0;
        for (FileInfo fileInfo : filesInfo) {
            task.updateMessage("creating object for " + fileInfo.getFile().getName() + "...");

            result.add(from(fileInfo, showFullFileName, selectByDefault));

            task.updateProgress(i + 1, filesInfo.size());
            i++;
        }

        return result;
    }

    private static GuiFileInfo from(FileInfo fileInfo, boolean showFullFileName, boolean selected) {
        String path = showFullFileName ? fileInfo.getFile().getAbsolutePath() : fileInfo.getFile().getName();

        List<GuiSubtitleStreamInfo> subtitleStreamsInfo = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
            subtitleStreamsInfo = fileInfo.getSubtitleStreamsInfo().stream()
                    .map(BackgroundTask::from)
                    .collect(Collectors.toList());
        }

        return new GuiFileInfo(
                path,
                selected,
                fileInfo.getLastModified(),
                LocalDateTime.now(),
                fileInfo.getSize(),
                guiTextFrom(fileInfo.getUnavailabilityReason()),
                "",
                subtitleStreamsInfo
        );
    }

    private static GuiSubtitleStreamInfo from(SubtitleStreamInfo subtitleStreamInfo) {
        return new GuiSubtitleStreamInfo(
                guiTextFrom(subtitleStreamInfo.getUnavailabilityReason()),
                subtitleStreamInfo.getLanguage() != null ? subtitleStreamInfo.getLanguage().toString() : "unknown",
                subtitleStreamInfo.getTitle()
        );
    }

    private static String guiTextFrom(SubtitleStreamInfo.UnavailabilityReason unavailabilityReason) {
        if (unavailabilityReason == null) {
            return "";
        }

        if (unavailabilityReason == SubtitleStreamInfo.UnavailabilityReason.NOT_ALLOWED_CODEC) {
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
}
