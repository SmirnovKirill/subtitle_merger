package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableData;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableVideoInfo;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableWithVideos;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

@AllArgsConstructor
public class LoadSeparateFilesRunner implements BackgroundRunner<LoadSeparateFilesRunner.Result> {
    private List<File> files;

    private GuiContext context;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        List<VideoInfo> filesInfo = VideoBackgroundUtils.getVideosInfo(files, context.getFfprobe(), backgroundManager);
        List<TableVideoInfo> allTableFilesInfo = VideoBackgroundUtils.tableVideosInfoFrom(
                filesInfo,
                true,
                true,
                context.getSettings(),
                backgroundManager
        );

        List<TableVideoInfo> tableFilesToShowInfo = VideoBackgroundUtils.getSortedVideosInfo(
                allTableFilesInfo,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                backgroundManager
        );

        return new Result(
                filesInfo,
                allTableFilesInfo,
                VideoBackgroundUtils.getTableData(
                        TableWithVideos.Mode.SEPARATE_FILES,
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
        private List<VideoInfo> filesInfo;

        private List<TableVideoInfo> allTableFilesInfo;

        private TableData tableData;
    }
}
