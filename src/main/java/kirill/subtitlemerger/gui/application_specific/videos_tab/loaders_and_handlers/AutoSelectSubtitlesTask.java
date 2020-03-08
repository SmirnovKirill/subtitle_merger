package kirill.subtitlemerger.gui.application_specific.videos_tab.loaders_and_handlers;

import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFfmpegSubtitleStream;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.core.SubtitleParser;
import kirill.subtitlemerger.logic.work_with_files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AutoSelectSubtitlesTask implements BackgroundRunner<AutoSelectSubtitlesTask.Result> {
    private static final Comparator<FfmpegSubtitleStream> STREAM_COMPARATOR = Comparator.comparing(
            (FfmpegSubtitleStream stream) -> stream.getSubtitles().getSize()
    ).reversed();

    private List<FileInfo> allFilesInfo;

    private List<TableFileInfo> displayedGuiFilesInfo;

    private Ffmpeg ffmpeg;

    private GuiSettings guiSettings;

    public AutoSelectSubtitlesTask(
            List<FileInfo> allFilesInfo,
            List<GuiFileInfo> displayedGuiFilesInfo,
            Ffmpeg ffmpeg,
            GuiSettings guiSettings
    ) {
        this.allFilesInfo = allFilesInfo;
        this.displayedGuiFilesInfo = displayedGuiFilesInfo;
        this.ffmpeg = ffmpeg;
        this.guiSettings = guiSettings;
    }

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        BackgroundTaskUtils.clearFileInfoResults(displayedGuiFilesInfo, runnerManager);

        List<GuiFileInfo> guiFilesInfoToWorkWith = getGuiFilesInfoToWorkWith(displayedGuiFilesInfo, runnerManager);

        Result result = new Result(
                guiFilesInfoToWorkWith.size(),
                0,
                0,
                0,
                0
        );

        runnerManager.setCancellationPossible(true);
        for (GuiFileInfo guiFileInfo : guiFilesInfoToWorkWith) {
            if (runnerManager.isCancelled()) {
                return result;
            }

            FileInfo fileInfo = GuiUtils.findMatchingFileInfo(guiFileInfo, allFilesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
                result.setNotPossibleCount(result.getNotPossibleCount() + 1);
                result.setProcessedCount(result.getProcessedCount() + 1);
                continue;
            }

            List<FfmpegSubtitleStream> matchingUpperSubtitles = getMatchingUpperSubtitles(fileInfo, guiSettings);
            List<FfmpegSubtitleStream> matchingLowerSubtitles = getMatchingLowerSubtitles(fileInfo, guiSettings);
            if (CollectionUtils.isEmpty(matchingUpperSubtitles) || CollectionUtils.isEmpty(matchingLowerSubtitles)) {
                result.setNotPossibleCount(result.getNotPossibleCount() + 1);
                result.setProcessedCount(result.getProcessedCount() + 1);
                continue;
            }

            try {
                boolean loadedSuccessfully = loadSizesIfNecessary(
                        fileInfo.getFile(),
                        matchingUpperSubtitles,
                        matchingLowerSubtitles,
                        guiFileInfo,
                        result,
                        runnerManager
                );
                if (!loadedSuccessfully) {
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.setProcessedCount(result.getProcessedCount() + 1);
                    continue;
                }

                if (matchingUpperSubtitles.size() > 1) {
                    matchingUpperSubtitles.sort(STREAM_COMPARATOR);
                }
                if (matchingLowerSubtitles.size() > 1) {
                    matchingLowerSubtitles.sort(STREAM_COMPARATOR);
                }

                GuiSubtitleStream.getById(
                        matchingUpperSubtitles.get(0).getId(),
                        guiFileInfo.getSubtitleStreams()
                ).setSelectedAsUpper(true);

                GuiSubtitleStream.getById(
                        matchingLowerSubtitles.get(0).getId(),
                        guiFileInfo.getSubtitleStreams()
                ).setSelectedAsLower(true);

                guiFileInfo.setHaveSubtitleSizesToLoad(fileInfo.haveSubtitlesToLoad());

                result.setFinishedSuccessfullyCount(result.getFinishedSuccessfullyCount() + 1);
                result.setProcessedCount(result.getProcessedCount() + 1);
            } catch (FfmpegException e) {
                if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                    return result;
                } else {
                    throw new IllegalStateException();
                }
            }
        }

        return result;
    }

    private static List<GuiFileInfo> getGuiFilesInfoToWorkWith(
            List<GuiFileInfo> displayedGuiFilesInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        runnerManager.updateMessage("getting list of files to work with...");

        return displayedGuiFilesInfo.stream().filter(GuiFileInfo::isSelected).collect(Collectors.toList());
    }

    private static List<FfmpegSubtitleStream> getMatchingUpperSubtitles(FileInfo fileInfo, GuiSettings guiSettings) {
        return fileInfo.getFfmpegSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .filter(stream -> stream.getLanguage() == guiSettings.getUpperLanguage())
                .collect(Collectors.toList());
    }

    private static List<FfmpegSubtitleStream> getMatchingLowerSubtitles(FileInfo fileInfo, GuiSettings guiSettings) {
        return fileInfo.getFfmpegSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .filter(stream -> stream.getLanguage() == guiSettings.getLowerLanguage())
                .collect(Collectors.toList());
    }

    private boolean loadSizesIfNecessary(
            File file,
            List<FfmpegSubtitleStream> upperSubtitleStreams,
            List<FfmpegSubtitleStream> lowerSubtitleStreams,
            GuiFileInfo fileInfo,
            Result taskResult,
            BackgroundRunnerManager runnerManager
    ) throws FfmpegException {
        boolean result = true;

        List<FfmpegSubtitleStream> subtitlesToLoad = new ArrayList<>();
        if (upperSubtitleStreams.size() > 1) {
            subtitlesToLoad.addAll(upperSubtitleStreams);
        }
        if (lowerSubtitleStreams.size() > 1) {
            subtitlesToLoad.addAll(lowerSubtitleStreams);
        }

        int failedToLoadForFile = 0;

        for (FfmpegSubtitleStream stream : subtitlesToLoad) {
            runnerManager.updateMessage(
                    getUpdateMessage(
                            taskResult.getProcessedCount(),
                            taskResult.getAllFileCount(),
                            stream,
                            file
                    )
            );

            if (stream.getSubtitles() != null) {
                continue;
            }

            GuiFfmpegSubtitleStream guiStream = GuiSubtitleStream.getById(
                    stream.getId(),
                    fileInfo.getFfmpegSubtitleStreams()
            );

            try {
                String subtitleText = ffmpeg.getSubtitlesText(stream.getFfmpegIndex(), file);
                stream.setSubtitles(SubtitleParser.fromSubRipText(subtitleText, stream.getLanguage()));

                /*
                 * Have to call this in the JavaFX thread because this change can lead to updates on the screen.
                 */
                Platform.runLater(() -> guiStream.setSize(stream.getSubtitles().getSize()));
            } catch (FfmpegException e) {
                if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                    setFileInfoErrorIfNecessary(failedToLoadForFile, fileInfo);
                    throw e;
                }

                result = false;
                Platform.runLater(() -> guiStream.setFailedToLoadReason(BackgroundTaskUtils.guiTextFrom(e)));
                failedToLoadForFile++;
            } catch (SubtitleParser.IncorrectFormatException e) {
                result = false;
                Platform.runLater(() -> guiStream.setFailedToLoadReason("subtitles seem to have an incorrect format"));
                failedToLoadForFile++;
            }
        }

        setFileInfoErrorIfNecessary(failedToLoadForFile, fileInfo);

        return result;
    }

    private static String getUpdateMessage(
            int processedCount,
            int allSubtitleCount,
            FfmpegSubtitleStream subtitleStream,
            File file
    ) {
        String progressPrefix = allSubtitleCount > 1
                ? (processedCount + 1) + "/" + allSubtitleCount + " "
                : "";

        return progressPrefix + "getting subtitle "
                + GuiUtils.languageToString(subtitleStream.getLanguage()).toUpperCase()
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }

    private static void setFileInfoErrorIfNecessary(int failedToLoadForFile, GuiFileInfo fileInfo) {
        if (failedToLoadForFile == 0) {
            return;
        }

        String message = GuiUtils.getTextDependingOnTheCount(
                failedToLoadForFile,
                "Auto-select has failed because failed to load subtitles",
                "Auto-select has failed because failed to load %d subtitles"
        );

        Platform.runLater(() -> fileInfo.setResultOnlyError(message));
    }

    public static ActionResult generateMultiPartResult(Result taskResult) {
        String success = null;
        String warn = null;
        String error = null;

        if (taskResult.getProcessedCount() == 0) {
            warn = "Task has been cancelled, nothing was done";
        } else if (taskResult.getFinishedSuccessfullyCount() == taskResult.getAllFileCount()) {
            success = GuiUtils.getTextDependingOnTheCount(
                    taskResult.getFinishedSuccessfullyCount(),
                    "Auto-selection has finished successfully for the file",
                    "Auto-selection has finished successfully for all %d files"
            );
        } else if (taskResult.getNotPossibleCount() == taskResult.getAllFileCount()) {
            warn = GuiUtils.getTextDependingOnTheCount(
                    taskResult.getNotPossibleCount(),
                    "Auto-selection is not possible for the file",
                    "Auto-selection is not possible for all %d files"
            );
        } else if (taskResult.getFailedCount() == taskResult.getAllFileCount()) {
            error = GuiUtils.getTextDependingOnTheCount(
                    taskResult.getFailedCount(),
                    "Failed to perform auto-selection for the file",
                    "Failed to perform auto-selection for all %d files"
            );
        } else {
            if (taskResult.getFinishedSuccessfullyCount() != 0) {
                success = String.format(
                        "Auto-selection has finished for %d/%d files successfully",
                        taskResult.getFinishedSuccessfullyCount(),
                        taskResult.getAllFileCount()
                );
            }

            if (taskResult.getProcessedCount() != taskResult.getAllFileCount()) {
                if (taskResult.getFinishedSuccessfullyCount() == 0) {
                    warn = String.format(
                            "Auto-selection has been cancelled for %d/%d files",
                            taskResult.getAllFileCount() - taskResult.getProcessedCount(),
                            taskResult.getAllFileCount()
                    );
                } else {
                    warn = String.format(
                            "cancelled for %d/%d",
                            taskResult.getAllFileCount() - taskResult.getProcessedCount(),
                            taskResult.getAllFileCount()
                    );
                }
            }

            if (taskResult.getNotPossibleCount() != 0) {
                if (taskResult.getProcessedCount() != taskResult.getAllFileCount()) {
                    warn += String.format(
                            ", not possible for %d/%d",
                            taskResult.getNotPossibleCount(),
                            taskResult.getAllFileCount()
                    );
                } else if (taskResult.getFinishedSuccessfullyCount() != 0) {
                    warn = String.format(
                            "not possible for %d/%d",
                            taskResult.getNotPossibleCount(),
                            taskResult.getAllFileCount()
                    );
                } else {
                    warn = String.format(
                            "Auto-selection is not possible for %d/%d files",
                            taskResult.getNotPossibleCount(),
                            taskResult.getAllFileCount()
                    );
                }
            }

            if (taskResult.getFailedCount() != 0) {
                error = String.format(
                        "failed for %d/%d",
                        taskResult.getFailedCount(),
                        taskResult.getAllFileCount()
                );
            }
        }

        return new ActionResult(success, warn, error);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class Result {
        private int allFileCount;

        private int processedCount;

        private int finishedSuccessfullyCount;

        private int notPossibleCount;

        private int failedCount;
    }
}
