package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.utils.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiExternalSubtitleStream;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFfmpegSubtitleStream;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStream;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

@CommonsLog
public class LoadDirectoryFilesTask extends BackgroundTask<LoadDirectoryFilesTask.Result> {
    private File directory;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext guiContext;

    private Consumer<Result> onFinish;

    public LoadDirectoryFilesTask(
            File directory,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            GuiContext guiContext,
            Consumer<Result> onFinish
    ) {
        this.directory = directory;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.guiContext = guiContext;
        this.onFinish = onFinish;
    }

    @Override
    protected Result run() {
        List<File> files = getDirectoryFiles(directory, this);
        List<FileInfo> filesInfo = getFilesInfo(files, guiContext.getFfprobe(), this);
        List<GuiFileInfo> allGuiFilesInfo = convert(
                filesInfo,
                false,
                false,
                this,
                guiContext.getSettings()
        );

        boolean hideUnavailable = shouldHideUnavailable(filesInfo, this);
        List<GuiFileInfo> guiFilesToShowInfo = getFilesInfoToShow(
                allGuiFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                this
        );

        return new Result(filesInfo, allGuiFilesInfo, guiFilesToShowInfo, hideUnavailable);
    }

    private static List<File> getDirectoryFiles(File directory, LoadDirectoryFilesTask task) {
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

    static List<GuiFileInfo> getFilesInfoToShow(
            List<GuiFileInfo> allFilesInfo,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            BackgroundTask task
    ) {
        task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);

        List<GuiFileInfo> result = new ArrayList<>(allFilesInfo);
        if (hideUnavailable) {
            task.updateMessage("filtering unavailable...");
            result.removeIf(fileInfo -> !StringUtils.isBlank(fileInfo.getUnavailabilityReason()));
        }

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
        task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);

        List<GuiFileInfo> result = new ArrayList<>();

        for (FileInfo fileInfo : filesInfo) {
            task.updateMessage("creating object for " + fileInfo.getFile().getName() + "...");

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
        if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
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
                RegularContentController.getSubtitleCanBeHiddenCount(fileInfo, guiSettings),
                RegularContentController.getSubtitleCanBeHiddenCount(fileInfo, guiSettings) != 0,
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
                RegularContentController.isExtra(subtitleStream, guiSettings)
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

    @Override
    protected void onFinish(Result result) {
        onFinish.accept(result);
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
