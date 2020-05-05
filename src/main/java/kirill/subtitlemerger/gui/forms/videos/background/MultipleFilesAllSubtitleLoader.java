package kirill.subtitlemerger.gui.forms.videos.background;


import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.FAILED_TO_LOAD_INCORRECT_FORMAT;
import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.failedToLoadReasonFrom;

@CommonsLog
@AllArgsConstructor
public class MultipleFilesAllSubtitleLoader implements BackgroundRunner<ActionResult> {
    private List<TableVideo> displayedTableFilesInfo;

    private List<Video> filesInfo;

    private Ffmpeg ffmpeg;

    @Override
    public ActionResult run(BackgroundManager backgroundManager) {
        VideosBackgroundUtils.clearActionResults(displayedTableFilesInfo, backgroundManager);

        List<TableVideo> selectedTableFilesInfo = VideosBackgroundUtils.getSelectedVideos(
                displayedTableFilesInfo,
                backgroundManager
        );

        int streamToLoadCount = getStreamToLoadCount(selectedTableFilesInfo, filesInfo, backgroundManager);
        int processedCount = 0;
        int loadedSuccessfullyCount = 0;
        int failedToLoadCount = 0;

        backgroundManager.setIndeterminateProgress();
        backgroundManager.setCancellationDescription("Please be patient, this may take a while depending on the size.");
        backgroundManager.setCancellationPossible(true);

        mainLoop: for (TableVideo tableFileInfo : selectedTableFilesInfo) {
            Video fileInfo = Video.getById(tableFileInfo.getId(), filesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getBuiltInSubtitleOptions())) {
                continue;
            }

            int failedToLoadForFile = 0;

            for (BuiltInSubtitleOption ffmpegStream : fileInfo.getBuiltInSubtitleOptions()) {
                if (ffmpegStream.getNotValidReason() != null || ffmpegStream.getSubtitles() != null) {
                    continue;
                }

                backgroundManager.updateMessage(
                        VideosBackgroundUtils.getLoadSubtitlesProgressMessage(
                                processedCount,
                                streamToLoadCount,
                                ffmpegStream,
                                fileInfo.getFile()
                        )
                );

                TableSubtitleOption tableSubtitleOption = TableSubtitleOption.getById(
                        ffmpegStream.getId(),
                        tableFileInfo.getSubtitleOptions()
                );

                try {
                    String subtitleText = ffmpeg.getSubtitleText(
                            ffmpegStream.getFfmpegIndex(),
                            ffmpegStream.getFormat(),
                            fileInfo.getFile()
                    );
                    SubtitlesAndInput subtitlesAndInput = SubtitlesAndInput.from(
                            subtitleText.getBytes(),
                            StandardCharsets.UTF_8
                    );

                    if (subtitlesAndInput.isCorrectFormat()) {
                        ffmpegStream.setSubtitlesAndInput(subtitlesAndInput);
                        Platform.runLater(() -> tableSubtitleOption.loadedSuccessfully(subtitlesAndInput.getSize()));
                        loadedSuccessfullyCount++;
                    } else {
                        Platform.runLater(() -> tableSubtitleOption.failedToLoad(FAILED_TO_LOAD_INCORRECT_FORMAT));
                        failedToLoadCount++;
                        failedToLoadForFile++;
                    }
                } catch (FfmpegException e) {
                    log.warn(
                            "failed to get subtitle text: " + e.getCode() + ", console output " + e.getConsoleOutput()
                    );
                    Platform.runLater(() -> tableSubtitleOption.failedToLoad(failedToLoadReasonFrom(e.getCode())));
                    failedToLoadCount++;
                    failedToLoadForFile++;
                } catch (InterruptedException e) {
                    setFileInfoError(failedToLoadForFile, tableFileInfo);
                    break mainLoop;
                }

                processedCount++;
            }

            setFileInfoError(failedToLoadForFile, tableFileInfo);
        }

        return VideosBackgroundUtils.getSubtitleLoadingActionResult(
                streamToLoadCount,
                processedCount,
                loadedSuccessfullyCount,
                failedToLoadCount
        );
    }

    private static int getStreamToLoadCount(
            List<TableVideo> selectedTableFilesInfo,
            List<Video> allFilesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Calculating number of subtitles to load...");

        int result = 0;

        for (TableVideo tableFileInfo : selectedTableFilesInfo) {
            Video fileInfo = Video.getById(tableFileInfo.getId(), allFilesInfo);
            if (!CollectionUtils.isEmpty(fileInfo.getBuiltInSubtitleOptions())) {
                for (BuiltInSubtitleOption stream : fileInfo.getBuiltInSubtitleOptions()) {
                    if (stream.getNotValidReason() != null || stream.getSubtitles() != null) {
                        continue;
                    }

                    result++;
                }
            }
        }

        return result;
    }

    private static void setFileInfoError(int failedToLoadForFile, TableVideo fileInfo) {
        if (failedToLoadForFile == 0) {
            return;
        }

        String message = Utils.getTextDependingOnCount(
                failedToLoadForFile,
                "Failed to load subtitles",
                "Failed to load %d subtitles"
        );

        Platform.runLater(() -> fileInfo.setOnlyError(message));
    }
}
