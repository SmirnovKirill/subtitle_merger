package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStreamInfo;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.core.Writer;
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
    private int allSubtitleCount;

    @Getter
    private int processedCount;

    @Getter
    private int loadedSuccessfullyCount;

    @Getter
    private int loadedBeforeCount;

    @Getter
    private int failedToLoadCount;

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
    protected Void call() {
        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        updateMessage("calculating number of subtitles to load...");

        initializeCounters();

        mainLoop: for (FileInfo fileInfo : filesInfo) {
            GuiFileInfo guiFileInfo = RegularContentController.findMatchingGuiFileInfo(fileInfo, guiFilesInfo);
            if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
                int index = 0;
                for (SubtitleStreamInfo subtitleStream : fileInfo.getSubtitleStreamsInfo()) {
                    if (isCancelled()) {
                        cancelled = true;
                        break mainLoop;
                    }

                    if (subtitleStream.getUnavailabilityReason() != null) {
                        index++;
                        continue;
                    }

                    if (subtitleIndex != null && subtitleIndex != subtitleStream.getIndex()) {
                        index++;
                        continue;
                    }

                    if (subtitleStream.getSubtitles() != null) {
                        processedCount++;
                        index++;
                        continue;
                    }

                    try {
                        updateMessage(
                                getUpdateMessage(
                                        processedCount,
                                        allSubtitleCount,
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

                        GuiSubtitleStreamInfo guiSubtitleStreamInfo = guiFileInfo.getSubtitleStreamsInfo().get(index);
                        int subtitleSize = Writer.toSubRipText(subtitleStream.getSubtitles()).getBytes().length;

                        /*
                         * Have to call this in the JavaFX thread because this change can lead to updates on the screen.
                         */
                        Platform.runLater(() -> guiSubtitleStreamInfo.setSize(subtitleSize));

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
                    index++;
                }
            }
        }

        return null;
    }

    private void initializeCounters() {
        for (FileInfo fileInfo : filesInfo) {
            if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
                for (SubtitleStreamInfo subtitleStream : fileInfo.getSubtitleStreamsInfo()) {
                    if (subtitleStream.getUnavailabilityReason() != null) {
                        continue;
                    }

                    if (subtitleIndex != null && subtitleIndex != subtitleStream.getIndex()) {
                        continue;
                    }

                    allSubtitleCount++;

                    if (subtitleStream.getSubtitles() != null) {
                        loadedBeforeCount++;
                    }
                }
            }
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
