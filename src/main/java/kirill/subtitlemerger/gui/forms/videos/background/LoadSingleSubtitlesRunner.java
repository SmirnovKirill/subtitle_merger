package kirill.subtitlemerger.gui.forms.videos.background;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
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
public class LoadSingleSubtitlesRunner implements BackgroundRunner<ActionResult> {
    private BuiltInSubtitleOption ffmpegStream;

    private Video fileInfo;

    private TableSubtitleOption tableSubtitleOption;

    private Ffmpeg ffmpeg;

    @Override
    public ActionResult run(BackgroundManager backgroundManager) {
        backgroundManager.setIndeterminateProgress();

        backgroundManager.updateMessage(
                VideosBackgroundUtils.getLoadSubtitlesProgressMessage(
                        0,
                        1,
                        ffmpegStream,
                        fileInfo.getFile()
                )
        );

        backgroundManager.setCancellationDescription("Please be patient, this may take a while depending on the size.");
        backgroundManager.setCancellationPossible(true);

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
                return ActionResult.onlySuccess("The subtitles have been loaded successfully");
            } else {
                Platform.runLater(() -> tableSubtitleOption.failedToLoad(FAILED_TO_LOAD_INCORRECT_FORMAT));
                return ActionResult.onlyError("Failed to load subtitles");
            }
        } catch (FfmpegException e) {
            log.warn("failed to get subtitle text: " + e.getCode() + ", console output " + e.getConsoleOutput());
            Platform.runLater(() -> tableSubtitleOption.failedToLoad(failedToLoadReasonFrom(e.getCode())));
            return ActionResult.onlyError("Failed to load subtitles");
        } catch (InterruptedException e) {
            return ActionResult.onlyWarn("Task has been cancelled");
        }
    }
}
