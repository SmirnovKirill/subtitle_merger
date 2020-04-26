package kirill.subtitlemerger.gui.forms.videos.background;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.core.SubRipParser;
import kirill.subtitlemerger.logic.core.SubtitleMerger;
import kirill.subtitlemerger.logic.core.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.video_files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.video_files.entities.VideoFile;
import kirill.subtitlemerger.logic.video_files.entities.MergedSubtitleInfo;
import kirill.subtitlemerger.logic.video_files.entities.SubtitleOption;
import kirill.subtitlemerger.logic.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CommonsLog
@AllArgsConstructor
public class MergedPreviewRunner implements BackgroundRunner<MergedPreviewRunner.Result> {
    private SubtitleOption upperOption;

    private SubtitleOption lowerOption;

    private VideoFile fileInfo;

    private TableFileInfo tableFileInfo;

    private TableWithFiles tableWithFiles;

    private Ffmpeg ffmpeg;

    public Result run(BackgroundManager backgroundManager) {
        if (fileInfo.getMergedSubtitleInfo() != null) {
            if (mergedMatchesCurrentSelection(fileInfo.getMergedSubtitleInfo(), upperOption, lowerOption)) {
                return new Result(false, fileInfo.getMergedSubtitleInfo());
            }
        }

        backgroundManager.setCancellationPossible(true);

        try {
            loadStreams(backgroundManager);
        } catch (InterruptedException e) {
            return new Result(true, null);
        }

        backgroundManager.updateMessage("Merging subtitles...");

        Subtitles merged;
        try {
            merged = SubtitleMerger.mergeSubtitles(upperOption.getSubtitles(), lowerOption.getSubtitles());
        } catch (InterruptedException e) {
            return new Result(true, null);
        }

        return new Result(
                false,
                new MergedSubtitleInfo(
                        merged,
                        upperOption.getId(),
                        upperOption.getEncoding(),
                        lowerOption.getId(),
                        lowerOption.getEncoding()
                )
        );
    }

    private static boolean mergedMatchesCurrentSelection(
            MergedSubtitleInfo mergedSubtitleInfo,
            SubtitleOption upperOption,
            SubtitleOption lowerOption
    ) {
        if (!Objects.equals(mergedSubtitleInfo.getUpperOptionId(), upperOption.getId())) {
            return false;
        }

        if (!Objects.equals(mergedSubtitleInfo.getUpperEncoding(), upperOption.getEncoding())) {
            return false;
        }

        if (!Objects.equals(mergedSubtitleInfo.getLowerOptionId(), lowerOption.getId())) {
            return false;
        }

        if (!Objects.equals(mergedSubtitleInfo.getLowerEncoding(), lowerOption.getEncoding())) {
            return false;
        }

        return true;
    }

    private void loadStreams(BackgroundManager backgroundManager) throws InterruptedException {
        List<FfmpegSubtitleStream> streamsToLoad = new ArrayList<>();
        if (upperOption instanceof FfmpegSubtitleStream) {
            streamsToLoad.add((FfmpegSubtitleStream) upperOption);
        }
        if (lowerOption instanceof FfmpegSubtitleStream) {
            streamsToLoad.add((FfmpegSubtitleStream) lowerOption);
        }

        for (FfmpegSubtitleStream ffmpegStream : streamsToLoad) {
            backgroundManager.updateMessage(getUpdateMessage(ffmpegStream, fileInfo.getFile()));

            if (ffmpegStream.getSubtitles() != null) {
                continue;
            }

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
                ffmpegStream.setSubtitlesAndSize(SubRipParser.from(subtitleText), subtitleText.getBytes().length);

                Platform.runLater(
                        () -> tableWithFiles.subtitlesLoadedSuccessfully(
                                ffmpegStream.getSize(),
                                tableSubtitleOption,
                                tableFileInfo
                        )
                );
            } catch (FfmpegException e) {
                log.warn("failed to get subtitle text: " + e.getCode() + ", console output " + e.getConsoleOutput());
                Platform.runLater(
                        () -> tableWithFiles.failedToLoadSubtitles(
                                VideoTabBackgroundUtils.failedToLoadReasonFrom(e.getCode()),
                                tableSubtitleOption
                        )
                );
            } catch (SubtitleFormatException e) {
                Platform.runLater(
                        () -> tableWithFiles.failedToLoadSubtitles(
                                VideoTabBackgroundUtils.FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT,
                                tableSubtitleOption
                        )
                );
            }
        }
    }

    private static String getUpdateMessage(FfmpegSubtitleStream subtitleStream, File file) {
        return "Getting subtitles "
                + Utils.languageToString(subtitleStream.getLanguage()).toUpperCase()
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private boolean canceled;

        private MergedSubtitleInfo mergedSubtitleInfo;
    }
 }
