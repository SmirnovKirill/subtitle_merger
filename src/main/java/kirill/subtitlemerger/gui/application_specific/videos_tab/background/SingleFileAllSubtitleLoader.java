package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.util.entities.ActionResult;
import kirill.subtitlemerger.logic.core.SubtitleParser;
import kirill.subtitlemerger.logic.work_with_files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SingleFileAllSubtitleLoader implements BackgroundRunner<ActionResult> {
    private FileInfo fileInfo;

    private TableFileInfo tableFileInfo;

    private TableWithFiles tableWithFiles;

    private Ffmpeg ffmpeg;

    @Override
    public ActionResult run(BackgroundRunnerManager runnerManager) {
        int streamToLoadCount = getStreamToLoadCount(fileInfo, runnerManager);
        int processedCount = 0;
        int loadedSuccessfullyCount = 0;
        int failedToLoadCount = 0;

        runnerManager.setIndeterminateProgress();
        runnerManager.setCancellationPossible(true);

        for (FfmpegSubtitleStream ffmpegStream : fileInfo.getFfmpegSubtitleStreams()) {
            if (runnerManager.isCancelled()) {
                break;
            }

            if (ffmpegStream.getUnavailabilityReason() != null || ffmpegStream.getSubtitles() != null) {
                continue;
            }

            runnerManager.updateMessage(
                    VideoTabBackgroundUtils.getLoadSubtitlesProgressMessage(
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
                String subtitleText = ffmpeg.getSubtitleText(ffmpegStream.getFfmpegIndex(), fileInfo.getFile());
                ffmpegStream.setSubtitles(SubtitleParser.fromSubRipText(subtitleText, ffmpegStream.getLanguage()));

                Platform.runLater(
                        () -> tableWithFiles.subtitlesLoadedSuccessfully(
                                ffmpegStream.getSubtitles().getSize(),
                                tableSubtitleOption,
                                tableFileInfo
                        )
                );

                loadedSuccessfullyCount++;
            } catch (FfmpegException e) {
                if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                    break;
                } else {
                    Platform.runLater(
                            () -> tableWithFiles.failedToLoadSubtitles(
                                    VideoTabBackgroundUtils.failedToLoadReasonFrom(e.getCode()),
                                    tableSubtitleOption
                            )
                    );
                    failedToLoadCount++;
                }
            } catch (SubtitleParser.IncorrectFormatException e) {
                Platform.runLater(
                        () -> tableWithFiles.failedToLoadSubtitles(
                                VideoTabBackgroundUtils.FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT,
                                tableSubtitleOption
                        )
                );
                failedToLoadCount++;
            }

            processedCount++;
        }

        return VideoTabBackgroundUtils.generateSubtitleLoadingActionResult(
                streamToLoadCount,
                processedCount,
                loadedSuccessfullyCount,
                failedToLoadCount
        );
    }

    private static int getStreamToLoadCount(FileInfo fileInfo, BackgroundRunnerManager runnerManager) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("Calculating number of subtitles to load...");

        int result = 0;

        for (FfmpegSubtitleStream stream : fileInfo.getFfmpegSubtitleStreams()) {
            if (stream.getUnavailabilityReason() != null || stream.getSubtitles() != null) {
                continue;
            }

            result++;
        }

        return result;
    }
}

