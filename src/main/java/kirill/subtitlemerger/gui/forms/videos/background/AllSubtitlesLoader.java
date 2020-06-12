package kirill.subtitlemerger.gui.forms.videos.background;


import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.MultiPartActionResult;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.*;


@CommonsLog
@AllArgsConstructor
public class AllSubtitlesLoader implements BackgroundRunner<MultiPartActionResult> {
    private List<TableVideo> tableVideos;

    private List<Video> videos;

    private Ffmpeg ffmpeg;

    @Override
    public MultiPartActionResult run(BackgroundManager backgroundManager) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();

        clearActionResults(tableVideos, backgroundManager);

        List<TableVideo> selectedVideos = getSelectedVideos(tableVideos, backgroundManager);
        int toLoadCount = getSubtitlesToLoadCount(selectedVideos, videos, backgroundManager);
        int processedCount = 0;
        int successfulCount = 0;
        int incorrectCount = 0;
        int failedCount = 0;

        backgroundManager.setCancelPossible(true);
        try {
            for (TableVideo tableVideo : selectedVideos) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                Video video = Video.getById(tableVideo.getId(), videos);
                int videoToLoadCount = video.getOptionsToLoad().size();
                int videoFailedCount = 0;
                int videoIncorrectCount = 0;
                if (videoToLoadCount == 0) {
                    backgroundManager.setCancelDescription(null);
                } else {
                    backgroundManager.setCancelDescription(getLoadingCancelDescription(video));
                }

                for (BuiltInSubtitleOption option : video.getOptionsToLoad()) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    TableSubtitleOption tableOption = tableVideo.getOption(option.getId());

                    String action = "Loading " + tableOption.getTitle() + " in " + video.getFile().getName() + "...";
                    backgroundManager.updateMessage(getProgressAction(processedCount, toLoadCount, action));

                    LoadSubtitlesResult loadResult = loadSubtitles(option, video, tableOption, ffmpeg);
                    if (loadResult == LoadSubtitlesResult.SUCCESS) {
                        successfulCount++;
                    } else if (loadResult == LoadSubtitlesResult.INCORRECT_FORMAT) {
                        incorrectCount++;
                        videoIncorrectCount++;
                    } else if (loadResult == LoadSubtitlesResult.FAILED) {
                        failedCount++;
                        videoFailedCount++;
                    } else {
                        log.error("unexpected load result: " + loadResult + ", most likely a bug");
                        throw new IllegalStateException();
                    }

                    if (videoFailedCount != 0 || videoIncorrectCount != 0) {
                        MultiPartActionResult actionResult = getLoadSubtitlesActionResult(
                                videoToLoadCount,
                                videoIncorrectCount,
                                videoFailedCount
                        );
                        Platform.runLater(() -> tableVideo.setActionResult(actionResult));
                    }

                    processedCount++;
                }
            }
        } catch (InterruptedException e) {
            /* Do nothing here, will just return a result based on the work done. */
        }

        return getLoadSubtitlesResult(toLoadCount, processedCount, successfulCount, incorrectCount, failedCount);
    }

    private static int getSubtitlesToLoadCount(
            List<TableVideo> tableVideos,
            List<Video> videos,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.updateMessage("Calculating the number of subtitles to load...");

        int result = 0;

        for (TableVideo tableVideo : tableVideos) {
            Video video = Video.getById(tableVideo.getId(), videos);
            result += video.getOptionsToLoad().size();
        }

        return result;
    }

    /**
     * This method differs from VideosBackgroundUtils::getLoadSubtitlesError because here an incorrect format isn't
     * considered to be an error. If subtitles have an incorrect format it doesn't mean that the loading has failed
     * whereas for other operations an incorrect format means that the operation can't be performed.
     */
    static MultiPartActionResult getLoadSubtitlesActionResult(
            int toLoadCount,
            int incorrectCount,
            int failedCount
    ) {
        String warning = null;
        String error = null;

        if (toLoadCount == 1) {
            if (incorrectCount != 0) {
                warning = "The subtitles have been loaded but have an incorrect format";
            } else if (failedCount != 0) {
                error = "Failed to load the subtitles";
            }
        } else {
            if (incorrectCount != 0) {
                String formatSuffix = Utils.getTextDependingOnCount(
                        incorrectCount,
                        "an incorrect format",
                        "incorrect formats"
                );

                warning = incorrectCount + "/" + toLoadCount + " subtitles have been loaded but have " + formatSuffix;
            }

            if (failedCount != 0) {
                if (StringUtils.isBlank(warning)) {
                    error = String.format("Failed to load %d/%d subtitles", failedCount, toLoadCount);
                } else {
                    error = String.format("failed to load %d/%d", failedCount, toLoadCount);
                }
            }
        }

        return new MultiPartActionResult(null, warning, error);
    }
}
