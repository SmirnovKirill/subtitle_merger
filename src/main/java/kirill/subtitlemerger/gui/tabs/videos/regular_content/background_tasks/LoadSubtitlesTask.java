package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
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

public abstract class LoadSubtitlesTask extends BackgroundTask<LoadSubtitlesTask.Result> {
    private Ffmpeg ffmpeg;

    private Consumer<Result> onFinish;

    LoadSubtitlesTask(Ffmpeg ffmpeg, Consumer<Result> onFinish) {
        this.ffmpeg = ffmpeg;
        this.onFinish = onFinish;
    }

    protected Result load(
            int allSubtitleCount,
            List<GuiFileInfo> guiFilesInfo,
            List<FileInfo> filesInfo
    ) {
        Result result = new Result(
                allSubtitleCount,
                0,
                0,
                0,
                0
        );

        setCancellationPossible(true);

        for (GuiFileInfo guiFileInfo : guiFilesInfo) {
            FileInfo fileInfo = RegularContentController.findMatchingFileInfo(guiFileInfo, filesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
                continue;
            }

            for (SubtitleStream subtitleStream : fileInfo.getSubtitleStreams()) {
                if (super.isCancelled()) {
                    return result;
                }

                if (subtitleStream.getUnavailabilityReason() != null) {
                    continue;
                }

                if (subtitleStream.getSubtitles() != null) {
                    result.setLoadedBeforeCount(result.getLoadedSuccessfullyCount() + 1);
                    result.setProcessedCount(result.getProcessedCount() + 1);
                    continue;
                }

                updateMessage(getUpdateMessage(result, subtitleStream, fileInfo.getFile()));

                GuiSubtitleStream guiSubtitleStream = RegularContentController.findMatchingGuiStream(
                        subtitleStream.getFfmpegIndex(),
                        guiFileInfo.getSubtitleStreams()
                );

                try {
                    String subtitleText = ffmpeg.getSubtitlesText(subtitleStream.getFfmpegIndex(), fileInfo.getFile());
                    subtitleStream.setSubtitlesAndSize(
                            Parser.fromSubRipText(
                                    subtitleText,
                                    subtitleStream.getTitle(),
                                    subtitleStream.getLanguage()
                            )
                    );

                    /*
                     * Have to call this in the JavaFX thread because this change can lead to updates on the screen.
                     */
                    Platform.runLater(() -> {
                        guiSubtitleStream.setSize(subtitleStream.getSubtitleSize());
                        guiSubtitleStream.setFailedToLoadReason(null);
                    });

                    result.setLoadedSuccessfullyCount(result.getLoadedSuccessfullyCount() + 1);
                } catch (FfmpegException e) {
                    if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                        return result;
                    } else {
                        Platform.runLater(() -> {
                            guiSubtitleStream.setFailedToLoadReason(guiTextFrom(e));
                        });
                        result.setFailedToLoadCount(result.getFailedToLoadCount() + 1);
                    }
                } catch (Parser.IncorrectFormatException e) {
                    Platform.runLater(() -> {
                        guiSubtitleStream.setFailedToLoadReason("subtitles seem to have incorrect format");
                    });

                    result.setFailedToLoadCount(result.getFailedToLoadCount() + 1);
                }

                result.setProcessedCount(result.getProcessedCount() + 1);
            }

            guiFileInfo.setHaveSubtitleSizesToLoad(RegularContentController.haveSubtitlesToLoad(fileInfo));
        }

       return result;
    }

    static String getUpdateMessage(
            Result taskResult,
            SubtitleStream subtitleStream,
            File file
    ) {
        String language = subtitleStream.getLanguage() != null
                ? subtitleStream.getLanguage().toString().toUpperCase()
                : "UNKNOWN LANGUAGE";

        String progressPrefix = taskResult.getAllSubtitleCount() > 1
                ? (taskResult.getProcessedCount() + 1) + "/" + taskResult.getAllSubtitleCount() + " "
                : "";

        return progressPrefix + "getting subtitle "
                + language
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }

    public static String guiTextFrom(FfmpegException e) {
        if (e.getCode() == FfmpegException.Code.GENERAL_ERROR) {
            return "ffmpeg returned an error";
        }

        throw new IllegalStateException();
    }

    @Override
    protected void onFinish(Result result) {
        onFinish.accept(result);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class Result {
        protected int allSubtitleCount;

        protected int loadedBeforeCount;

        protected int processedCount;

        protected int loadedSuccessfullyCount;

        protected int failedToLoadCount;
    }
}
