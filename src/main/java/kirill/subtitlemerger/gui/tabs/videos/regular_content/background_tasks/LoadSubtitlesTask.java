package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.event.Event;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStreamInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class LoadSubtitlesTask extends BackgroundTask<Void> {
    private Integer subtitleIndex;

    private List<FileInfo> filesInfo;

    private List<GuiFileInfo> guiFilesInfo;

    private Ffmpeg ffmpeg;

    @Getter
    private boolean cancelled;

    @Getter
    private int loadableCount;

    @Getter
    private int failedToLoadCount;

    @Getter
    private int loadedBeforeCount;

    @Getter
    private int loadedSuccessfullyCount;

    public LoadSubtitlesTask(
            List<FileInfo> filesInfo,
            List<GuiFileInfo> guiFilesInfo,
            Ffmpeg ffmpeg
    ) {
        super();
        setOnCancelled(this::taskCancelled);

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
        setOnCancelled(this::taskCancelled);

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
        setOnCancelled(this::taskCancelled);

        this.subtitleIndex = subtitleIndex;
        this.filesInfo = Collections.singletonList(fileInfo);
        this.guiFilesInfo = Collections.singletonList(guiFileInfo);
        this.ffmpeg = ffmpeg;
    }

    private void taskCancelled(Event e) {
        cancel();
    }

    @Override
    protected Void call() {
        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        updateMessage("calculating number of subtitles to load...");

        int subtitleToLoadCount = getSubtitleToLoadCount();

        int processedCount = 0;
        mainLoop: for (FileInfo fileInfo : filesInfo) {
            if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
                for (SubtitleStreamInfo subtitleStream : fileInfo.getSubtitleStreamsInfo()) {
                    if (isCancelled()) {
                        cancelled = true;
                        break mainLoop;
                    }

                    if (subtitleStream.getUnavailabilityReason() != null) {
                        continue;
                    }

                    if (subtitleIndex != null && subtitleIndex != subtitleStream.getIndex()) {
                        continue;
                    }

                    loadableCount++;

                    if (subtitleStream.getSubtitles() != null) {
                        loadedBeforeCount++;
                        continue;
                    }

                    try {
                        updateMessage(
                                getUpdateMessage(
                                        processedCount,
                                        subtitleToLoadCount,
                                        subtitleStream,
                                        fileInfo.getFile()
                                )
                        );

                        String subtitleText = ffmpeg.getSubtitlesText(subtitleStream.getIndex(), fileInfo.getFile());
                        subtitleStream.setSubtitles(
                                Parser.fromSubRipText(
                                        subtitleText,
                                        subtitleStream.getTitle(),
                                        subtitleStream.getLanguage()
                                )
                        );
                        loadedSuccessfullyCount++;
                    } catch (FfmpegException e) {
                        if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                            cancelled = true;
                            break mainLoop;
                        } else {
                            //todo save reason
                            failedToLoadCount++;
                        }
                    } catch (Parser.IncorrectFormatException e) {
                        //todo save reason
                        failedToLoadCount++;
                    }

                    processedCount++;
                }
            }
        }

        return null;
    }

    private int getSubtitleToLoadCount() {
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

    private static String getUpdateMessage(
            int processedCount,
            int subtitleToLoadCount,
            SubtitleStreamInfo subtitleStream,
            File file
    ) {
        String language = subtitleStream.getLanguage() != null
                ? subtitleStream.getLanguage().toString().toUpperCase()
                : "UNKNOWN LANGUAGE";
        return (processedCount + 1) + "/" + subtitleToLoadCount + " getting subtitle "
                + language
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }
}
