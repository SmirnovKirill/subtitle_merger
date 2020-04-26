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
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
@AllArgsConstructor
public class SingleFileAllSubtitleLoader implements BackgroundRunner<ActionResult> {
    private VideoInfo fileInfo;

    private TableFileInfo tableFileInfo;

    private TableWithFiles tableWithFiles;

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

                loadedSuccessfullyCount++;
            } catch (FfmpegException e) {
                log.warn("failed to get subtitle text: " + e.getCode() + ", console output " + e.getConsoleOutput());
                Platform.runLater(
                        () -> tableWithFiles.failedToLoadSubtitles(
                                VideoTabBackgroundUtils.failedToLoadReasonFrom(e.getCode()),
                                tableSubtitleOption
                        )
                );
                failedToLoadCount++;
            } catch (SubtitleFormatException e) {
                Platform.runLater(
                        () -> tableWithFiles.failedToLoadSubtitles(
                                VideoTabBackgroundUtils.FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT,
                                tableSubtitleOption
                        )
                );
                failedToLoadCount++;
            } catch (InterruptedException e) {
                break;
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

    private static int getStreamToLoadCount(VideoInfo fileInfo, BackgroundManager backgroundManager) {
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

