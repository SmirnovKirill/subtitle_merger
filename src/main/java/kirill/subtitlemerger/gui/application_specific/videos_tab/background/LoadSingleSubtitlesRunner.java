package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.util.entities.ActionResult;
import kirill.subtitlemerger.logic.core.SubRipParser;
import kirill.subtitlemerger.logic.core.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.work_with_files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LoadSingleSubtitlesRunner implements BackgroundRunner<ActionResult> {
    private FfmpegSubtitleStream ffmpegStream;

    private FileInfo fileInfo;

    private TableSubtitleOption tableSubtitleOption;

    private TableFileInfo tableFileInfo;

    private TableWithFiles tableWithFiles;

    private Ffmpeg ffmpeg;

    @Override
    public ActionResult run(BackgroundRunnerManager runnerManager) {
        runnerManager.setIndeterminateProgress();

        runnerManager.updateMessage(
                VideoTabBackgroundUtils.getLoadSubtitlesProgressMessage(
                        0,
                        1,
                        ffmpegStream,
                        fileInfo.getFile()
                )
        );

        runnerManager.setCancellationDescription("Please be patient, this may take a while depending on the size.");
        runnerManager.setCancellationPossible(true);

        try {
            String subtitleText = ffmpeg.getSubtitleText(ffmpegStream.getFfmpegIndex(), fileInfo.getFile());
            ffmpegStream.setSubtitles(SubRipParser.from(subtitleText, ffmpegStream.getLanguage()));

            Platform.runLater(
                    () -> tableWithFiles.subtitlesLoadedSuccessfully(
                            ffmpegStream.getSubtitles().getTextSize(),
                            tableSubtitleOption,
                            tableFileInfo
                    )
            );

            return ActionResult.onlySuccess("Subtitles have been loaded successfully");
        } catch (FfmpegException e) {
            if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                return ActionResult.onlyWarn("Task has been cancelled");
            } else {
                Platform.runLater(
                        () -> tableWithFiles.failedToLoadSubtitles(
                                VideoTabBackgroundUtils.failedToLoadReasonFrom(e.getCode()),
                                tableSubtitleOption
                        )
                );

                return ActionResult.onlyError("Failed to load subtitles");
            }
        } catch (SubtitleFormatException e) {
            Platform.runLater(
                    () -> tableWithFiles.failedToLoadSubtitles(
                            VideoTabBackgroundUtils.FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT,
                            tableSubtitleOption
                    )
            );

            return ActionResult.onlyError("Failed to load subtitles");
        }
    }
}
