package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import kirill.subtitlemerger.gui.core.GuiUtils;
import kirill.subtitlemerger.gui.core.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStream;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.function.Consumer;

@AllArgsConstructor
public class LoadSingleFileAllSubtitlesTask extends BackgroundTask<LoadFilesAllSubtitlesTask.Result> {
    private FileInfo fileInfo;

    private GuiFileInfo guiFileInfo;

    private Consumer<LoadFilesAllSubtitlesTask.Result> onFinish;

    private Ffmpeg ffmpeg;

    @Override
    protected LoadFilesAllSubtitlesTask.Result run() {
        updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);

        LoadFilesAllSubtitlesTask.Result result = new LoadFilesAllSubtitlesTask.Result(
                getStreamToLoadCount(fileInfo),
                0,
                0,
                0
        );

        setCancellationPossible(true);
        for (SubtitleStream stream : fileInfo.getSubtitleStreams()) {
            if (super.isCancelled()) {
                return result;
            }

            if (stream.getUnavailabilityReason() != null || stream.getSubtitles() != null) {
                continue;
            }

            updateMessage(
                    LoadFilesAllSubtitlesTask.getUpdateMessage(
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
                    return result;
                } else {
                    Platform.runLater(() -> guiStream.setFailedToLoadReason(BackgroundTaskUtils.guiTextFrom(e)));
                    result.setFailedToLoadCount(result.getFailedToLoadCount() + 1);
                }
            } catch (Parser.IncorrectFormatException e) {
                Platform.runLater(() -> guiStream.setFailedToLoadReason("subtitles seem to have incorrect format"));
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
            for (SubtitleStream stream : fileInfo.getSubtitleStreams()) {
                if (stream.getUnavailabilityReason() != null || stream.getSubtitles() != null) {
                    continue;
                }

                result++;
            }
        }

        return result;
    }

    @Override
    protected void onFinish(LoadFilesAllSubtitlesTask.Result result) {
        this.onFinish.accept(result);
    }
}
