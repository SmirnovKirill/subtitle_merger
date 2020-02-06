package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.load_subtitles;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.CancellableBackgroundTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStream;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public abstract class LoadSubtitlesTask extends CancellableBackgroundTask<Void> {
    private Ffmpeg ffmpeg;

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

    LoadSubtitlesTask(Ffmpeg ffmpeg, BooleanProperty cancelTaskPaneVisible) {
        super(cancelTaskPaneVisible);

        this.ffmpeg = ffmpeg;
    }

    protected void load(Integer ffmpegIndex, List<GuiFileInfo> guiFilesInfo, List<FileInfo> filesInfo) {
        if (ffmpegIndex != null && guiFilesInfo.size() != 1) {
            throw new IllegalArgumentException();
        }

        for (GuiFileInfo guiFileInfo : guiFilesInfo) {
            FileInfo fileInfo = RegularContentController.findMatchingFileInfo(guiFileInfo, filesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
                continue;
            }

            for (SubtitleStream subtitleStream : fileInfo.getSubtitleStreams()) {
                if (super.isCancelled()) {
                    setFinished(true);
                    return;
                }

                if (ffmpegIndex != null && ffmpegIndex != subtitleStream.getFfmpegIndex()) {
                    continue;
                }

                if (subtitleStream.getUnavailabilityReason() != null) {
                    continue;
                }

                if (subtitleStream.getSubtitles() != null) {
                    loadedBeforeCount++;
                    processedCount++;
                    continue;
                }

                updateMessage(
                        getUpdateMessage(
                                processedCount,
                                allSubtitleCount,
                                subtitleStream,
                                fileInfo.getFile()
                        )
                );

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

                    loadedSuccessfullyCount++;
                } catch (FfmpegException e) {
                    if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                        setFinished(true);
                        return;
                    } else {
                        Platform.runLater(() -> {
                            guiSubtitleStream.setFailedToLoadReason(guiTextFrom(e));
                        });
                        failedToLoadCount++;
                    }
                } catch (Parser.IncorrectFormatException e) {
                    Platform.runLater(() -> {
                        guiSubtitleStream.setFailedToLoadReason("subtitles seem to have incorrect format");
                    });

                    failedToLoadCount++;
                }

                processedCount++;
            }

            guiFileInfo.setHaveSubtitleSizesToLoad(RegularContentController.haveSubtitlesToLoad(fileInfo));
        }

        setFinished(true);
    }

    static String getUpdateMessage(
            int processedCount,
            int allSubtitleCount,
            SubtitleStream subtitleStream,
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

    public static String guiTextFrom(FfmpegException e) {
        if (e.getCode() == FfmpegException.Code.GENERAL_ERROR) {
            return "ffmpeg returned an error";
        }

        throw new IllegalStateException();
    }
}
