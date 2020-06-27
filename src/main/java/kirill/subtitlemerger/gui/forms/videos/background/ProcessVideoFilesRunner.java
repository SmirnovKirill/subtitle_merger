package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.forms.videos.table.TableData;
import kirill.subtitlemerger.gui.forms.videos.table.TableMode;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.forms.videos.table.TableWithVideos;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.getSortedVideos;
import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.getTableData;

@AllArgsConstructor
public class ProcessVideoFilesRunner implements BackgroundRunner<ProcessVideoFilesRunner.Result> {
    private List<File> videoFiles;

    private TableWithVideos table;

    private Ffprobe ffprobe;

    private Settings settings;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        List<Video> allVideos = VideosBackgroundUtils.getVideos(videoFiles, ffprobe, backgroundManager);
        List<TableVideo> allTableVideos = VideosBackgroundUtils.tableVideosFrom(
                allVideos,
                true,
                true,
                table,
                settings,
                backgroundManager
        );
        allTableVideos = getSortedVideos(allTableVideos, settings.getSort(), backgroundManager);

        return new Result(
                allVideos,
                allTableVideos,
                getTableData(
                        allTableVideos,
                        false,
                        TableMode.SEPARATE_VIDEOS,
                        settings.getSort(),
                        backgroundManager
                )
        );
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<Video> allVideos;

        private List<TableVideo> allTableVideos;

        private TableData tableData;
    }
}
