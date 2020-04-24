package kirill.subtitlemerger.gui.forms.videos.background;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.core.SubRipParser;
import kirill.subtitlemerger.logic.core.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.files.entities.FileInfo;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
@AllArgsConstructor
public class LoadSingleSubtitlesRunner implements BackgroundRunner<ActionResult> {
    private FfmpegSubtitleStream ffmpegStream;

    private FileInfo fileInfo;

    private TableSubtitleOption tableSubtitleOption;

    private TableFileInfo tableFileInfo;

    private TableWithFiles tableWithFiles;

    private Ffmpeg ffmpeg;

    @Override
    public ActionResult run(BackgroundManager backgroundManager) {
        backgroundManager.setIndeterminateProgress();

        backgroundManager.updateMessage(
                VideoTabBackgroundUtils.getLoadSubtitlesProgressMessage(
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
            ffmpegStream.setSubtitlesAndSize(SubRipParser.from(subtitleText), subtitleText.getBytes().length);

            Platform.runLater(
                    () -> tableWithFiles.subtitlesLoadedSuccessfully(
                            ffmpegStream.getSize(),
                            tableSubtitleOption,
                            tableFileInfo
                    )
            );

            return ActionResult.onlySuccess("Subtitles have been loaded successfully");
        } catch (FfmpegException e) {
            log.warn("failed to get subtitle text: " + e.getCode() + ", console output " + e.getConsoleOutput());
            Platform.runLater(
                    () -> tableWithFiles.failedToLoadSubtitles(
                            VideoTabBackgroundUtils.failedToLoadReasonFrom(e.getCode()),
                            tableSubtitleOption
                    )
            );

            return ActionResult.onlyError("Failed to load subtitles");
        } catch (SubtitleFormatException e) {
            Platform.runLater(
                    () -> tableWithFiles.failedToLoadSubtitles(
                            VideoTabBackgroundUtils.FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT,
                            tableSubtitleOption
                    )
            );

            return ActionResult.onlyError("Failed to load subtitles");
        } catch (InterruptedException e) {
            return ActionResult.onlyWarn("Task has been cancelled");
        }
    }
}
