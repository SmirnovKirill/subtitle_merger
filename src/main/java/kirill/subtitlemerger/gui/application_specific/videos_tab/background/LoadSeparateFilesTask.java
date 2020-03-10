package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public class LoadSeparateFilesTask implements BackgroundRunner<LoadSeparateFilesTask.Result> {
    private List<File> files;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext guiContext;

    public LoadSeparateFilesTask(
            List<File> files,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            GuiContext guiContext
    ) {
        this.files = files;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.guiContext = guiContext;
    }

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        List<FileInfo> filesInfo = BackgroundHelperMethods.getFilesInfo(files, guiContext.getFfprobe(), runnerManager);
        List<TableFileInfo> allTableFilesInfo = BackgroundHelperMethods.tableFilesInfoFrom(
                filesInfo,
                true,
                true,
                runnerManager,
                guiContext.getSettings()
        );

        List<TableFileInfo> tableFilesToShowInfo = BackgroundHelperMethods.getFilesToShowInfo(
                allTableFilesInfo,
                false,
                sortBy,
                sortDirection,
                runnerManager
        );

        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("calculating number of files...");

        int selectedAvailableCount = 0;
        int selectedUnavailableCount = 0;

        for (TableFileInfo tableFileInfoToShow : tableFilesToShowInfo) {
            if (StringUtils.isBlank(tableFileInfoToShow.getUnavailabilityReason())) {
                selectedAvailableCount++;
            } else {
                selectedUnavailableCount++;
            }
        }

        return new Result(
                filesInfo,
                allTableFilesInfo,
                tableFilesToShowInfo,
                selectedAvailableCount,
                selectedUnavailableCount
        );
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<FileInfo> filesInfo;

        private List<TableFileInfo> allTableFilesInfo;

        private List<TableFileInfo> tableFilesToShowInfo;

        private int selectedAvailableCount;

        private int selectedUnavailableCount;
    }
}
