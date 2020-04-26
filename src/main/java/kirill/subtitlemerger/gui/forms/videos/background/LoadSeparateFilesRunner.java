package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import kirill.subtitlemerger.logic.settings.SortBy;
import kirill.subtitlemerger.logic.settings.SortDirection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

@AllArgsConstructor
public class LoadSeparateFilesRunner implements BackgroundRunner<LoadSeparateFilesRunner.Result> {
    private List<File> files;

    private SortBy sortBy;

    private SortDirection sortDirection;

    private GuiContext context;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        List<VideoInfo> filesInfo = VideoTabBackgroundUtils.getFilesInfo(files, context.getFfprobe(), backgroundManager);
        List<TableFileInfo> allTableFilesInfo = VideoTabBackgroundUtils.tableFilesInfoFrom(
                filesInfo,
                true,
                true,
                backgroundManager,
                context.getSettings()
        );

        List<TableFileInfo> tableFilesToShowInfo = VideoTabBackgroundUtils.getSortedFilesInfo(
                allTableFilesInfo,
                sortBy,
                sortDirection,
                backgroundManager
        );

        return new Result(
                filesInfo,
                allTableFilesInfo,
                new TableFilesToShowInfo(
                        tableFilesToShowInfo,
                        tableFilesToShowInfo.size(),
                        VideoTabBackgroundUtils.getSelectedAvailableCount(tableFilesToShowInfo, backgroundManager),
                        VideoTabBackgroundUtils.getSelectedUnavailableCount(tableFilesToShowInfo, backgroundManager)
                )
        );
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<VideoInfo> filesInfo;

        private List<TableFileInfo> allTableFilesInfo;

        private TableFilesToShowInfo tableFilesToShowInfo;
    }
}
