package kirill.subtitlemerger.gui.application_specific.videos_tab.background_tasks;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFfmpegSubtitleStream;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiSubtitleStream;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.core.SubtitleParser;
import kirill.subtitlemerger.logic.work_with_files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

@AllArgsConstructor
public class LoadSingleFileAllSubtitlesTask implements BackgroundRunner<LoadFilesAllSubtitlesTask.Result> {
    private FileInfo fileInfo;

    private GuiFileInfo guiFileInfo;

    private Ffmpeg ffmpeg;

    @Override
    public LoadFilesAllSubtitlesTask.Result run(BackgroundRunnerManager runnerManager) {
        runnerManager.updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);

        LoadFilesAllSubtitlesTask.Result result = new LoadFilesAllSubtitlesTask.Result(
                getStreamToLoadCount(fileInfo),
                0,
                0,
                0
        );

        runnerManager.setCancellationPossible(true);
        for (FfmpegSubtitleStream stream : fileInfo.getFfmpegSubtitleStreams()) {
            if (runnerManager.isCancelled()) {
                return result;
            }

            if (stream.getUnavailabilityReason() != null || stream.getSubtitles() != null) {
                continue;
            }

            runnerManager.updateMessage(
                    LoadFilesAllSubtitlesTask.getUpdateMessage(
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
                    return result;
                } else {
                    Platform.runLater(() -> guiStream.setFailedToLoadReason(BackgroundTaskUtils.guiTextFrom(e)));
                    result.setFailedToLoadCount(result.getFailedToLoadCount() + 1);
                }
            } catch (SubtitleParser.IncorrectFormatException e) {
                Platform.runLater(() -> guiStream.setFailedToLoadReason("subtitles seem to have an incorrect format"));
                result.setFailedToLoadCount(result.getFailedToLoadCount() + 1);
            }

            result.setProcessedCount(result.getProcessedCount() + 1);
        }

        boolean haveSubtitlesToLoad = fileInfo.haveSubtitlesToLoad();
        Platform.runLater(() -> guiFileInfo.setHaveSubtitleSizesToLoad(haveSubtitlesToLoad));

        return result;
    }

    private static int getStreamToLoadCount(FileInfo fileInfo) {
        int result = 0;

        if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
            for (FfmpegSubtitleStream stream : fileInfo.getFfmpegSubtitleStreams()) {
                if (stream.getUnavailabilityReason() != null || stream.getSubtitles() != null) {
                    continue;
                }

                result++;
            }
        }

        return result;
    }
}

