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
        List<FileInfo> filesInfo = getFilesInfo();
        filesInfo = getSortedFilesInfo(filesInfo, sortBy, sortDirection);

        boolean hideUnavailable = shouldHideUnavailable(filesInfo);

        List<GuiFileInfo> guiFilesInfo = getGuiFilesInfo(filesInfo, hideUnavailable);

        return new FilesInfo(filesInfo, guiFilesInfo, hideUnavailable);
    }

    private List<FileInfo> getFilesInfo() {
        if (mode == Mode.SEPARATE_FILES) {
            return getFilesInfo(files);
        } else if (mode == Mode.DIRECTORY) {
            updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
            updateMessage("getting file list...");

            File[] directoryFilesArray = directory.listFiles();
            List<File> directoryFiles;
            if (directoryFilesArray == null) {
                log.warn("failed to get directory files, directory " + directory.getAbsolutePath());
                directoryFiles = new ArrayList<>();
            } else {
                directoryFiles = Arrays.asList(directoryFilesArray);
            }

            return getFilesInfo(directoryFiles);
        } else if (mode == Mode.ONLY_UPDATE_GUI) {
            if (filesInfo == null) {
                return new ArrayList<>();
            }

            return filesInfo;
        } else if (mode == Mode.ADD_SEPARATE_FILES) {
            List<FileInfo> result = new ArrayList<>(filesInfo);

            List<FileInfo> chosenFilesInfo = getFilesInfo(files);

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

    private List<FileInfo> getFilesInfo(List<File> files) {
        List<FileInfo> result = new ArrayList<>();

        updateProgress(0, files.size());

        int i = 0;
        for (File file : files) {
            updateMessage("processing " + file.getName() + "...");

            if (file.isDirectory() || !file.exists()) {
                updateProgress(i + 1, files.size());
                i++;
                continue;
            }

            result.add(
                    FileInfoGetter.getFileInfoWithoutSubtitles(
                            file,
                            LogicConstants.ALLOWED_VIDEO_EXTENSIONS,
                            LogicConstants.ALLOWED_VIDEO_MIME_TYPES,
                            ffprobe
                    )
            );

            updateProgress(i + 1, files.size());
            i++;
        }

        return result;
    }

    private List<FileInfo> getSortedFilesInfo(
            List<FileInfo> filesInfo,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection
    ) {
        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        updateMessage("sorting file list...");

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

    private boolean shouldHideUnavailable(List<FileInfo> filesInfo) {
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

    private List<GuiFileInfo> getGuiFilesInfo(List<FileInfo> filesInfo, boolean hideUnavailable) {
        List<GuiFileInfo> result = new ArrayList<>();

        updateProgress(0, filesInfo.size());

        int i = 0;
        for (FileInfo fileInfo : filesInfo) {
            updateMessage("preparing to display " + fileInfo.getFile().getName() + "...");

            if (hideUnavailable && fileInfo.getUnavailabilityReason() != null) {
                updateProgress(i + 1, filesInfo.size());
                i++;
                continue;
            }

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

            updateProgress(i + 1, filesInfo.size());
            i++;
        }

        return result;
    }

    private static String convert(FileInfo.UnavailabilityReason unavailabilityReason) {
        if (unavailabilityReason == null) {
            return "";
        }

        return unavailabilityReason.toString(); //todo
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