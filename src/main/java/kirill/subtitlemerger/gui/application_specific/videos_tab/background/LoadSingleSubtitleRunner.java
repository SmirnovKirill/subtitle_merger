package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.core.SubtitleParser;
import kirill.subtitlemerger.logic.work_with_files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class LoadSingleSubtitleRunner implements BackgroundRunner<LoadSingleSubtitleRunner.Result> {
    private FfmpegSubtitleStream ffmpegStream;

    private FileInfo fileInfo;

    private Ffmpeg ffmpeg;

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        runnerManager.setIndeterminateProgress();

        runnerManager.updateMessage(
                BackgroundHelperMethods.getLoadSubtitlesProgressMessage(
                        1,
                        0,
                        ffmpegStream,
                        fileInfo.getFile()
                )
        );
        runnerManager.setCancellationPossible(true);

        try {
            String subtitleText = ffmpeg.getSubtitleText(ffmpegStream.getFfmpegIndex(), fileInfo.getFile());
            ffmpegStream.setSubtitles(SubtitleParser.fromSubRipText(subtitleText, ffmpegStream.getLanguage()));

            return new Result(
                    false,
                    new TableWithFiles.LoadedSubtitles(
                            ffmpegStream.getSubtitles().getSize(),
                            null
                    )
            );
        } catch (FfmpegException e) {
            if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                return new Result(true, null);
            } else {
                return new Result(
                        false,
                        new TableWithFiles.LoadedSubtitles(
                                null,
                                TableSubtitleOption.FailedToLoadSubtitlesReason.FFMPEG_ERROR
                        )
                );
            }
        } catch (SubtitleParser.IncorrectFormatException e) {
            return new Result(
                    false,
                    new TableWithFiles.LoadedSubtitles(
                            null,
                            TableSubtitleOption.FailedToLoadSubtitlesReason.INCORRECT_FORMAT
                    )
            );
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private boolean cancelled;

        private TableWithFiles.LoadedSubtitles loadedSubtitles;
    }
}
