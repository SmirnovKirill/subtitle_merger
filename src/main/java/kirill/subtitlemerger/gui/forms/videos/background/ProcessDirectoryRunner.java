package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.forms.videos.table.TableData;
import kirill.subtitlemerger.gui.forms.videos.table.TableMode;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.forms.videos.table.TableWithVideos;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.file_validation.FileValidator;
import kirill.subtitlemerger.logic.videos.entities.Video;
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

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.*;

@CommonsLog
@AllArgsConstructor
public class ProcessDirectoryRunner implements BackgroundRunner<ProcessDirectoryRunner.Result> {
    private String directoryPath;

    private TableWithVideos table;

    private Ffprobe ffprobe;

    private Settings settings;

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

        List<Video> allVideos = getVideos(directoryInfo.getDirectoryFiles(), ffprobe, backgroundManager);
        List<TableVideo> allTableVideos = tableVideosFrom(
                allVideos,
                false,
                false,
                table,
                settings,
                backgroundManager
        );
        allTableVideos = getSortedVideos(allTableVideos, settings.getSort(), backgroundManager);
        boolean hideUnavailable = shouldHideUnavailable(allVideos, backgroundManager);

        return new Result(
                null,
                false,
                allVideos,
                allTableVideos,
                hideUnavailable,
                getTableData(
                        allTableVideos,
                        hideUnavailable,
                        TableMode.WHOLE_DIRECTORY,
                        settings.getSort(),
                        backgroundManager
                )
        );
    }

    private static DirectoryInfo getDirectoryInfo(String path, BackgroundManager backgroundManager) {
        String shortenedPath = Utils.getShortenedString(path, 20, 40);

        if (StringUtils.isBlank(path)) {
            return new DirectoryInfo("A directory is not selected!", false, null);
        }

        if (path.length() > FileValidator.PATH_LENGTH_LIMIT) {
            return new DirectoryInfo("The directory path is too long", false, null);
        }

        try {
            Path.of(path);
        } catch (InvalidPathException e) {
            return new DirectoryInfo("The directory path is invalid", false, null);
        }

        File directory = new File(path);
        if (directory.isFile()) {
            String notValidReason = "'" + shortenedPath + "' is a file, not a directory";
            return new DirectoryInfo(notValidReason, true, null);
        }

        if (!directory.exists()) {
            String notValidReason = "The directory '" + shortenedPath + "' doesn't exist";
            return new DirectoryInfo(notValidReason, true, null);
        }

        List<File> directoryFiles = getDirectoryFiles(directory, backgroundManager);
        if (directoryFiles.size() > GuiConstants.VIDEO_TABLE_LIMIT) {
            String notValidReason = "The directory has too many videos (>" + GuiConstants.VIDEO_TABLE_LIMIT + ")";
            return new DirectoryInfo(notValidReason, true, null);
        }

        return new DirectoryInfo(null, true, directoryFiles);
    }

    private static List<File> getDirectoryFiles(File directory, BackgroundManager backgroundManager) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting the video list...");

        File[] directoryFiles = directory.listFiles();

        if (directoryFiles == null) {
            log.warn("failed to get directory files, directory " + directory.getAbsolutePath());
            return new ArrayList<>();
        } else {
            return Arrays.asList(directoryFiles);
        }
    }

    /*
     * We should set the "hide unavailable" checkbox by default if there is at least one available video. Otherwise it
     * should not be checked because the user will see just an empty video list which isn't very user friendly.
     */
    private static boolean shouldHideUnavailable(List<Video> videos, BackgroundManager backgroundManager) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Calculating whether to hide unavailable videos by default...");

        return videos.stream().anyMatch(video -> video.getNotValidReason() == null);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private String notValidReason;

        private boolean disableRefresh;

        private List<Video> allVideos;

        private List<TableVideo> allTableVideos;

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
