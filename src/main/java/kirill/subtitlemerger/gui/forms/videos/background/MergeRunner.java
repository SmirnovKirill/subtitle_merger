package kirill.subtitlemerger.gui.forms.videos.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegInjectInfo;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.ffmpeg.json.JsonFfprobeVideoInfo;
import kirill.subtitlemerger.logic.videos.Videos;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import kirill.subtitlemerger.logic.videos.entities.ExternalSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOption;
import kirill.subtitlemerger.logic.settings.MergeMode;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.stream.Collectors.toList;

@CommonsLog
@AllArgsConstructor
public class MergeRunner implements BackgroundRunner<MergeRunner.Result> {
    private List<MergePreparationRunner.FileMergeInfo> filesMergeInfo;

    private List<File> confirmedFilesToOverwrite;

    private File directoryForTempFile;

    private List<TableFileInfo> displayedTableFilesInfo;

    private List<VideoInfo> allFilesInfo;

    private TableWithFiles tableWithFiles;

    private Ffprobe ffprobe;

    private Ffmpeg ffmpeg;

    private Settings settings;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        int allFileCount = filesMergeInfo.size();
        int processedCount = 0;
        int finishedSuccessfullyCount = 0;
        int noAgreementCount = 0;
        int alreadyMergedCount = 0;
        int failedCount = 0;

        for (MergePreparationRunner.FileMergeInfo fileMergeInfo : filesMergeInfo) {
            TableFileInfo tableFileInfo = TableFileInfo.getById(fileMergeInfo.getId(), displayedTableFilesInfo);

            String progressMessagePrefix = getProgressMessagePrefix(processedCount, allFileCount, tableFileInfo);
            backgroundManager.updateMessage(progressMessagePrefix + "...");

            VideoInfo fileInfo = VideoInfo.getById(fileMergeInfo.getId(), allFilesInfo);

            if (fileMergeInfo.getStatus() == MergePreparationRunner.FileMergeStatus.FAILED_TO_LOAD_SUBTITLES) {
                String message = Utils.getTextDependingOnCount(
                        fileMergeInfo.getFailedToLoadSubtitlesCount(),
                        "Merge is unavailable because failed to load subtitles",
                        "Merge is unavailable because failed to load %d subtitles"
                );

                Platform.runLater(() -> tableWithFiles.setActionResult(ActionResult.onlyError(message), tableFileInfo));
                failedCount++;
            } else if (fileMergeInfo.getStatus() == MergePreparationRunner.FileMergeStatus.DUPLICATE) {
                String message = "Selected subtitles have already been merged";
                Platform.runLater(() -> tableWithFiles.setActionResult(ActionResult.onlyWarn(message), tableFileInfo));
                alreadyMergedCount++;
            } else if (fileMergeInfo.getStatus() == MergePreparationRunner.FileMergeStatus.OK) {
                if (noPermission(fileMergeInfo, confirmedFilesToOverwrite, settings)) {
                    String message = "Merge is unavailable because you need to agree to the file overwriting";
                    Platform.runLater(
                            () -> tableWithFiles.setActionResult(ActionResult.onlyWarn(message), tableFileInfo)
                    );
                    noAgreementCount++;
                } else {
                    try {
                        if (settings.getMergeMode() == MergeMode.ORIGINAL_VIDEOS) {
                            if (directoryForTempFile == null) {
                                String message = "Failed to get the directory for temp files";
                                Platform.runLater(
                                        () -> tableWithFiles.setActionResult(
                                                ActionResult.onlyError(message), tableFileInfo
                                        )
                                );
                                failedCount++;
                            } else {
                                FfmpegInjectInfo injectInfo = new FfmpegInjectInfo(
                                        fileMergeInfo.getMergedSubtitleText(),
                                        fileInfo.getBuiltInSubtitleOptions().size(),
                                        getMergedSubtitleLanguage(fileMergeInfo),
                                        "merged-" + getOptionTitleForFfmpeg(fileMergeInfo.getUpperSubtitles())
                                                + "-" + getOptionTitleForFfmpeg(fileMergeInfo.getLowerSubtitles()),
                                        settings.isMakeMergedStreamsDefault(),
                                        fileInfo.getBuiltInSubtitleOptions().stream()
                                                .filter(BuiltInSubtitleOption::isDefaultDisposition)
                                                .map(BuiltInSubtitleOption::getFfmpegIndex)
                                                .collect(toList()),
                                        fileInfo.getFile(),
                                        directoryForTempFile
                                );

                                ffmpeg.injectSubtitlesToFile(injectInfo);

                                try {
                                    updateFileInfo(
                                            fileMergeInfo.getMergedSubtitles(),
                                            fileMergeInfo.getMergedSubtitleText().getBytes().length,
                                            fileInfo,
                                            tableFileInfo,
                                            ffprobe,
                                            settings
                                    );
                                } catch (FfmpegException | IllegalStateException e) {
                                    String message = "Subtitles have been merged but failed to update stream list";
                                    Platform.runLater(
                                            () -> tableWithFiles.setActionResult(
                                                    ActionResult.onlyWarn(message), tableFileInfo
                                            )
                                    );
                                }

                                finishedSuccessfullyCount++;
                            }
                        } else if (settings.getMergeMode() == MergeMode.SEPARATE_SUBTITLE_FILES) {
                            FileUtils.writeStringToFile(
                                    fileMergeInfo.getFileWithResult(),
                                    fileMergeInfo.getMergedSubtitleText(),
                                    StandardCharsets.UTF_8
                            );

                            finishedSuccessfullyCount++;
                        } else {
                            throw new IllegalStateException();
                        }
                    } catch (IOException e) {
                        String message = "Failed to write the result, probably there is no access to the file";
                        Platform.runLater(
                                () -> tableWithFiles.setActionResult(ActionResult.onlyError(message), tableFileInfo)
                        );
                        failedCount++;
                    } catch (FfmpegException e) {
                        log.warn("failed to merge: " + e.getCode() + ", console output " + e.getConsoleOutput());
                        String message = "Ffmpeg returned an error";
                        Platform.runLater(
                                () -> tableWithFiles.setActionResult(ActionResult.onlyError(message), tableFileInfo)
                        );
                        failedCount++;
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } else {
                throw new IllegalStateException();
            }

            processedCount++;
        }

        List<TableFileInfo> filesToShowInfo = VideoTabBackgroundUtils.getSortedFilesInfo(
                displayedTableFilesInfo,
                settings.getSortBy(),
                settings.getSortDirection(),
                backgroundManager
        );

        return new Result(
                new TableFilesToShowInfo(
                        filesToShowInfo,
                        tableWithFiles.getAllSelectableCount(),
                        tableWithFiles.getSelectedAvailableCount(),
                        tableWithFiles.getSelectedUnavailableCount()
                ),
                generateActionResult(
                        allFileCount,
                        processedCount,
                        finishedSuccessfullyCount,
                        noAgreementCount,
                        alreadyMergedCount,
                        failedCount
                )
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
            Settings settings
    ) {
        if (settings.getMergeMode() != MergeMode.SEPARATE_SUBTITLE_FILES) {
            return false;
        }

        File fileWithResult = fileMergeInfo.getFileWithResult();

        return fileWithResult.exists() && !confirmedFilesToOverwrite.contains(fileWithResult);
    }

    private static LanguageAlpha3Code getMergedSubtitleLanguage(MergePreparationRunner.FileMergeInfo mergeInfo) {
        LanguageAlpha3Code result = null;
        if (mergeInfo.getUpperSubtitles() instanceof BuiltInSubtitleOption) {
            result = ((BuiltInSubtitleOption) mergeInfo.getUpperSubtitles()).getLanguage();
        }

        if (result == null && mergeInfo.getLowerSubtitles() instanceof BuiltInSubtitleOption) {
            result = ((BuiltInSubtitleOption) mergeInfo.getUpperSubtitles()).getLanguage();
        }

        if (result == null) {
            result = LanguageAlpha3Code.undefined;
        }

        return result;
    }

    private static String getOptionTitleForFfmpeg(SubtitleOption subtitleOption) {
        if (subtitleOption instanceof ExternalSubtitleOption) {
            return "external";
        } else if (subtitleOption instanceof BuiltInSubtitleOption) {
            LanguageAlpha3Code language = ((BuiltInSubtitleOption) subtitleOption).getLanguage();
            return language != null ? language.toString() : "unknown";
        } else {
            throw new IllegalStateException();
        }
    }

    private static void updateFileInfo(
            Subtitles subtitles,
            int subtitleSize,
            VideoInfo fileInfo,
            TableFileInfo tableFileInfo,
            Ffprobe ffprobe,
            Settings settings
    ) throws FfmpegException, InterruptedException {
        //todo diagnostics
        JsonFfprobeVideoInfo ffprobeInfo = ffprobe.getVideoInfo(fileInfo.getFile());
        List<BuiltInSubtitleOption> subtitleOptions = Videos.getSubtitleOptions(ffprobeInfo);
        if (settings.isMakeMergedStreamsDefault()) {
            for (BuiltInSubtitleOption stream : subtitleOptions) {
                stream.disableDefaultDisposition();
            }
        }

        List<String> currentOptionsIds = fileInfo.getSubtitleOptions().stream()
                .map(SubtitleOption::getId)
                .collect(toList());
        List<BuiltInSubtitleOption> newSubtitleOptions = subtitleOptions.stream()
                .filter(option -> !currentOptionsIds.contains(option.getId()))
                .collect(toList());
        if (newSubtitleOptions.size() != 1) {
            throw new IllegalStateException();
        }

        BuiltInSubtitleOption subtitleOption = newSubtitleOptions.get(0);
        subtitleOption.setSubtitlesAndSize(subtitles, subtitleSize);

        fileInfo.getSubtitleOptions().add(subtitleOption);
        fileInfo.setCurrentSizeAndLastModified();

        boolean haveHideableOptions = tableFileInfo.getSubtitleOptions().stream()
                .anyMatch(TableSubtitleOption::isHideable);
        TableSubtitleOption newSubtitleOption = VideoTabBackgroundUtils.tableSubtitleOptionFrom(
                subtitleOption,
                haveHideableOptions,
                settings
        );

        tableFileInfo.addSubtitleOption(newSubtitleOption);
        tableFileInfo.updateSizeAndLastModified(fileInfo.getSize(), fileInfo.getLastModified());
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
            success = Utils.getTextDependingOnCount(
                    finishedSuccessfullyCount,
                    "Merge has finished successfully for the file",
                    "Merge has finished successfully for all %d files"
            );
        } else if (noAgreementCount == allFileCount) {
            warn = Utils.getTextDependingOnCount(
                    noAgreementCount,
                    "Merge is not possible because you didn't agree to overwrite the file",
                    "Merge is not possible because you didn't agree to overwrite all %d files"
            );
        } else if (alreadyMergedCount == allFileCount) {
            warn = Utils.getTextDependingOnCount(
                    alreadyMergedCount,
                    "Selected subtitles have already been merged",
                    "Selected subtitles have already been merged for all %d files"
            );
        } else if (failedCount == allFileCount) {
            error = Utils.getTextDependingOnCount(
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
                    warn = String.format("no agreement for %d/%d", noAgreementCount, allFileCount);
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
                            "Subtitles have already been merged for %d/%d files",
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

    @AllArgsConstructor
    @Getter
    public static class Result {
        private TableFilesToShowInfo tableFilesToShowInfo;

        private ActionResult actionResult;
    }
}
