package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStreamInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class LoadSubtitlesTask extends BackgroundTask<LoadSubtitlesTask.Result> {
    private Integer subtitleIndex;

    private List<FileInfo> filesInfo;

    private List<GuiFileInfo> guiFilesInfo;

    private Ffmpeg ffmpeg;

    public LoadSubtitlesTask(
            List<FileInfo> filesInfo,
            List<GuiFileInfo> guiFilesInfo,
            Ffmpeg ffmpeg
    ) {
        super();

        this.filesInfo = filesInfo;
        this.guiFilesInfo = guiFilesInfo;
        this.ffmpeg = ffmpeg;
    }

    public LoadSubtitlesTask(
            FileInfo fileInfo,
            GuiFileInfo guiFileInfo,
            Ffmpeg ffmpeg
    ) {
        super();

        this.filesInfo = Collections.singletonList(fileInfo);
        this.guiFilesInfo = Collections.singletonList(guiFileInfo);
        this.ffmpeg = ffmpeg;
    }

    public LoadSubtitlesTask(
            int subtitleIndex,
            FileInfo fileInfo,
            GuiFileInfo guiFileInfo,
            Ffmpeg ffmpeg
    ) {
        super();

        this.subtitleIndex = subtitleIndex;
        this.filesInfo = Collections.singletonList(fileInfo);
        this.guiFilesInfo = Collections.singletonList(guiFileInfo);
        this.ffmpeg = ffmpeg;
    }

    @Override
    protected Result call() {
        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        updateMessage("calculating number of subtitles to load...");

        int subtitlesToLoadQuantity = getSubtitlesToLoadQuantity();
        updateProgress(0, subtitlesToLoadQuantity);

        int loadableQuantity = 0;
        int failedToLoadQuantity = 0;
        int alreadyLoadedQuantity = 0;
        int loadedQuantity = 0;
        for (FileInfo fileInfo : filesInfo) {
            if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
                for (SubtitleStreamInfo subtitleStream : fileInfo.getSubtitleStreamsInfo()) {
                    if (subtitleStream.getUnavailabilityReason() != null) {
                        continue;
                    }

                    if (subtitleIndex != null && subtitleIndex != subtitleStream.getIndex()) {
                        continue;
                    }

                    loadableQuantity++;

                    if (subtitleStream.getSubtitles() != null) {
                        alreadyLoadedQuantity++;
                        continue;
                    }

                    try {
                        updateMessage(getUpdateMessage(subtitleStream, fileInfo.getFile()));

                        String subtitleText = ffmpeg.getSubtitlesText(subtitleStream.getIndex(), fileInfo.getFile());
                        subtitleStream.setSubtitles(
                                Parser.fromSubRipText(
                                        subtitleText,
                                        subtitleStream.getTitle(),
                                        subtitleStream.getLanguage()
                                )
                        );
                        updateProgress(++loadedQuantity, subtitlesToLoadQuantity);
                    } catch (FfmpegException | Parser.IncorrectFormatException e) {
                        failedToLoadQuantity++;
                    }
                }
            }
        }

        return new Result(loadableQuantity, failedToLoadQuantity, alreadyLoadedQuantity, loadedQuantity);
    }

    private int getSubtitlesToLoadQuantity() {
        if (subtitleIndex != null) {
            return  1;
        } else {
            int result = 0;

            for (FileInfo fileInfo : filesInfo) {
                if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
                    for (SubtitleStreamInfo subtitleStream : fileInfo.getSubtitleStreamsInfo()) {
                        if (subtitleStream.getUnavailabilityReason() != null) {
                            continue;
                        }

                        if (subtitleIndex != null && subtitleIndex != subtitleStream.getIndex()) {
                            continue;
                        }

                        if (subtitleStream.getSubtitles() != null) {
                            continue;
                        }

                        result++;
                    }
                }
            }

            return result;
        }
    }

    private static String getUpdateMessage(SubtitleStreamInfo subtitleStream, File file) {
        String language = subtitleStream.getLanguage() != null
                ? subtitleStream.getLanguage().toString().toUpperCase()
                : "UNKNOWN LANGUAGE";
        return "getting subtitle "
                + language
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private int loadableQuantity;

        private int failedToLoadQuantity;

        private int alreadyLoadedQuantity;

        private int loadedQuantity;
    }
}
