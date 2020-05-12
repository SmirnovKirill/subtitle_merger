package kirill.subtitlemerger.gui.forms.videos.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegInjectInfo;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.ffmpeg.json.JsonFfprobeVideoInfo;
import kirill.subtitlemerger.logic.settings.MergeMode;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.Videos;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.ExternalSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
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
public class MergeRunner implements BackgroundRunner<ActionResult> {
    private List<MergePrepareRunner.FileMergeInfo> filesMergeInfo;

    private List<File> confirmedFilesToOverwrite;

    private File directoryForTempFile;

    private List<TableVideo> displayedTableVideos;

    private List<Video> allFilesInfo;

    private Ffprobe ffprobe;

    private Ffmpeg ffmpeg;

    private Settings settings;

    @Override
    public ActionResult run(BackgroundManager backgroundManager) {
        int allFileCount = filesMergeInfo.size();
        int processedCount = 0;
        int finishedSuccessfullyCount = 0;
        int noAgreementCount = 0;
        int alreadyMergedCount = 0;
        int failedCount = 0;

        for (MergePrepareRunner.FileMergeInfo fileMergeInfo : filesMergeInfo) {
            TableVideo tableFileInfo = TableVideo.getById(fileMergeInfo.getId(), displayedTableVideos);

            String progressMessagePrefix = getProgressMessagePrefix(processedCount, allFileCount, tableFileInfo);
            backgroundManager.updateMessage(progressMessagePrefix + "...");

            Video fileInfo = Video.getById(fileMergeInfo.getId(), allFilesInfo);

            if (fileMergeInfo.getStatus() == MergePrepareRunner.FileMergeStatus.FAILED_TO_LOAD_SUBTITLES) {
                String message = Utils.getTextDependingOnCount(
                        fileMergeInfo.getFailedToLoadSubtitlesCount(),
                        "Merge is unavailable because failed to load subtitles",
                        "Merge is unavailable because failed to load %d subtitles"
                );

                Platform.runLater(() -> tableFileInfo.setOnlyError(message));
                failedCount++;
            } else if (fileMergeInfo.getStatus() == MergePrepareRunner.FileMergeStatus.DUPLICATE) {
                String message = "Selected subtitles have already been merged";
                Platform.runLater(() -> tableFileInfo.setOnlyWarn(message));
                alreadyMergedCount++;
            } else if (fileMergeInfo.getStatus() == MergePrepareRunner.FileMergeStatus.OK) {
                if (noPermission(fileMergeInfo, confirmedFilesToOverwrite, settings)) {
                    String message = "Merge is unavailable because you need to agree to the file overwriting";
                    Platform.runLater(() -> tableFileInfo.setOnlyWarn(message));
                    noAgreementCount++;
                } else {
                    try {
                        if (settings.getMergeMode() == MergeMode.ORIGINAL_VIDEOS) {
                            if (directoryForTempFile == null) {
                                String message = "Failed to get the directory for temp files";
                                Platform.runLater(() -> tableFileInfo.setOnlyError(message));

                                failedCount++;
                            } else {
                                FfmpegInjectInfo injectInfo = new FfmpegInjectInfo(
                                        fileMergeInfo.getMergedSubtitleText(),
                                        fileInfo.getBuiltInOptions().size(),
                                        getMergedSubtitleLanguage(fileMergeInfo),
                                        "merged-" + getOptionTitleForFfmpeg(fileMergeInfo.getUpperSubtitles())
                                                + "-" + getOptionTitleForFfmpeg(fileMergeInfo.getLowerSubtitles()),
                                        settings.isMakeMergedStreamsDefault(),
                                        fileInfo.getBuiltInOptions().stream()
                                                .filter(BuiltInSubtitleOption::isDefaultDisposition)
                                                .map(BuiltInSubtitleOption::getFfmpegIndex)
                                                .collect(toList()),
                                        fileInfo.getFile(),
                                        directoryForTempFile
                                );

                                ffmpeg.injectSubtitlesToFile(injectInfo);

                                try {
                                    updateFileInfo(
                                            fileInfo,
                                            tableFileInfo,
                                            ffprobe,
                                            ffmpeg,
                                            settings
                                    );
                                } catch (FfmpegException | IllegalStateException e) {
                                    String message = "The subtitles have been merged but failed to update stream list";
                                    Platform.runLater(() -> tableFileInfo.setOnlyWarn(message));
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
                        Platform.runLater(() -> tableFileInfo.setOnlyError(message));
                        failedCount++;
                    } catch (FfmpegException e) {
                        log.warn("failed to merge: " + e.getCode() + ", console output " + e.getConsoleOutput());
                        String message = "Ffmpeg returned an error";
                        Platform.runLater(() -> tableFileInfo.setOnlyError(message));
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

        return getActionResult(
                allFileCount,
                processedCount,
                finishedSuccessfullyCount,
                noAgreementCount,
                alreadyMergedCount,
                failedCount
        );
    }

    private static String getProgressMessagePrefix(int processedCount, int allFileCount, TableVideo fileInfo) {
        String progressPrefix = allFileCount > 1
                ? String.format("%d/%d ", processedCount + 1, allFileCount) + "stage 2 of 2: processing file "
                : "Stage 2 of 2: processing file ";

        return progressPrefix + fileInfo.getFilePath();
    }

    private static boolean noPermission(
            MergePrepareRunner.FileMergeInfo fileMergeInfo,
            List<File> confirmedFilesToOverwrite,
            Settings settings
    ) {
        if (settings.getMergeMode() != MergeMode.SEPARATE_SUBTITLE_FILES) {
            return false;
        }

        File fileWithResult = fileMergeInfo.getFileWithResult();

        return fileWithResult.exists() && !confirmedFilesToOverwrite.contains(fileWithResult);
    }

    private static LanguageAlpha3Code getMergedSubtitleLanguage(MergePrepareRunner.FileMergeInfo mergeInfo) {
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
            Video fileInfo,
            TableVideo tableVideoInfo,
            Ffprobe ffprobe,
            Ffmpeg ffmpeg,
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

        List<String> currentOptionsIds = fileInfo.getOptions().stream()
                .map(SubtitleOption::getId)
                .collect(toList());
        List<BuiltInSubtitleOption> newSubtitleOptions = subtitleOptions.stream()
                .filter(option -> !currentOptionsIds.contains(option.getId()))
                .collect(toList());
        if (newSubtitleOptions.size() != 1) {
            //todo just mark as incorrect or something
            throw new IllegalStateException();
        }

        BuiltInSubtitleOption subtitleOption = newSubtitleOptions.get(0);

        //todo background message
        String subtitleText = ffmpeg.getSubtitleText(
                subtitleOption.getFfmpegIndex(),
                subtitleOption.getFormat(),
                fileInfo.getFile()
        );
        SubtitlesAndInput subtitlesAndInput = SubtitlesAndInput.from(
                subtitleText.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
        if (!subtitlesAndInput.isCorrectFormat()) {
            //todo separate message
            throw new IllegalStateException();
        }

        subtitleOption.setSubtitlesAndInput(subtitlesAndInput);

        fileInfo.getOptions().add(subtitleOption);
        fileInfo.setCurrentSizeAndLastModified();

        boolean haveHideableOptions = tableVideoInfo.getOptions().stream()
                .anyMatch(TableSubtitleOption::isHideable);
        TableSubtitleOption newSubtitleOption = VideosBackgroundUtils.tableOptionFrom(
                subtitleOption,
                haveHideableOptions,
                tableVideoInfo,
                settings
        );

        Platform.runLater(() -> {
            tableVideoInfo.setSizeAndLastModified(fileInfo.getSize(), fileInfo.getLastModified());
            tableVideoInfo.addOption(newSubtitleOption);
        });
    }

    private static ActionResult getActionResult(
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
        private ActionResult actionResult;
    }
}
