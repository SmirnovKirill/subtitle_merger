package kirill.subtitlemerger.gui.application_specific.videos_tab.background;


import javafx.application.Platform;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.GuiUtils;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.util.entities.ActionResult;
import kirill.subtitlemerger.logic.core.SubtitleParser;
import kirill.subtitlemerger.logic.work_with_files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

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
        runnerManager.setCancellationPossible(true);

        mainLoop: for (TableFileInfo tableFileInfo : selectedTableFilesInfo) {
            FileInfo fileInfo = FileInfo.getById(tableFileInfo.getId(), filesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
                continue;
            }

            int failedToLoadForFile = 0;

            for (FfmpegSubtitleStream ffmpegStream : fileInfo.getFfmpegSubtitleStreams()) {
                if (runnerManager.isCancelled()) {
                    break mainLoop;
                }

                if (ffmpegStream.getUnavailabilityReason() != null || ffmpegStream.getSubtitles() != null) {
                    continue;
                }

                runnerManager.updateMessage(
                        VideoTabBackgroundUtils.getLoadSubtitlesProgressMessage(
                                streamToLoadCount,
                                processedCount,
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
                        setFileInfoErrorIfNecessary(failedToLoadForFile, tableFileInfo, tableWithFiles);

                        break mainLoop;
                    } else {
                        Platform.runLater(
                                () -> tableWithFiles.failedToLoadSubtitles(
                                        VideoTabBackgroundUtils.failedToLoadReasonFrom(e.getCode()),
                                        tableSubtitleOption
                                )
                        );
                        failedToLoadCount++;
                        failedToLoadForFile++;
                    }
                } catch (SubtitleParser.IncorrectFormatException e) {
                    Platform.runLater(
                            () -> tableWithFiles.failedToLoadSubtitles(
                                    VideoTabBackgroundUtils.FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT,
                                    tableSubtitleOption
                            )
                    );
                    failedToLoadCount++;
                    failedToLoadForFile++;
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
        runnerManager.updateMessage("calculating number of subtitles to load...");

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
