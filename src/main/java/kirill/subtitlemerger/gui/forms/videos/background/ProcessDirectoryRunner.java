package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableData;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableVideoInfo;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableWithVideos;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.file_validation.FileValidator;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommonsLog
@AllArgsConstructor
public class ProcessDirectoryRunner implements BackgroundRunner<ProcessDirectoryRunner.Result> {
    private String directoryPath;

    private GuiContext context;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        DirectoryInfo directoryInfo = getDirectoryInfo(directoryPath, backgroundManager);
        if (!StringUtils.isBlank(directoryInfo.getNotValidReason())) {
            return new Result(
                    directoryInfo.getNotValidReason(),
                    !directoryInfo.isCanRefresh(),
                    null,
                    null,
                    false,
                    null
            );
        }

        List<VideoInfo> filesInfo = VideoBackgroundUtils.getVideosInfo(
                directoryInfo.getDirectoryFiles(),
                context.getFfprobe(),
                backgroundManager
        );
        List<TableVideoInfo> allTableFilesInfo = VideoBackgroundUtils.tableVideosInfoFrom(
                filesInfo,
                false,
                false,
                context.getSettings(),
                backgroundManager
        );

        boolean hideUnavailable = shouldHideUnavailable(filesInfo, backgroundManager);

        List<TableVideoInfo> tableFilesToShowInfo = null;
        if (hideUnavailable) {
            tableFilesToShowInfo = VideoBackgroundUtils.getOnlyAvailableFilesInfo(allTableFilesInfo, backgroundManager);
        }

        tableFilesToShowInfo = VideoBackgroundUtils.getSortedVideosInfo(
                tableFilesToShowInfo != null ? tableFilesToShowInfo : allTableFilesInfo,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                backgroundManager
        );

        return new Result(
                null,
                false,
                filesInfo,
                allTableFilesInfo,
                hideUnavailable,
                VideoBackgroundUtils.getTableData(
                        TableWithVideos.Mode.DIRECTORY,
                        tableFilesToShowInfo,
                        context.getSettings().getSortBy(),
                        context.getSettings().getSortDirection(),
                        backgroundManager
                )
        );
    }

    private static DirectoryInfo getDirectoryInfo(String path, BackgroundManager backgroundManager) {
        String shortenedPath = Utils.getShortenedString(path, 20, 40);

        if (StringUtils.isBlank(path)) {
            return new DirectoryInfo("Directory is not selected!", false, null);
        }

        if (path.length() > FileValidator.PATH_LENGTH_LIMIT) {
            return new DirectoryInfo("Directory path is too long", false, null);
        }

        try {
            Path.of(path);
        } catch (InvalidPathException e) {
            return new DirectoryInfo("Directory path is invalid", false, null);
        }

        File directory = new File(path);
        if (directory.isFile()) {
            String notValidReason = shortenedPath + " is a file, not a directory";
            return new DirectoryInfo(notValidReason, true, null);
        }

        if (!directory.exists()) {
            String notValidReason = "Directory '" + shortenedPath + "' doesn't exist";
            return new DirectoryInfo(notValidReason, true, null);
        }

        List<File> directoryFiles = getDirectoryFiles(directory, backgroundManager);
        if (directoryFiles.size() > GuiConstants.TABLE_FILE_LIMIT) {
            String notValidReason = String.format("Directory has too many files (>%d)", GuiConstants.TABLE_FILE_LIMIT);
            return new DirectoryInfo(notValidReason, true, null);
        }

        return new DirectoryInfo(null, true, directoryFiles);
    }

    private static List<File> getDirectoryFiles(File directory, BackgroundManager backgroundManager) {
        backgroundManager.setCancellationPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting video list...");

        File[] directoryFiles = directory.listFiles();

        if (directoryFiles == null) {
            log.warn("failed to get directory files, directory " + directory.getAbsolutePath());
            return new ArrayList<>();
        } else {
            return Arrays.asList(directoryFiles);
        }
    }

    /*
     * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
     * not be checked because the user will see just an empty file list which isn't very user friendly.
     */
    private static boolean shouldHideUnavailable(List<VideoInfo> filesInfo, BackgroundManager backgroundManager) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Calculating whether to hide unavailable files by default...");

        return filesInfo.stream().anyMatch(fileInfo -> fileInfo.getNotValidReason() == null);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private String unavailabilityReason;

        private boolean disableRefresh;

        private List<VideoInfo> filesInfo;

        private List<TableVideoInfo> allTableFilesInfo;

        private boolean hideUnavailable;

        private TableData tableData;
    }

    @AllArgsConstructor
    @Getter
    private static class DirectoryInfo {
        private String notValidReason;

        private boolean canRefresh;

        private List<File> directoryFiles;
    }
}
