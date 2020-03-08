package kirill.subtitlemerger.gui.application_specific.videos_tab.loaders_and_handlers;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
        List<FileInfo> filesInfo = LoadDirectoryFilesTask.getFilesInfo(files, guiContext.getFfprobe(), runnerManager);
        List<GuiFileInfo> allGuiFilesInfo = LoadDirectoryFilesTask.convert(
                filesInfo,
                true,
                true,
                runnerManager,
                guiContext.getSettings()
        );

        boolean hideUnavailable = LoadDirectoryFilesTask.shouldHideUnavailable(filesInfo, runnerManager);
        List<GuiFileInfo> guiFilesToShowInfo = LoadDirectoryFilesTask.getFilesInfoToShow(
                allGuiFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                runnerManager
        );

        return new Result(filesInfo, allGuiFilesInfo, guiFilesToShowInfo, hideUnavailable);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<FileInfo> filesInfo;

        private List<GuiFileInfo> allGuiFilesInfo;

        private List<GuiFileInfo> guiFilesToShowInfo;

        private boolean hideUnavailable;
    }
}
