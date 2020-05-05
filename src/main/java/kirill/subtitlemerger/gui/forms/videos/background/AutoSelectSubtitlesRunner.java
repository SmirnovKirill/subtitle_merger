package kirill.subtitlemerger.gui.forms.videos.background;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.FAILED_TO_LOAD_INCORRECT_FORMAT;
import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.failedToLoadReasonFrom;

@CommonsLog
@AllArgsConstructor
public class AutoSelectSubtitlesRunner implements BackgroundRunner<ActionResult> {
    private static final Comparator<BuiltInSubtitleOption> STREAM_COMPARATOR = Comparator.comparing(
            BuiltInSubtitleOption::getSize
    ).reversed();

    private List<TableVideo> displayedTableFilesInfo;

    private List<Video> filesInfo;

    private Ffmpeg ffmpeg;

    private Settings settings;

    @Override
    public ActionResult run(BackgroundManager backgroundManager) {
        VideosBackgroundUtils.clearActionResults(displayedTableFilesInfo, backgroundManager);

        List<TableVideo> selectedTableFilesInfo = VideosBackgroundUtils.getSelectedVideos(
                displayedTableFilesInfo,
                backgroundManager
        );

        int allFileCount = selectedTableFilesInfo.size();
        int processedCount = 0;
        int finishedSuccessfullyCount = 0;
        int notPossibleCount = 0;
        int failedCount = 0;

        backgroundManager.setIndeterminateProgress();

        backgroundManager.setCancellationDescription("Please be patient, this may take a while depending on the size.");
        backgroundManager.setCancellationPossible(true);

        for (TableVideo tableFileInfo : selectedTableFilesInfo) {
            backgroundManager.updateMessage(
                    VideosBackgroundUtils.getProcessFileProgressMessage(processedCount, allFileCount, tableFileInfo)
            );

            Video fileInfo = Video.getById(tableFileInfo.getId(), filesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getBuiltInSubtitleOptions())) {
                String message = "Auto-selection is unavailable because there are no subtitles";
                Platform.runLater(() -> tableFileInfo.setOnlyWarn(message));
                notPossibleCount++;
                processedCount++;
                continue;
            }

            List<BuiltInSubtitleOption> matchingUpperSubtitles = getMatchingUpperSubtitles(fileInfo, settings);
            List<BuiltInSubtitleOption> matchingLowerSubtitles = getMatchingLowerSubtitles(fileInfo, settings);
            if (CollectionUtils.isEmpty(matchingUpperSubtitles) || CollectionUtils.isEmpty(matchingLowerSubtitles)) {
                List<String> missingLanguages = new ArrayList<>();
                if (CollectionUtils.isEmpty(matchingUpperSubtitles)) {
                    missingLanguages.add(Utils.languageToString(settings.getUpperLanguage()).toUpperCase());
                }
                if (CollectionUtils.isEmpty(matchingLowerSubtitles)) {
                    missingLanguages.add(Utils.languageToString(settings.getLowerLanguage()).toUpperCase());
                }
                String message = "Auto-selection is unavailable because there are no "
                        + StringUtils.join(missingLanguages, "and") + " subtitles";
                Platform.runLater(() -> tableFileInfo.setOnlyWarn(message));

                notPossibleCount++;
                processedCount++;
                continue;
            }

            try {
                boolean loadedSuccessfully = loadStreams(
                        tableFileInfo,
                        fileInfo,
                        matchingUpperSubtitles,
                        matchingLowerSubtitles,
                        processedCount,
                        allFileCount,
                        backgroundManager
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
                Platform.runLater(() -> upperOption.setSelectedAsUpper(true));

                TableSubtitleOption lowerOption = TableSubtitleOption.getById(
                        matchingLowerSubtitles.get(0).getId(),
                        tableFileInfo.getSubtitleOptions()
                );
                Platform.runLater(() -> lowerOption.setSelectedAsLower(true));

                finishedSuccessfullyCount++;
                processedCount++;
            } catch (InterruptedException e) {
                break;
            }
        }

        return getActionResult(allFileCount, processedCount, finishedSuccessfullyCount, notPossibleCount, failedCount);
    }

    private static List<BuiltInSubtitleOption> getMatchingUpperSubtitles(Video fileInfo, Settings settings) {
        return fileInfo.getBuiltInSubtitleOptions().stream()
                .filter(stream -> stream.getNotValidReason() == null && !stream.isMerged())
                .filter(stream -> Utils.languagesEqual(stream.getLanguage(), settings.getUpperLanguage()))
                .collect(Collectors.toList());
    }

    private static List<BuiltInSubtitleOption> getMatchingLowerSubtitles(Video fileInfo, Settings settings) {
        return fileInfo.getBuiltInSubtitleOptions().stream()
                .filter(stream -> stream.getNotValidReason() == null && !stream.isMerged())
                .filter(stream -> Utils.languagesEqual(stream.getLanguage(), settings.getLowerLanguage()))
                .collect(Collectors.toList());
    }

    private boolean loadStreams(
            TableVideo tableFileInfo,
            Video fileInfo,
            List<BuiltInSubtitleOption> matchingUpperSubtitles,
            List<BuiltInSubtitleOption> matchingLowerSubtitles,
            int processedCount,
            int allFileCount,
            BackgroundManager backgroundManager
    ) throws InterruptedException {
        boolean result = true;

        List<BuiltInSubtitleOption> ffmpegStreams = new ArrayList<>();
        if (matchingUpperSubtitles.size() > 1) {
            ffmpegStreams.addAll(matchingUpperSubtitles);
        }
        if (matchingLowerSubtitles.size() > 1) {
            ffmpegStreams.addAll(matchingLowerSubtitles);
        }

        int failedToLoadForFile = 0;

        for (BuiltInSubtitleOption ffmpegStream : ffmpegStreams) {
            backgroundManager.updateMessage(
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
                String subtitleText = ffmpeg.getSubtitleText(
                        ffmpegStream.getFfmpegIndex(),
                        ffmpegStream.getFormat(),
                        fileInfo.getFile()
                );
                SubtitlesAndInput subtitlesAndInput = SubtitlesAndInput.from(
                        subtitleText.getBytes(),
                        StandardCharsets.UTF_8
                );
                if (subtitlesAndInput.isCorrectFormat()) {
                    ffmpegStream.setSubtitlesAndInput(subtitlesAndInput);
                    Platform.runLater(() -> tableSubtitleOption.loadedSuccessfully(subtitlesAndInput.getSize()));
                } else {
                    result = false;
                    Platform.runLater(() -> tableSubtitleOption.failedToLoad(FAILED_TO_LOAD_INCORRECT_FORMAT));
                    failedToLoadForFile++;
                }
            } catch (FfmpegException e) {
                log.warn("failed to get subtitle text: " + e.getCode() + ", console output " + e.getConsoleOutput());
                result = false;
                Platform.runLater(() -> tableSubtitleOption.failedToLoad(failedToLoadReasonFrom(e.getCode())));
                failedToLoadForFile++;
            } catch (InterruptedException e) {
                setFileInfoError(failedToLoadForFile, tableFileInfo);
                throw e;
            }
        }

        setFileInfoError(failedToLoadForFile, tableFileInfo);

        return result;
    }

    private static String getUpdateMessage(
            int processedCount,
            int allFileCount,
            BuiltInSubtitleOption subtitleStream,
            File file
    ) {
        String progressPrefix = allFileCount > 1
                ? (processedCount + 1) + "/" + allFileCount + " getting subtitles "
                : "Getting subtitles ";

        return progressPrefix
                + Utils.languageToString(subtitleStream.getLanguage()).toUpperCase()
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }

    private static void setFileInfoError(int failedToLoadForFile, TableVideo fileInfo) {
        if (failedToLoadForFile == 0) {
            return;
        }

        String message = Utils.getTextDependingOnCount(
                failedToLoadForFile,
                "Auto-select has failed because failed to load subtitles",
                "Auto-select has failed because failed to load %d subtitles"
        );

        Platform.runLater(() -> fileInfo.setOnlyError(message));
    }

    private static ActionResult getActionResult(
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
            success = Utils.getTextDependingOnCount(
                    finishedSuccessfullyCount,
                    "Auto-selection has finished successfully for the file",
                    "Auto-selection has finished successfully for all %d files"
            );
        } else if (notPossibleCount == allFileCount) {
            warn = Utils.getTextDependingOnCount(
                    notPossibleCount,
                    "Auto-selection is not possible for this file",
                    "Auto-selection is not possible for all %d files"
            );
        } else if (failedCount == allFileCount) {
            error = Utils.getTextDependingOnCount(
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