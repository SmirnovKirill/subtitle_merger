package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.load_subtitles;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStreamInfo;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStreamInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public abstract class LoadSubtitlesTask extends BackgroundTask<Void> {
    private Ffmpeg ffmpeg;

    @Getter
    protected boolean cancelled;

    @Getter
    protected int allSubtitleCount;

    @Getter
    protected int loadedBeforeCount;

    @Getter
    protected int processedCount;

    @Getter
    protected int loadedSuccessfullyCount;

    @Getter
    protected int failedToLoadCount;

    LoadSubtitlesTask(
            Ffmpeg ffmpeg
    ) {
        super();

        this.ffmpeg = ffmpeg;
    }

    protected void load(Integer subtitleId, List<GuiFileInfo> guiFilesInfo, List<FileInfo> filesInfo) {
        if (subtitleId != null && guiFilesInfo.size() != 1) {
            throw new IllegalArgumentException();
        }

        mainLoop: for (GuiFileInfo guiFileInfo : guiFilesInfo) {
            FileInfo fileInfo = RegularContentController.findMatchingFileInfo(guiFileInfo, filesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
                continue;
            }

            for (SubtitleStreamInfo subtitleStream : fileInfo.getSubtitleStreamsInfo()) {
                if (super.isCancelled()) {
                    cancelled = true;
                    break mainLoop;
                }

                if (subtitleId != null && subtitleId != subtitleStream.getId()) {
                    continue;
                }

                if (subtitleStream.getUnavailabilityReason() != null) {
                    continue;
                }

                if (subtitleStream.getSubtitles() != null) {
                    processedCount++;
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

                    String subtitleText = ffmpeg.getSubtitlesText(subtitleStream.getId(), fileInfo.getFile());
                    subtitleStream.setSubtitlesAndSize(
                            Parser.fromSubRipText(
                                    subtitleText,
                                    subtitleStream.getTitle(),
                                    subtitleStream.getLanguage()
                            )
                    );

                    GuiSubtitleStreamInfo guiSubtitleStreamInfo = RegularContentController.findMatchingGuiStreamInfo(
                            subtitleStream.getId(),
                            guiFileInfo.getSubtitleStreamsInfo()
                    );

                    /*
                     * Have to call this in the JavaFX thread because this change can lead to updates on the screen.
                     */
                    Platform.runLater(() -> guiSubtitleStreamInfo.setSize(subtitleStream.getSubtitleSize()));

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

            guiFileInfo.setHaveSubtitleSizesToLoad(RegularContentController.haveSubtitlesToLoad(fileInfo));
        }
    }

    private static String getUpdateMessage(
            int processedCount,
            int allSubtitleCount,
            SubtitleStreamInfo subtitleStream,
            File file
    ) {
        String language = subtitleStream.getLanguage() != null
                ? subtitleStream.getLanguage().toString().toUpperCase()
                : "UNKNOWN LANGUAGE";

        String progressPrefix = allSubtitleCount > 1
                ? (processedCount + 1) + "/" + allSubtitleCount + " "
                : "";

        return progressPrefix + "getting subtitle "
                + language
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }
}
