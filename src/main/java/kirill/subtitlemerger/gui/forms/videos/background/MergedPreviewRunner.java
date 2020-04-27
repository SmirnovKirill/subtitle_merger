package kirill.subtitlemerger.gui.forms.videos.background;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableVideoInfo;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableWithVideos;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.subtitles.SubtitleMerger;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndOutput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.MergedSubtitleInfo;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CommonsLog
@AllArgsConstructor
public class MergedPreviewRunner implements BackgroundRunner<MergedPreviewRunner.Result> {
    private SubtitleOption upperOption;

    private SubtitleOption lowerOption;

    private VideoInfo fileInfo;

    private TableVideoInfo tableFileInfo;

    private TableWithVideos tableWithFiles;

    private GuiContext context;

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
                        SubtitlesAndOutput.from(merged, context.getSettings().isPlainTextSubtitles()),
                        upperOption.getId(),
                        upperOption.getSubtitlesAndInput().getEncoding(),
                        lowerOption.getId(),
                        lowerOption.getSubtitlesAndInput().getEncoding()
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

        if (!Objects.equals(mergedSubtitleInfo.getUpperEncoding(), upperOption.getSubtitlesAndInput().getEncoding())) {
            return false;
        }

        if (!Objects.equals(mergedSubtitleInfo.getLowerOptionId(), lowerOption.getId())) {
            return false;
        }

        return Objects.equals(mergedSubtitleInfo.getLowerEncoding(), lowerOption.getSubtitlesAndInput().getEncoding());
    }

    private void loadStreams(BackgroundManager backgroundManager) throws InterruptedException {
        List<BuiltInSubtitleOption> streamsToLoad = new ArrayList<>();
        if (upperOption instanceof BuiltInSubtitleOption) {
            streamsToLoad.add((BuiltInSubtitleOption) upperOption);
        }
        if (lowerOption instanceof BuiltInSubtitleOption) {
            streamsToLoad.add((BuiltInSubtitleOption) lowerOption);
        }

        for (BuiltInSubtitleOption ffmpegStream : streamsToLoad) {
            backgroundManager.updateMessage(getUpdateMessage(ffmpegStream, fileInfo.getFile()));

            if (ffmpegStream.getSubtitles() != null) {
                continue;
            }

            TableSubtitleOption tableSubtitleOption = TableSubtitleOption.getById(
                    ffmpegStream.getId(),
                    tableFileInfo.getSubtitleOptions()
            );

            try {
                String subtitleText = context.getFfmpeg().getSubtitleText(
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

                    Platform.runLater(
                            () -> tableWithFiles.subtitlesLoadedSuccessfully(
                                    subtitlesAndInput.getSize(),
                                    tableSubtitleOption,
                                    tableFileInfo
                            )
                    );
                } else {
                    Platform.runLater(
                            () -> tableWithFiles.failedToLoadSubtitles(
                                    VideoBackgroundUtils.FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT,
                                    tableSubtitleOption
                            )
                    );
                }
            } catch (FfmpegException e) {
                log.warn("failed to get subtitle text: " + e.getCode() + ", console output " + e.getConsoleOutput());
                Platform.runLater(
                        () -> tableWithFiles.failedToLoadSubtitles(
                                VideoBackgroundUtils.failedToLoadReasonFrom(e.getCode()),
                                tableSubtitleOption
                        )
                );
            }
        }
    }

    private static String getUpdateMessage(BuiltInSubtitleOption subtitleStream, File file) {
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
