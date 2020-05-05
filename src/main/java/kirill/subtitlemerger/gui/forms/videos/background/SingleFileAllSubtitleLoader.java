package kirill.subtitlemerger.gui.forms.videos.background;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;

import java.nio.charset.StandardCharsets;

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.FAILED_TO_LOAD_INCORRECT_FORMAT;
import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.failedToLoadReasonFrom;

@CommonsLog
@AllArgsConstructor
public class SingleFileAllSubtitleLoader implements BackgroundRunner<ActionResult> {
    private Video fileInfo;

    private TableVideo tableFileInfo;

    private Ffmpeg ffmpeg;

    @Override
    public ActionResult run(BackgroundManager backgroundManager) {
        int streamToLoadCount = getStreamToLoadCount(fileInfo, backgroundManager);
        int processedCount = 0;
        int loadedSuccessfullyCount = 0;
        int failedToLoadCount = 0;

        backgroundManager.setIndeterminateProgress();
        backgroundManager.setCancellationDescription("Please be patient, this may take a while depending on the size.");
        backgroundManager.setCancellationPossible(true);

        for (BuiltInSubtitleOption ffmpegStream : fileInfo.getBuiltInSubtitleOptions()) {
            if (ffmpegStream.getNotValidReason() != null || ffmpegStream.getSubtitles() != null) {
                continue;
            }

            backgroundManager.updateMessage(
                    VideosBackgroundUtils.getLoadSubtitlesProgressMessage(
                            processedCount,
                            streamToLoadCount,
                            ffmpegStream,
                            fileInfo.getFile()
                    )
            );

            TableSubtitleOption tableSubtitleOption = TableSubtitleOption.getById(
                    ffmpegStream.getId(),
                    tableFileInfo.getSubtitleOptions()
            );

            try {
                String subtitleText = ffmpeg.getSubtitleText(
                        ffmpegStream.getFfmpegIndex(),
                        ffmpegStream.getFormat(),
                        fileInfo.getFile()
                );
                SubtitlesAndInput subtitlesAndInput = SubtitlesAndInput.from(
                        subtitleText.getBytes(),
                        StandardCharsets.UTF_8
                );

                if (subtitlesAndInput.isCorrectFormat()) {
                    ffmpegStream.setSubtitlesAndInput(subtitlesAndInput);
                    Platform.runLater(() -> tableSubtitleOption.loadedSuccessfully(subtitlesAndInput.getSize()));
                    loadedSuccessfullyCount++;
                } else {
                    Platform.runLater(() -> tableSubtitleOption.failedToLoad(FAILED_TO_LOAD_INCORRECT_FORMAT));
                    failedToLoadCount++;
                }
            } catch (FfmpegException e) {
                log.warn("failed to get subtitle text: " + e.getCode() + ", console output " + e.getConsoleOutput());
                Platform.runLater(() -> tableSubtitleOption.failedToLoad(failedToLoadReasonFrom(e.getCode())));
                failedToLoadCount++;
            } catch (InterruptedException e) {
                break;
            }

            processedCount++;
        }

        return VideosBackgroundUtils.getSubtitleLoadingActionResult(
                streamToLoadCount,
                processedCount,
                loadedSuccessfullyCount,
                failedToLoadCount
        );
    }

    private static int getStreamToLoadCount(Video fileInfo, BackgroundManager backgroundManager) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Calculating number of subtitles to load...");

        int result = 0;

        for (BuiltInSubtitleOption stream : fileInfo.getBuiltInSubtitleOptions()) {
            if (stream.getNotValidReason() != null || stream.getSubtitles() != null) {
                continue;
            }

            result++;
        }

        return result;
    }
}

