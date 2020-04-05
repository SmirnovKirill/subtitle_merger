package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.GuiUtils;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.util.entities.ActionResult;
import kirill.subtitlemerger.logic.core.SubtitleWriter;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@AllArgsConstructor
public class MergeRunner implements BackgroundRunner<ActionResult> {
    private List<MergePreparationRunner.FileMergeInfo> filesMergeInfo;

    private List<File> confirmedFilesToOverwrite;

    private List<TableFileInfo> displayedTableFilesInfo;

    private List<FileInfo> allFilesInfo;

    private TableWithFiles tableWithFiles;

    private Ffmpeg ffmpeg;

    private GuiSettings settings;

    @Override
    public ActionResult run(BackgroundRunnerManager runnerManager) {
        int allFileCount = filesMergeInfo.size();
        int processedCount = 0;
        int finishedSuccessfullyCount = 0;
        int noAgreementCount = 0;
        int alreadyMergedCount = 0;
        int failedCount = 0;

        for (MergePreparationRunner.FileMergeInfo fileMergeInfo : filesMergeInfo) {
            TableFileInfo tableFileInfo = TableFileInfo.getById(fileMergeInfo.getId(), displayedTableFilesInfo);

            String progressMessagePrefix = getProgressMessagePrefix(processedCount, allFileCount, tableFileInfo);
            runnerManager.updateMessage(progressMessagePrefix + "...");

            FileInfo fileInfo = FileInfo.getById(fileMergeInfo.getId(), allFilesInfo);

            if (fileMergeInfo.getStatus() == MergePreparationRunner.FileMergeStatus.FAILED_TO_LOAD_SUBTITLES) {
                String message = GuiUtils.getTextDependingOnTheCount(
                        fileMergeInfo.getFailedToLoadSubtitlesCount(),
                        "Merge is unavailable because failed to load subtitles",
                        "Merge is unavailable because failed to load %d subtitles"
                );

                tableWithFiles.setActionResult(ActionResult.onlyError(message), tableFileInfo);
                failedCount++;
            } else if (fileMergeInfo.getStatus() == MergePreparationRunner.FileMergeStatus.DUPLICATE) {
                String message = "These subtitles have already been merged";
                tableWithFiles.setActionResult(ActionResult.onlyWarn(message), tableFileInfo);
                alreadyMergedCount++;
            } else if (fileMergeInfo.getStatus() == MergePreparationRunner.FileMergeStatus.OK) {
                if (noPermission(fileMergeInfo, confirmedFilesToOverwrite, settings)) {
                    String message = "Merge is unavailable because you need to agree to the file overwriting";
                    tableWithFiles.setActionResult(ActionResult.onlyWarn(message), tableFileInfo);
                    noAgreementCount++;
                } else {
                    try {
                        boolean injectInVideos = settings.getMergeMode() == GuiSettings.MergeMode.ORIGINAL_VIDEOS
                                || settings.getMergeMode() == GuiSettings.MergeMode.VIDEO_COPIES;
                        if (injectInVideos) {
                            ffmpeg.injectSubtitlesToFile(
                                    fileMergeInfo.getMergedSubtitles(),
                                    null,
                                    fileMergeInfo.getMergedSubtitles().getLanguage(),
                                    settings.isMarkMergedStreamAsDefault(),
                                    fileInfo
                            );
                        } else if (settings.getMergeMode() == GuiSettings.MergeMode.SEPARATE_SUBTITLE_FILES) {
                            FileUtils.writeStringToFile(
                                    fileMergeInfo.getFileWithResult(),
                                    SubtitleWriter.toSubRipText(fileMergeInfo.getMergedSubtitles()),
                                    StandardCharsets.UTF_8
                            );
                        } else {
                            throw new IllegalStateException();
                        }
                        finishedSuccessfullyCount++;
                    } catch (IOException e) {
                        String message = "Failed to write the result, probably there is no access to the file";
                        Platform.runLater(
                                () -> tableWithFiles.setActionResult(ActionResult.onlyError(message), tableFileInfo)
                        );
                        failedCount++;
                    } catch (FfmpegException e) {
                        if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                            break;
                        } else {
                            String message = "Ffmpeg returned an error";
                            Platform.runLater(
                                    () -> tableWithFiles.setActionResult(ActionResult.onlyError(message), tableFileInfo)
                            );
                            failedCount++;
                        }
                    }
                }
            } else {
                throw new IllegalStateException();
            }

            processedCount++;
        }

        return generateActionResult(
                allFileCount,
                processedCount,
                finishedSuccessfullyCount,
                noAgreementCount,
                alreadyMergedCount,
                failedCount
        );
    }

    private static String getProgressMessagePrefix(int processedCount, int allFileCount, TableFileInfo fileInfo) {
        String progressPrefix = allFileCount > 1
                ? String.format("%d/%d ", processedCount + 1, allFileCount) + "stage 2 of 2: processing file "
                : "Stage 2 of 2: processing file ";

        return progressPrefix + fileInfo.getFilePath();
    }

    private static boolean noPermission(
            MergePreparationRunner.FileMergeInfo fileMergeInfo,
            List<File> confirmedFilesToOverwrite,
            GuiSettings settings
    ) {
        boolean incorrectMode = settings.getMergeMode() != GuiSettings.MergeMode.VIDEO_COPIES
                && settings.getMergeMode() != GuiSettings.MergeMode.SEPARATE_SUBTITLE_FILES;
        if (incorrectMode) {
            return false;
        }

        File fileWithResult = fileMergeInfo.getFileWithResult();

        return fileWithResult.exists() && !confirmedFilesToOverwrite.contains(fileWithResult);
    }

    private static ActionResult generateActionResult(
            int allFileCount,
            int processedCount,
            int finishedSuccessfullyCount,
            int noAgreementCount,
            int alreadyMergedCount,
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
                    "Merge has finished successfully for the file",
                    "Merge has finished successfully for all %d files"
            );
        } else if (noAgreementCount == allFileCount) {
            warn = GuiUtils.getTextDependingOnTheCount(
                    noAgreementCount,
                    "Merge is not possible because you didn't agree to overwrite the file",
                    "Merge is not possible because you didn't agree to overwrite all %d files"
            );
        } else if (alreadyMergedCount == allFileCount) {
            warn = GuiUtils.getTextDependingOnTheCount(
                    failedCount,
                    "Selected subtitles have already been merged",
                    "Selected subtitles have already been merged for all %d files"
            );
        } else if (failedCount == allFileCount) {
            error = GuiUtils.getTextDependingOnTheCount(
                    failedCount,
                    "Failed to merge subtitles for the file",
                    "Failed to merge subtitles for all %d files"
            );
        } else {
            if (finishedSuccessfullyCount != 0) {
                success = String.format(
                        "Merge has finished for %d/%d files successfully",
                        finishedSuccessfullyCount,
                        allFileCount
                );
            }

            if (processedCount != allFileCount) {
                if (finishedSuccessfullyCount == 0) {
                    warn = String.format(
                            "Merge has been cancelled for %d/%d files",
                            allFileCount - processedCount,
                            allFileCount
                    );
                } else {
                    warn = String.format("cancelled for %d/%d", allFileCount - processedCount, allFileCount);
                }
            }

            if (noAgreementCount != 0) {
                if (processedCount != allFileCount) {
                    warn += String.format(", no agreement for %d/%d", noAgreementCount, allFileCount);
                } else if (finishedSuccessfullyCount != 0) {
                    warn = String.format("not agreement for %d/%d", noAgreementCount, allFileCount);
                } else {
                    warn = String.format(
                            "No agreement for %d/%d files",
                            noAgreementCount,
                            allFileCount
                    );
                }
            }

            if (alreadyMergedCount != 0) {
                if (processedCount != allFileCount || noAgreementCount != 0) {
                    warn += String.format(", already merged for %d/%d", alreadyMergedCount, allFileCount);
                } else if (finishedSuccessfullyCount != 0) {
                    warn = String.format("already merged for %d/%d", alreadyMergedCount, allFileCount);
                } else {
                    warn = String.format(
                            "Already merged for %d/%d files",
                            alreadyMergedCount,
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
