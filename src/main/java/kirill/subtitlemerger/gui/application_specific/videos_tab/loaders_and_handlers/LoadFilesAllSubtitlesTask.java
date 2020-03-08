package kirill.subtitlemerger.gui.application_specific.videos_tab.loaders_and_handlers;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
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
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class LoadFilesAllSubtitlesTask implements BackgroundRunner<LoadFilesAllSubtitlesTask.Result> {
    private List<FileInfo> allFilesInfo;

    private List<GuiFileInfo> displayedGuiFilesInfo;

    private Ffmpeg ffmpeg;

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        BackgroundTaskUtils.clearFileInfoResults(displayedGuiFilesInfo, runnerManager);
        List<GuiFileInfo> guiFilesInfoToWorkWith = getGuiFilesInfoToWorkWith(displayedGuiFilesInfo, runnerManager);

        runnerManager.updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);

        Result result = new Result(
                getStreamToLoadCount(guiFilesInfoToWorkWith, allFilesInfo, runnerManager),
                0,
                0,
                0
        );

        runnerManager.setCancellationPossible(true);
        for (GuiFileInfo guiFileInfo : guiFilesInfoToWorkWith) {
            FileInfo fileInfo = GuiUtils.findMatchingFileInfo(guiFileInfo, allFilesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
                continue;
            }

            int failedToLoadForFile = 0;

            for (FfmpegSubtitleStream stream : fileInfo.getFfmpegSubtitleStreams()) {
                if (runnerManager.isCancelled()) {
                    return result;
                }

                if (stream.getUnavailabilityReason() != null || stream.getSubtitles() != null) {
                    continue;
                }

                runnerManager.updateMessage(
                        getUpdateMessage(
                                result.getStreamToLoadCount(),
                                result.getProcessedCount(),
                                stream,
                                fileInfo.getFile()
                        )
                );

                GuiFfmpegSubtitleStream guiStream = GuiSubtitleStream.getById(
                        stream.getId(),
                        guiFileInfo.getFfmpegSubtitleStreams()
                );
                try {
                    String subtitleText = ffmpeg.getSubtitlesText(stream.getFfmpegIndex(), fileInfo.getFile());
                    stream.setSubtitles(SubtitleParser.fromSubRipText(subtitleText, stream.getLanguage()));

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
                } catch (SubtitleParser.IncorrectFormatException e) {
                    Platform.runLater(() -> guiStream.setFailedToLoadReason("subtitles seem to have an incorrect format"));
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
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);
        runnerManager.updateMessage("getting list of files to work with...");

        return displayedGuiFilesInfo.stream().filter(GuiFileInfo::isSelected).collect(Collectors.toList());
    }

    private static int getStreamToLoadCount(
            List<GuiFileInfo> guiFilesToWorkWith,
            List<FileInfo> allFiles,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);
        runnerManager.updateMessage("calculating number of subtitles to load...");

        int result = 0;

        for (GuiFileInfo guiFileToWorkWith : guiFilesToWorkWith) {
            FileInfo fileToWorkWith = GuiUtils.findMatchingFileInfo(guiFileToWorkWith, allFiles);
            if (!CollectionUtils.isEmpty(fileToWorkWith.getFfmpegSubtitleStreams())) {
                for (FfmpegSubtitleStream stream : fileToWorkWith.getFfmpegSubtitleStreams()) {
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
            FfmpegSubtitleStream subtitleStream,
            File file
    ) {
        String progressPrefix = streamToLoadCount > 1
                ? String.format("%d/%d ", processedCount + 1, streamToLoadCount)
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
                "Failed to load subtitles",
                "Failed to load %d subtitles"
        );

        Platform.runLater(() -> fileInfo.setResultOnlyError(message));
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
