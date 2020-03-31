package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.GuiSettings;
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
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class AutoSelectSubtitlesRunner implements BackgroundRunner<ActionResult> {
    private static final Comparator<FfmpegSubtitleStream> STREAM_COMPARATOR = Comparator.comparing(
            (FfmpegSubtitleStream stream) -> stream.getSubtitles().getSize()
    ).reversed();

    private List<TableFileInfo> displayedTableFilesInfo;

    private List<FileInfo> filesInfo;

    private TableWithFiles tableWithFiles;

    private Ffmpeg ffmpeg;

    private GuiSettings settings;

    @Override
    public ActionResult run(BackgroundRunnerManager runnerManager) {
        VideoTabBackgroundUtils.clearActionResults(displayedTableFilesInfo, tableWithFiles, runnerManager);

        List<TableFileInfo> selectedTableFilesInfo = VideoTabBackgroundUtils.getSelectedFilesInfo(
                displayedTableFilesInfo,
                runnerManager
        );

        int allFileCount = selectedTableFilesInfo.size();
        int processedCount = 0;
        int finishedSuccessfullyCount = 0;
        int notPossibleCount = 0;
        int failedCount = 0;

        runnerManager.setIndeterminateProgress();
        runnerManager.setCancellationPossible(true);

        for (TableFileInfo tableFileInfo : selectedTableFilesInfo) {
            if (runnerManager.isCancelled()) {
                break;
            }

            runnerManager.updateMessage(
                    VideoTabBackgroundUtils.getProcessFileProgressMessage(processedCount, allFileCount, tableFileInfo)
            );

            FileInfo fileInfo = FileInfo.getById(tableFileInfo.getId(), filesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
                notPossibleCount++;
                processedCount++;
                continue;
            }

            List<FfmpegSubtitleStream> matchingUpperSubtitles = getMatchingUpperSubtitles(fileInfo, settings);
            List<FfmpegSubtitleStream> matchingLowerSubtitles = getMatchingLowerSubtitles(fileInfo, settings);
            if (CollectionUtils.isEmpty(matchingUpperSubtitles) || CollectionUtils.isEmpty(matchingLowerSubtitles)) {
                notPossibleCount++;
                processedCount++;
                continue;
            }

            try {
                boolean loadedSuccessfully = loadStreamsIfNecessary(
                        tableFileInfo,
                        fileInfo,
                        CollectionUtils.union(matchingUpperSubtitles, matchingLowerSubtitles),
                        processedCount,
                        allFileCount,
                        runnerManager
                );
                if (!loadedSuccessfully) {
                    failedCount++;
                    processedCount++;
                    continue;
                }

                if (matchingUpperSubtitles.size() > 1) {
                    matchingUpperSubtitles.sort(STREAM_COMPARATOR);
                }
                if (matchingLowerSubtitles.size() > 1) {
                    matchingLowerSubtitles.sort(STREAM_COMPARATOR);
                }

                TableSubtitleOption upperOption = TableSubtitleOption.getById(
                        matchingUpperSubtitles.get(0).getId(),
                        tableFileInfo.getSubtitleOptions()
                );
                Platform.runLater(() -> tableWithFiles.setSelectedAsUpper(upperOption));

                TableSubtitleOption lowerOption = TableSubtitleOption.getById(
                        matchingLowerSubtitles.get(0).getId(),
                        tableFileInfo.getSubtitleOptions()
                );
                Platform.runLater(() -> tableWithFiles.setSelectedAsLower(lowerOption));

                finishedSuccessfullyCount++;
                processedCount++;
            } catch (FfmpegException e) {
                if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                    break;
                } else {
                    throw new IllegalStateException();
                }
            }
        }

        return generateActionResult(
                allFileCount, processedCount, finishedSuccessfullyCount, notPossibleCount, failedCount
        );
    }

    private static List<FfmpegSubtitleStream> getMatchingUpperSubtitles(FileInfo fileInfo, GuiSettings settings) {
        return fileInfo.getFfmpegSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .filter(stream -> stream.getLanguage() == settings.getUpperLanguage())
                .collect(Collectors.toList());
    }

    private static List<FfmpegSubtitleStream> getMatchingLowerSubtitles(FileInfo fileInfo, GuiSettings settings) {
        return fileInfo.getFfmpegSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .filter(stream -> stream.getLanguage() == settings.getLowerLanguage())
                .collect(Collectors.toList());
    }

    private boolean loadStreamsIfNecessary(
            TableFileInfo tableFileInfo,
            FileInfo fileInfo,
            Collection<FfmpegSubtitleStream> ffmpegStreams,
            int processedCount,
            int allFileCount,
            BackgroundRunnerManager runnerManager
    ) throws FfmpegException {
        boolean result = true;

        int failedToLoadForFile = 0;

        for (FfmpegSubtitleStream ffmpegStream : ffmpegStreams) {
            runnerManager.updateMessage(
                    getUpdateMessage(processedCount, allFileCount, ffmpegStream, fileInfo.getFile())
            );

            if (ffmpegStream.getSubtitles() != null) {
                continue;
            }

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
            } catch (FfmpegException e) {
                if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                    setFileInfoErrorIfNecessary(failedToLoadForFile, tableFileInfo, tableWithFiles);
                    throw e;
                }

                result = false;
                Platform.runLater(
                        () -> tableWithFiles.failedToLoadSubtitles(
                                VideoTabBackgroundUtils.failedToLoadReasonFrom(e.getCode()),
                                tableSubtitleOption
                        )
                );
                failedToLoadForFile++;
            } catch (SubtitleParser.IncorrectFormatException e) {
                result = false;
                Platform.runLater(
                        () -> tableWithFiles.failedToLoadSubtitles(
                                VideoTabBackgroundUtils.FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT,
                                tableSubtitleOption
                        )
                );
                failedToLoadForFile++;
            }
        }

        setFileInfoErrorIfNecessary(failedToLoadForFile, tableFileInfo, tableWithFiles);

        return result;
    }

    private static String getUpdateMessage(
            int processedCount,
            int allFileCount,
            FfmpegSubtitleStream subtitleStream,
            File file
    ) {
        String progressPrefix = allFileCount > 1
                ? (processedCount + 1) + "/" + allFileCount + " "
                : "";

        return progressPrefix + "getting subtitles "
                + GuiUtils.languageToString(subtitleStream.getLanguage()).toUpperCase()
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
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
                "Auto-select has failed because failed to load subtitles",
                "Auto-select has failed because failed to load %d subtitles"
        );

        Platform.runLater(() -> tableWithFiles.setActionResult(ActionResult.onlyError(message), fileInfo));
    }

    private static ActionResult generateActionResult(
            int allFileCount,
            int processedCount,
            int finishedSuccessfullyCount,
            int notPossibleCount,
            int failedCount
    ) {
        String success = null;
        String warn = null;
        String error = null;

        if (processedCount == 0) {
            warn = "Task has been cancelled, nothing was done";
        } else if (finishedSuccessfullyCount == allFileCount) {
            success = GuiUtils.getTextDependingOnTheCount(
                    finishedSuccessfullyCount,
                    "Auto-selection has finished successfully for the file",
                    "Auto-selection has finished successfully for all %d files"
            );
        } else if (notPossibleCount == allFileCount) {
            warn = GuiUtils.getTextDependingOnTheCount(
                    notPossibleCount,
                    "Auto-selection is not possible for this file",
                    "Auto-selection is not possible for all %d files"
            );
        } else if (failedCount == allFileCount) {
            error = GuiUtils.getTextDependingOnTheCount(
                    failedCount,
                    "Failed to perform auto-selection for the file",
                    "Failed to perform auto-selection for all %d files"
            );
        } else {
            if (finishedSuccessfullyCount != 0) {
                success = String.format(
                        "Auto-selection has finished for %d/%d files successfully",
                        finishedSuccessfullyCount,
                        allFileCount
                );
            }

            if (processedCount != allFileCount) {
                if (finishedSuccessfullyCount == 0) {
                    warn = String.format(
                            "Auto-selection has been cancelled for %d/%d files",
                            allFileCount - processedCount,
                            allFileCount
                    );
                } else {
                    warn = String.format("cancelled for %d/%d", allFileCount - processedCount, allFileCount);
                }
            }

            if (notPossibleCount != 0) {
                if (processedCount != allFileCount) {
                    warn += String.format(", not possible for %d/%d", notPossibleCount, allFileCount);
                } else if (finishedSuccessfullyCount != 0) {
                    warn = String.format("not possible for %d/%d", notPossibleCount, allFileCount);
                } else {
                    warn = String.format(
                            "Auto-selection is not possible for %d/%d files",
                            notPossibleCount,
                            allFileCount
                    );
                }
            }

            if (failedCount != 0) {
                error = String.format("failed for %d/%d", failedCount, allFileCount);
            }
        }

        return new ActionResult(success, warn, error);
    }
}