package kirill.subtitlemerger.gui.forms.videos.background;


import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;

import java.util.List;

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.*;


@CommonsLog
@AllArgsConstructor
public class AllSubtitleLoader implements BackgroundRunner<ActionResult> {
    private List<TableVideo> tableVideos;

    private List<Video> videos;

    private Ffmpeg ffmpeg;

    @Override
    public ActionResult run(BackgroundManager backgroundManager) {
        clearActionResults(tableVideos, backgroundManager);

        List<TableVideo> selectedVideos = getSelectedVideos(tableVideos, backgroundManager);
        int toLoadCount = getSubtitlesToLoadCount(selectedVideos, videos, backgroundManager);
        int processedCount = 0;
        int successfulCount = 0;
        int failedCount = 0;
        int incorrectCount = 0;

        backgroundManager.setCancelPossible(true);
        backgroundManager.setCancelDescription("Please be patient, this may take a while depending on the video.");
        backgroundManager.setIndeterminateProgress();
        try {
            for (TableVideo tableVideo : selectedVideos) {
                Video video = Video.getById(tableVideo.getId(), videos);

                int videoToLoadCount = video.getOptionsToLoad().size();
                int videoFailedCount = 0;
                int videoIncorrectCount = 0;
                for (BuiltInSubtitleOption option : video.getOptionsToLoad()) {
                    TableSubtitleOption tableOption = tableVideo.getOption(option.getId());

                    String action = "Loading " + tableOption.getTitle() + " in " + video.getFile().getName() + "...";
                    backgroundManager.updateMessage(getProgressAction(processedCount, toLoadCount, action));

                    LoadSubtitlesResult loadResult = loadSubtitles(option, video, tableOption, ffmpeg);
                    if (loadResult == LoadSubtitlesResult.SUCCESS) {
                        successfulCount++;
                    } else if (loadResult == LoadSubtitlesResult.FAILED) {
                        failedCount++;
                        videoFailedCount++;
                    } else if (loadResult == LoadSubtitlesResult.INCORRECT_FORMAT) {
                        incorrectCount++;
                        videoIncorrectCount++;
                    } else {
                        log.error("unexpected load result: " + loadResult + ", most likely a bug");
                        throw new IllegalStateException();
                    }

                    if (videoFailedCount != 0 || videoIncorrectCount != 0) {
                        String error = getLoadSubtitlesError(videoToLoadCount, videoFailedCount, videoIncorrectCount);
                        Platform.runLater(() -> tableVideo.setOnlyError(error));
                    }

                    processedCount++;
                }
            }
        } catch (InterruptedException e) {
            /* Do nothing here, will just return the result based on the work done. */
        }

        return getLoadSubtitlesResult(toLoadCount, processedCount, successfulCount, failedCount, incorrectCount);
    }

    private static int getSubtitlesToLoadCount(
            List<TableVideo> tableVideos,
            List<Video> videos,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Calculating the number of subtitles to load...");

        int result = 0;

        for (TableVideo tableVideo : tableVideos) {
            Video video = Video.getById(tableVideo.getId(), videos);
            result += video.getOptionsToLoad().size();
        }

        return result;
    }
}
