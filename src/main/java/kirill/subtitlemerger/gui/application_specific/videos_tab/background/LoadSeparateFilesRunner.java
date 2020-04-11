package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.files.entities.FileInfo;
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
    public Result run(BackgroundRunnerManager runnerManager) {
        List<FileInfo> filesInfo = VideoTabBackgroundUtils.getFilesInfo(files, context.getFfprobe(), runnerManager);
        List<TableFileInfo> allTableFilesInfo = VideoTabBackgroundUtils.tableFilesInfoFrom(
                filesInfo,
                true,
                true,
                runnerManager,
                context.getSettings()
        );

        List<TableFileInfo> tableFilesToShowInfo = VideoTabBackgroundUtils.getSortedFilesInfo(
                allTableFilesInfo,
                sortBy,
                sortDirection,
                runnerManager
        );

        return new Result(
                filesInfo,
                allTableFilesInfo,
                new TableFilesToShowInfo(
                        tableFilesToShowInfo,
                        tableFilesToShowInfo.size(),
                        VideoTabBackgroundUtils.getSelectedAvailableCount(tableFilesToShowInfo, runnerManager),
                        VideoTabBackgroundUtils.getSelectedUnavailableCount(tableFilesToShowInfo, runnerManager)
                )
        );
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<FileInfo> filesInfo;

        private List<TableFileInfo> allTableFilesInfo;

        private TableFilesToShowInfo tableFilesToShowInfo;
    }
}
