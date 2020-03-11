package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

@AllArgsConstructor
public class LoadSeparateFilesRunner implements BackgroundRunner<LoadSeparateFilesRunner.Result> {
    private List<File> files;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext context;

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        List<FileInfo> filesInfo = BackgroundHelperMethods.getFilesInfo(files, context.getFfprobe(), runnerManager);
        List<TableFileInfo> allTableFilesInfo = BackgroundHelperMethods.tableFilesInfoFrom(
                filesInfo,
                true,
                true,
                runnerManager,
                context.getSettings()
        );

        List<TableFileInfo> tableFilesToShowInfo = BackgroundHelperMethods.getSortedFilesInfo(
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
                        BackgroundHelperMethods.getSelectedAvailableCount(tableFilesToShowInfo, runnerManager),
                        BackgroundHelperMethods.getSelectedUnavailableCount(tableFilesToShowInfo, runnerManager)
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
