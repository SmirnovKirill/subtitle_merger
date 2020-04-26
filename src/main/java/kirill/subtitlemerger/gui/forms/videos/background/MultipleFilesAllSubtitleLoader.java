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
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@CommonsLog
@AllArgsConstructor
public class MultipleFilesAllSubtitleLoader implements BackgroundRunner<ActionResult> {
    private List<TableFileInfo> displayedTableFilesInfo;

    private List<VideoInfo> filesInfo;

    private TableWithFiles tableWithFiles;

    private Ffmpeg ffmpeg;

    @Override
    public ActionResult run(BackgroundManager backgroundManager) {
        VideoTabBackgroundUtils.clearActionResults(displayedTableFilesInfo, tableWithFiles, backgroundManager);

        List<TableFileInfo> selectedTableFilesInfo = VideoTabBackgroundUtils.getSelectedFilesInfo(
                displayedTableFilesInfo,
                backgroundManager
        );

        int streamToLoadCount = getStreamToLoadCount(selectedTableFilesInfo, filesInfo, backgroundManager);
        int processedCount = 0;
        int loadedSuccessfullyCount = 0;
        int failedToLoadCount = 0;

        backgroundManager.setIndeterminateProgress();
        backgroundManager.setCancellationDescription("Please be patient, this may take a while depending on the size.");
        backgroundManager.setCancellationPossible(true);

        mainLoop: for (TableFileInfo tableFileInfo : selectedTableFilesInfo) {
            VideoInfo fileInfo = VideoInfo.getById(tableFileInfo.getId(), filesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getBuiltInSubtitleOptions())) {
                continue;
            }

            int failedToLoadForFile = 0;

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
                    failedToLoadForFile++;
                } catch (SubtitleFormatException e) {
                    Platform.runLater(
                            () -> tableWithFiles.failedToLoadSubtitles(
                                    VideoTabBackgroundUtils.FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT,
                                    tableSubtitleOption
                            )
                    );
                    failedToLoadCount++;
                    failedToLoadForFile++;
                } catch (InterruptedException e) {
                    setFileInfoError(failedToLoadForFile, tableFileInfo, tableWithFiles);
                    break mainLoop;
                }

                processedCount++;
            }

            setFileInfoError(failedToLoadForFile, tableFileInfo, tableWithFiles);
        }

        return VideoTabBackgroundUtils.generateSubtitleLoadingActionResult(
                streamToLoadCount,
                processedCount,
                loadedSuccessfullyCount,
                failedToLoadCount
        );
    }

    private static int getStreamToLoadCount(
            List<TableFileInfo> selectedTableFilesInfo,
            List<VideoInfo> allFilesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Calculating number of subtitles to load...");

        int result = 0;

        for (TableFileInfo tableFileInfo : selectedTableFilesInfo) {
            VideoInfo fileInfo = VideoInfo.getById(tableFileInfo.getId(), allFilesInfo);
            if (!CollectionUtils.isEmpty(fileInfo.getBuiltInSubtitleOptions())) {
                for (BuiltInSubtitleOption stream : fileInfo.getBuiltInSubtitleOptions()) {
                    if (stream.getNotValidReason() != null || stream.getSubtitles() != null) {
                        continue;
                    }

                    result++;
                }
            }
        }

        return result;
    }

    private static void setFileInfoError(
            int failedToLoadForFile,
            TableFileInfo fileInfo,
            TableWithFiles tableWithFiles
    ) {
        if (failedToLoadForFile == 0) {
            return;
        }

        String message = Utils.getTextDependingOnCount(
                failedToLoadForFile,
                "Failed to load subtitles",
                "Failed to load %d subtitles"
        );

        Platform.runLater(() -> tableWithFiles.setActionResult(ActionResult.onlyError(message), fileInfo));
    }
}
