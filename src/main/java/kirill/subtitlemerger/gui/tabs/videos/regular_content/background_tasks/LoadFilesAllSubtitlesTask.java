package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import kirill.subtitlemerger.gui.core.GuiUtils;
import kirill.subtitlemerger.gui.core.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.core.entities.MultiPartResult;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStream;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@AllArgsConstructor
public class LoadFilesAllSubtitlesTask extends BackgroundTask<LoadFilesAllSubtitlesTask.Result> {
    private List<FileInfo> allFilesInfo;

    private List<GuiFileInfo> displayedGuiFilesInfo;

    private Consumer<Result> onFinish;

    private Ffmpeg ffmpeg;

    @Override
    protected Result run() {
        BackgroundTaskUtils.clearFileInfoResults(displayedGuiFilesInfo, this);
        List<GuiFileInfo> guiFilesInfoToWorkWith = getGuiFilesInfoToWorkWith(displayedGuiFilesInfo, this);

        updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);

        Result result = new Result(
                getStreamToLoadCount(guiFilesInfoToWorkWith, allFilesInfo, this),
                0,
                0,
                0
        );

        setCancellationPossible(true);
        for (GuiFileInfo guiFileInfo : guiFilesInfoToWorkWith) {
            FileInfo fileInfo = GuiUtils.findMatchingFileInfo(guiFileInfo, allFilesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
                continue;
            }

            int failedToLoadForFile = 0;

            for (SubtitleStream stream : fileInfo.getSubtitleStreams()) {
                if (super.isCancelled()) {
                    return result;
                }

                if (stream.getUnavailabilityReason() != null || stream.getSubtitles() != null) {
                    continue;
                }

                updateMessage(
                        getUpdateMessage(
                                result.getStreamToLoadCount(),
                                result.getProcessedCount(),
                                stream,
                                fileInfo.getFile()
                        )
                );

                GuiSubtitleStream guiStream = GuiUtils.findMatchingGuiStream(
                        stream.getFfmpegIndex(),
                        guiFileInfo.getSubtitleStreams()
                );
                try {
                    String subtitleText = ffmpeg.getSubtitlesText(stream.getFfmpegIndex(), fileInfo.getFile());
                    stream.setSubtitles(Parser.fromSubRipText(subtitleText, stream.getTitle(), stream.getLanguage()));

                    /*
                     * Have to call this in the JavaFX thread because this change can lead to updates on the screen.
                     */
                    Platform.runLater(() -> {
                        guiStream.setSize(stream.getSubtitles().getSize());
                        guiStream.setFailedToLoadReason(null);
                    });

                    result.setLoadedSuccessfullyCount(result.getLoadedSuccessfullyCount() + 1);
                } catch (FfmpegException e) {
                    if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                        setFileInfoErrorIfNecessary(failedToLoadForFile, guiFileInfo);
                        return result;
                    } else {
                        Platform.runLater(() -> guiStream.setFailedToLoadReason(BackgroundTaskUtils.guiTextFrom(e)));
                        result.setFailedToLoadCount(result.getFailedToLoadCount() + 1);
                        failedToLoadForFile++;
                    }
                } catch (Parser.IncorrectFormatException e) {
                    Platform.runLater(() -> guiStream.setFailedToLoadReason("subtitles seem to have incorrect format"));
                    result.setFailedToLoadCount(result.getFailedToLoadCount() + 1);
                    failedToLoadForFile++;
                }

                result.setProcessedCount(result.getProcessedCount() + 1);
            }

            setFileInfoErrorIfNecessary(failedToLoadForFile, guiFileInfo);

            boolean haveSubtitlesToLoad = fileInfo.haveSubtitlesToLoad();
            Platform.runLater(() -> guiFileInfo.setHaveSubtitleSizesToLoad(haveSubtitlesToLoad));
        }

        return result;
    }

    private static List<GuiFileInfo> getGuiFilesInfoToWorkWith(
            List<GuiFileInfo> displayedGuiFilesInfo,
            LoadFilesAllSubtitlesTask task
    ) {
        task.updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);
        task.updateMessage("getting list of files to work with...");

        return displayedGuiFilesInfo.stream().filter(GuiFileInfo::isSelected).collect(Collectors.toList());
    }

    private static int getStreamToLoadCount(
            List<GuiFileInfo> guiFilesToWorkWith,
            List<FileInfo> allFiles,
            LoadFilesAllSubtitlesTask task
    ) {
        task.updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);
        task.updateMessage("calculating number of subtitles to load...");

        int result = 0;

        for (GuiFileInfo guiFileToWorkWith : guiFilesToWorkWith) {
            FileInfo fileToWorkWith = GuiUtils.findMatchingFileInfo(guiFileToWorkWith, allFiles);
            if (!CollectionUtils.isEmpty(fileToWorkWith.getSubtitleStreams())) {
                for (SubtitleStream stream : fileToWorkWith.getSubtitleStreams()) {
                    if (stream.getUnavailabilityReason() != null || stream.getSubtitles() != null) {
                        continue;
                    }

                    result++;
                }
            }
        }

        return result;
    }

    static String getUpdateMessage(
            int streamToLoadCount,
            int processedCount,
            SubtitleStream subtitleStream,
            File file
    ) {
        String language = subtitleStream.getLanguage() != null
                ? subtitleStream.getLanguage().toString().toUpperCase()
                : "UNKNOWN LANGUAGE";

        String progressPrefix = streamToLoadCount > 1
                ? String.format("%d/%d ", processedCount + 1, streamToLoadCount)
                : "";

        return progressPrefix + "getting subtitle "
                + language
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }

    private static void setFileInfoErrorIfNecessary(int failedToLoadForFile, GuiFileInfo fileInfo) {
        if (failedToLoadForFile == 0) {
            return;
        }

        String message = GuiUtils.getTextDependingOnTheCount(
                failedToLoadForFile,
                "Failed to load subtitle size",
                "Failed to load %d subtitle sizes"
        );

        Platform.runLater(() -> fileInfo.setResultOnlyError(message));
    }

    public static MultiPartResult generateMultiPartResult(Result taskResult) {
        String success = null;
        String warn = null;
        String error = null;

        if (taskResult.getStreamToLoadCount() == 0) {
            warn = "There are no subtitle sizes to load";
        } else if (taskResult.getProcessedCount() == 0) {
            warn = "Task has been cancelled, nothing was loaded";
        } else if (taskResult.getLoadedSuccessfullyCount() == taskResult.getStreamToLoadCount()) {
            success = GuiUtils.getTextDependingOnTheCount(
                    taskResult.getLoadedSuccessfullyCount(),
                    "Subtitle size has been loaded successfully",
                    "All %d subtitle sizes have been loaded successfully"
            );
        } else if (taskResult.getFailedToLoadCount() == taskResult.getStreamToLoadCount()) {
            error = GuiUtils.getTextDependingOnTheCount(
                    taskResult.getFailedToLoadCount(),
                    "Failed to load subtitle size",
                    "Failed to load all %d subtitle sizes"
            );
        } else {
            if (taskResult.getLoadedSuccessfullyCount() != 0) {
                success = GuiUtils.getTextDependingOnTheCount(
                        taskResult.getLoadedSuccessfullyCount(),
                        String.format(
                                "1/%d subtitle sizes has been loaded successfully",
                                taskResult.getStreamToLoadCount()
                        ),
                        String.format(
                                "%%d/%d subtitle sizes have been loaded successfully",
                                taskResult.getStreamToLoadCount()
                        )
                );
            }

            if (taskResult.getProcessedCount() != taskResult.getStreamToLoadCount()) {
                if (taskResult.getLoadedSuccessfullyCount() == 0) {
                    warn = GuiUtils.getTextDependingOnTheCount(
                            taskResult.getStreamToLoadCount() - taskResult.getProcessedCount(),
                            String.format(
                                    "1/%d subtitle sizes' loading has been cancelled",
                                    taskResult.getStreamToLoadCount()
                            ),
                            String.format(
                                    "%%d/%d subtitle sizes' loading have been cancelled",
                                    taskResult.getStreamToLoadCount()
                            )
                    );
                } else {
                    warn = String.format(
                            "%d/%d cancelled",
                            taskResult.getStreamToLoadCount() - taskResult.getProcessedCount(),
                            taskResult.getStreamToLoadCount()
                    );
                }
            }

            if (taskResult.getFailedToLoadCount() != 0) {
                error = String.format(
                        "%d/%d failed",
                        taskResult.getFailedToLoadCount(),
                        taskResult.getStreamToLoadCount()
                );
            }
        }

        return new MultiPartResult(success, warn, error);
    }

    @Override
    protected void onFinish(Result result) {
        onFinish.accept(result);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class Result {
        protected int streamToLoadCount;

        protected int processedCount;

        protected int loadedSuccessfullyCount;

        protected int failedToLoadCount;
    }
}
