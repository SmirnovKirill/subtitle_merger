package kirill.subtitlesmerger.gui.tabs.videos.regular_content;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlesmerger.gui.GuiSettings;
import kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlesmerger.logic.LogicConstants;
import kirill.subtitlesmerger.logic.work_with_files.FileInfoGetter;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.*;

@CommonsLog
public class GetFilesInfoTask extends Task<GetFilesInfoTask.FilesInfo> {
    private Mode mode;

    private List<File> files;

    private File directory;

    private List<FileInfo> filesInfo;

    private boolean showFullFileName;

    private boolean hideUnavailable;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private Ffprobe ffprobe;

    GetFilesInfoTask(
            List<File> files,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            Ffprobe ffprobe
    ) {
        this.mode = Mode.SEPARATE_FILES;
        this.files = files;
        this.showFullFileName = true;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.ffprobe = ffprobe;
        setFailedCancelledCallbacks();
    }

    GetFilesInfoTask(
            File directory,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            Ffprobe ffprobe
    ) {
        this.mode = Mode.DIRECTORY;
        this.directory = directory;
        this.showFullFileName = false;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.ffprobe = ffprobe;
        setFailedCancelledCallbacks();
    }

    GetFilesInfoTask(
            List<FileInfo> filesInfo,
            boolean showFullFileName,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection
    ) {
        this.mode = Mode.ONLY_UPDATE_GUI;
        this.filesInfo = filesInfo;
        this.showFullFileName = showFullFileName;
        this.hideUnavailable = hideUnavailable;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        setFailedCancelledCallbacks();
    }

    GetFilesInfoTask(
            List<FileInfo> filesInfo,
            List<File> files,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            Ffprobe ffprobe
    ) {
        this.mode = Mode.ADD_SEPARATE_FILES;
        this.filesInfo = filesInfo;
        this.files = files;
        this.showFullFileName = true;
        this.hideUnavailable = hideUnavailable;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.ffprobe = ffprobe;
        setFailedCancelledCallbacks();
    }

    private void setFailedCancelledCallbacks() {
        setOnFailed(GetFilesInfoTask::taskFailed);
        setOnCancelled(GetFilesInfoTask::taskCancelled);
    }

    private static void taskFailed(Event e) {
        log.error("task has failed, shouldn't happen");
        throw new IllegalStateException();
    }

    private static void taskCancelled(Event e) {
        log.error("task has been cancelled, shouldn't happen");
        throw new IllegalStateException();
    }

    @Override
    protected FilesInfo call() {
        List<FileInfo> filesInfo = getFilesInfo(mode, this.filesInfo, files, directory, ffprobe, this);
        filesInfo = getSortedFilesInfo(filesInfo, sortBy, sortDirection, this);

        boolean hideUnavailable = shouldHideUnavailable(mode, this.hideUnavailable, filesInfo);

        List<GuiFileInfo> guiFilesInfo = getGuiFilesInfo(filesInfo, showFullFileName, hideUnavailable, this);

        return new FilesInfo(filesInfo, guiFilesInfo, hideUnavailable);
    }

    private static List<FileInfo> getFilesInfo(
            Mode mode,
            List<FileInfo> filesInfo,
            List<File> files,
            File directory,
            Ffprobe ffprobe,
            GetFilesInfoTask task
    ) {
        if (mode == Mode.SEPARATE_FILES) {
            return getFilesInfo(files, ffprobe, task);
        } else if (mode == Mode.DIRECTORY) {
            return getFilesInfo(getDirectoryFiles(directory, task), ffprobe, task);
        } else if (mode == Mode.ONLY_UPDATE_GUI) {
            return filesInfo;
        } else if (mode == Mode.ADD_SEPARATE_FILES) {
            List<FileInfo> result = new ArrayList<>(filesInfo);

            List<FileInfo> chosenFilesInfo = getFilesInfo(files, ffprobe, task);

            for (FileInfo chosenFileInfo : chosenFilesInfo) {
                boolean alreadyAdded = filesInfo.stream()
                        .anyMatch(fileInfo -> Objects.equals(fileInfo.getFile(), chosenFileInfo.getFile()));
                if (!alreadyAdded) {
                    result.add(chosenFileInfo);
                }
            }

            return result;
        } else {
            throw new IllegalStateException();
        }
    }

    private static List<FileInfo> getFilesInfo(List<File> files, Ffprobe ffprobe, GetFilesInfoTask task) {
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

    private static List<File> getDirectoryFiles(File directory, GetFilesInfoTask task) {
        task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        task.updateMessage("getting file list...");

        File[] directoryFiles = directory.listFiles();

        if (directoryFiles == null) {
            log.warn("failed to get directory files, directory " + directory.getAbsolutePath());
            return new ArrayList<>();
        } else {
            return Arrays.asList(directoryFiles);
        }
    }

    private static List<FileInfo> getSortedFilesInfo(
            List<FileInfo> filesInfo,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            GetFilesInfoTask task
    ) {
        task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        task.updateMessage("sorting file list...");

        List<FileInfo> result = new ArrayList<>(filesInfo);

        Comparator<FileInfo> comparator;
        switch (sortBy) {
            case NAME:
                comparator = Comparator.comparing(fileInfo -> fileInfo.getFile().getAbsolutePath());
                break;
            case MODIFICATION_TIME:
                comparator = Comparator.comparing(FileInfo::getLastModified);
                break;
            case SIZE:
                comparator = Comparator.comparing(FileInfo::getSize);
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

    private static boolean shouldHideUnavailable(Mode mode, boolean hideUnavailable, List<FileInfo> filesInfo) {
        if (mode == Mode.ADD_SEPARATE_FILES || mode == Mode.ONLY_UPDATE_GUI) {
            return hideUnavailable;
        } else if (mode == Mode.DIRECTORY || mode == Mode.SEPARATE_FILES) {
            /*
             * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
             * not be checked because the user will see just an empty file list which isn't very user friendly.
             */
            return filesInfo.stream().anyMatch(fileInfo -> fileInfo.getUnavailabilityReason() == null);
        } else {
            throw new IllegalStateException();
        }
    }

    private static List<GuiFileInfo> getGuiFilesInfo(
            List<FileInfo> filesInfo,
            boolean showFullFileName,
            boolean hideUnavailable,
            GetFilesInfoTask task
    ) {
        List<GuiFileInfo> result = new ArrayList<>();

        task.updateProgress(0, filesInfo.size());

        int i = 0;
        for (FileInfo fileInfo : filesInfo) {
            task.updateMessage("preparing to display " + fileInfo.getFile().getName() + "...");

            if (!hideUnavailable || fileInfo.getUnavailabilityReason() == null) {
                String path = showFullFileName ? fileInfo.getFile().getAbsolutePath() : fileInfo.getFile().getName();

                result.add(
                        new GuiFileInfo(
                                path,
                                fileInfo.getLastModified(),
                                LocalDateTime.now(),
                                fileInfo.getSize(),
                                convert(fileInfo.getUnavailabilityReason()),
                                ""
                        )
                );
            }

            task.updateProgress(i + 1, filesInfo.size());
            i++;
        }

        return result;
    }

    private static String convert(FileInfo.UnavailabilityReason unavailabilityReason) {
        if (unavailabilityReason == null) {
            return "";
        }

        switch (unavailabilityReason) {
            case NO_EXTENSION:
                return "file has no extension";
            case NOT_ALLOWED_EXTENSION:
                return "file has not allowed extension";
            case FAILED_TO_GET_MIME_TYPE:
                return "failed to get mime type";
            case NOT_ALLOWED_MIME_TYPE:
                return "file has mime type that is not allowed";
            case FAILED_TO_GET_FFPROBE_INFO:
                return "failed to get video info with ffprobe";
            case NOT_ALLOWED_CONTAINER:
                return "video has format that is not allowed";
            default:
                throw new IllegalStateException();
        }
    }

    private enum Mode {
        SEPARATE_FILES,
        DIRECTORY,
        ONLY_UPDATE_GUI,
        ADD_SEPARATE_FILES
    }

    @AllArgsConstructor
    @Getter
    static class FilesInfo {
        private List<FileInfo> filesInfo;

        private List<GuiFileInfo> guiFilesInfo;

        private boolean hideUnavailable;
    }
}