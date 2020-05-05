package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.forms.videos.table.TableData;
import kirill.subtitlemerger.gui.forms.videos.table.TableMode;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.forms.videos.table.TableWithVideos;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

@AllArgsConstructor
public class LoadSeparateFilesRunner implements BackgroundRunner<LoadSeparateFilesRunner.Result> {
    private List<File> files;

    private TableWithVideos table;

    private GuiContext context;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        List<Video> filesInfo = VideosBackgroundUtils.getVideos(files, context.getFfprobe(), backgroundManager);
        List<TableVideo> allTableFilesInfo = VideosBackgroundUtils.tableVideosFrom(
                filesInfo,
                true,
                true,
                table,
                context.getSettings(),
                backgroundManager
        );

        List<TableVideo> tableFilesToShowInfo = VideosBackgroundUtils.getSortedVideos(
                allTableFilesInfo,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                backgroundManager
        );

        return new Result(
                filesInfo,
                allTableFilesInfo,
                VideosBackgroundUtils.getTableData(
                        TableMode.SEPARATE_VIDEOS,
                        tableFilesToShowInfo,
                        context.getSettings().getSortBy(),
                        context.getSettings().getSortDirection(),
                        backgroundManager
                )
        );
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<Video> filesInfo;

        private List<TableVideo> allTableFilesInfo;

        private TableData tableData;
    }
}
