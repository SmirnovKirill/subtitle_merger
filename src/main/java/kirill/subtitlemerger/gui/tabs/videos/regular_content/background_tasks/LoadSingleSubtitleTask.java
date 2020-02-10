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

import java.util.function.Consumer;

@AllArgsConstructor
public class LoadSingleSubtitleTask extends BackgroundTask<LoadSingleSubtitleTask.Result> {
    private int ffmpegIndex;

    private FileInfo fileInfo;

    private GuiFileInfo guiFileInfo;

    private Ffmpeg ffmpeg;

    private Consumer<Result> onFinish;

    @Override
    protected Result run() {
        SubtitleStream subtitleStream = fileInfo.getSubtitleStreams().stream()
                .filter(stream -> stream.getFfmpegIndex() == ffmpegIndex)
                .findFirst().orElseThrow(IllegalStateException::new);
        GuiSubtitleStream guiSubtitleStream = GuiUtils.findMatchingGuiStream(
                subtitleStream.getFfmpegIndex(),
                guiFileInfo.getSubtitleStreams()
        );

        updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);
        updateMessage(
                LoadSubtitlesTask.getUpdateMessage(
                        1,
                        0,
                        subtitleStream,
                        fileInfo.getFile()
                )
        );

        setCancellationPossible(true);

        try {
            String subtitleText = ffmpeg.getSubtitlesText(subtitleStream.getFfmpegIndex(), fileInfo.getFile());
            subtitleStream.setSubtitles(
                    Parser.fromSubRipText(
                            subtitleText,
                            subtitleStream.getTitle(),
                            subtitleStream.getLanguage()
                    )
            );
            boolean haveSubtitlesToLoad = fileInfo.haveSubtitlesToLoad();

            Platform.runLater(() -> {
                guiSubtitleStream.setSize(subtitleStream.getSubtitles().getSize());
                guiSubtitleStream.setFailedToLoadReason(null);
                guiFileInfo.setHaveSubtitleSizesToLoad(haveSubtitlesToLoad);
            });

            return new Result(Status.SUCCESS);
        } catch (FfmpegException e) {
            if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                return new Result(Status.CANCELLED);
            } else {
                Platform.runLater(() -> guiSubtitleStream.setFailedToLoadReason(LoadSubtitlesTask.guiTextFrom(e)));
                return new Result(Status.ERROR);
            }
        } catch (Parser.IncorrectFormatException e) {
            Platform.runLater(() -> guiSubtitleStream.setFailedToLoadReason("subtitles seem to have incorrect format"));
            return new Result(Status.ERROR);
        }
    }

    public static MultiPartResult generateMultiPartResult(Result taskResult) {
        String success = null;
        String warn = null;
        String error = null;

        switch (taskResult.getStatus()) {
            case SUCCESS:
                success = "Subtitle size has been loaded successfully";
                break;
            case CANCELLED:
                warn = "Task has been cancelled";
                break;
            case ERROR:
                error = "Failed to load subtitle size";
                break;
            default:
                throw new IllegalStateException();
        }

        return new MultiPartResult(success, warn, error);
    }

    @Override
    protected void onFinish(Result result) {
        this.onFinish.accept(result);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private Status status;
    }

    public enum Status {
        SUCCESS,
        ERROR,
        CANCELLED
    }
}
