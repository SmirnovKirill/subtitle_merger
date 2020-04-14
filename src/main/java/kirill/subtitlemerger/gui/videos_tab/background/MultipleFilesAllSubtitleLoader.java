package kirill.subtitlemerger.gui.videos_tab.background;


import javafx.application.Platform;
import kirill.subtitlemerger.gui.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.videos_tab.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.core.SubRipParser;
import kirill.subtitlemerger.logic.core.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@CommonsLog
@AllArgsConstructor
public class MultipleFilesAllSubtitleLoader implements BackgroundRunner<ActionResult> {
    private List<TableFileInfo> displayedTableFilesInfo;

    private List<FileInfo> filesInfo;

    private TableWithFiles tableWithFiles;

    private Ffmpeg ffmpeg;

    @Override
    public ActionResult run(BackgroundRunnerManager runnerManager) {
        VideoTabBackgroundUtils.clearActionResults(displayedTableFilesInfo, tableWithFiles, runnerManager);

        List<TableFileInfo> selectedTableFilesInfo = VideoTabBackgroundUtils.getSelectedFilesInfo(
                displayedTableFilesInfo,
                runnerManager
        );

        int streamToLoadCount = getStreamToLoadCount(selectedTableFilesInfo, filesInfo, runnerManager);
        int processedCount = 0;
        int loadedSuccessfullyCount = 0;
        int failedToLoadCount = 0;

        runnerManager.setIndeterminateProgress();
        runnerManager.setCancellationDescription("Please be patient, this may take a while depending on the size.");
        runnerManager.setCancellationPossible(true);

        mainLoop: for (TableFileInfo tableFileInfo : selectedTableFilesInfo) {
            FileInfo fileInfo = FileInfo.getById(tableFileInfo.getId(), filesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
                continue;
            }

            int failedToLoadForFile = 0;

            for (FfmpegSubtitleStream ffmpegStream : fileInfo.getFfmpegSubtitleStreams()) {
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
                    setFileInfoErrorIfNecessary(failedToLoadForFile, tableFileInfo, tableWithFiles);
                    break mainLoop;
                }

                processedCount++;
            }

            setFileInfoErrorIfNecessary(failedToLoadForFile, tableFileInfo, tableWithFiles);
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
            List<FileInfo> allFilesInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("Calculating number of subtitles to load...");

        int result = 0;

        for (TableFileInfo tableFileInfo : selectedTableFilesInfo) {
            FileInfo fileInfo = FileInfo.getById(tableFileInfo.getId(), allFilesInfo);
            if (!CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
                for (FfmpegSubtitleStream stream : fileInfo.getFfmpegSubtitleStreams()) {
                    if (stream.getUnavailabilityReason() != null || stream.getSubtitles() != null) {
                        continue;
                    }

                    result++;
                }
            }
        }

        return result;
    }

    private static void setFileInfoErrorIfNecessary(
            int failedToLoadForFile,
            TableFileInfo fileInfo,
            TableWithFiles tableWithFiles
    ) {
        if (failedToLoadForFile == 0) {
            return;
        }

        String message = GuiUtils.getTextDependingOnTheCount(
                failedToLoadForFile,
                "Failed to load subtitles",
                "Failed to load %d subtitles"
        );

        Platform.runLater(() -> tableWithFiles.setActionResult(ActionResult.onlyError(message), fileInfo));
    }
}
