package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.settings.MergeMode;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.clearActionResults;
import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.getSelectedVideos;

@CommonsLog
@AllArgsConstructor
public class MergeCheckRunner implements BackgroundRunner<MergeCheckRunner.Result> {
    private List<TableVideo> tableVideos;

    private List<Video> videos;

    private Settings settings;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        clearActionResults(tableVideos, backgroundManager);

        List<TableVideo> selectedTableVideos = getSelectedVideos(tableVideos, backgroundManager);

        String videosWithoutSelectionError = getVideosWithoutSelectionError(selectedTableVideos, backgroundManager);
        if (!StringUtils.isBlank(videosWithoutSelectionError)) {
            return new Result(
                    videosWithoutSelectionError,
                    null,
                    null,
                    null,
                    null
            );
        }

        FreeSpaceInfo freeSpaceInfo = getFreeSpaceInfo(selectedTableVideos, videos, settings, backgroundManager);

        return new Result(
                null,
                selectedTableVideos,
                freeSpaceInfo != null ? getFreeSpaceMessage(freeSpaceInfo) : null,
                freeSpaceInfo != null ? freeSpaceInfo.getLargestFreeSpaceDirectory() : null,
                getFilesToOverwrite(selectedTableVideos, videos, settings, backgroundManager)
        );
    }

    @Nullable
    private static String getVideosWithoutSelectionError(List<TableVideo> videos, BackgroundManager backgroundManager) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Checking whether all the videos have subtitles to merge...");

        int videosWithoutSelectionCount = (int) videos.stream()
                .filter(video -> video.getUpperOption() == null || video.getLowerOption() == null)
                .count();
        if (videosWithoutSelectionCount == 0) {
            return null;
        } else {
            return Utils.getTextDependingOnCount(
                    videos.size(),
                    "Merge for the video is unavailable because you have to select upper and lower "
                            + "subtitles",
                    "Merge is unavailable because you have to select upper and lower subtitles for "
                            + "all the selected videos (%d left)"
            );
        }
    }

    private static List<File> getFilesToOverwrite(
            List<TableVideo> tableVideos,
            List<Video> videos,
            Settings settings,
            BackgroundManager backgroundManager
    ) {
        if (settings.getMergeMode() != MergeMode.SEPARATE_SUBTITLE_FILES) {
            return null;
        }

        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting the files to overwrite list...");

        List<File> result = new ArrayList<>();
        for (TableVideo tableVideo : tableVideos) {
            Video video = Video.getById(tableVideo.getId(), videos);
            SubtitleOption upperOption = video.getOption(tableVideo.getUpperOption().getId());
            SubtitleOption lowerOption = video.getOption(tableVideo.getLowerOption().getId());

            File subtitleFile = new File(Utils.getMergedSubtitleFilePath(video, upperOption, lowerOption));
            if (subtitleFile.exists()) {
                result.add(subtitleFile);
            }
        }

        return result;
    }

    @Nullable
    private static FreeSpaceInfo getFreeSpaceInfo(
            List<TableVideo> tableVideos,
            List<Video> videos,
            Settings settings,
            BackgroundManager backgroundManager
    ) {
        if (settings.getMergeMode() != MergeMode.ORIGINAL_VIDEOS) {
            return null;
        }

        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Calculating required temporary space...");

        Long requiredSpace = null;
        Long freeSpace = null;
        File largestFreeSpaceDirectory = null;

        for (TableVideo tableVideo : tableVideos) {
            Video video = Video.getById(tableVideo.getId(), videos);

            long currentRequiredSpace = video.getFile().length();
            long currentFreeSpace = video.getFile().getFreeSpace();

            if (requiredSpace == null || requiredSpace < currentRequiredSpace) {
                requiredSpace = currentRequiredSpace;
            }

            if (freeSpace == null || freeSpace < currentFreeSpace) {
                freeSpace = currentFreeSpace;
                largestFreeSpaceDirectory = video.getFile().getParentFile();
            }
        }

        if (requiredSpace == null || largestFreeSpaceDirectory == null) {
            log.error("free space data is null, that means no videos have been selected, most likely a bug");
            throw new IllegalStateException();
        }

        return new FreeSpaceInfo(requiredSpace, freeSpace, largestFreeSpaceDirectory);
    }

    @Nullable
    private static String getFreeSpaceMessage(FreeSpaceInfo freeSpaceInfo) {
        if (freeSpaceInfo.getRequiredSpace() <= freeSpaceInfo.getFreeSpace()) {
            return null;
        }

        return "Merging requires approximately "
                + Utils.getSizeTextual(freeSpaceInfo.getRequiredSpace(), false) + " of free disk space during "
                + "the process but only " + Utils.getSizeTextual(freeSpaceInfo.getFreeSpace(), false) + " is "
                + "available, proceed anyway?";
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private String error;

        private List<TableVideo> selectedTableVideos;

        private String freeSpaceMessage;

        private File largestFreeSpaceDirectory;

        private List<File> filesToOverwrite;
    }

    @AllArgsConstructor
    @Getter
    private static class FreeSpaceInfo {
        private long requiredSpace;

        private long freeSpace;

        private File largestFreeSpaceDirectory;
    }
}
