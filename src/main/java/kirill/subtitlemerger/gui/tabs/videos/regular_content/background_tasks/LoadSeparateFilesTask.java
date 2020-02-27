package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.utils.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class LoadSeparateFilesTask extends BackgroundTask<LoadSeparateFilesTask.Result> {
    private List<File> files;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext guiContext;

    private Consumer<Result> onFinish;

    public LoadSeparateFilesTask(
            List<File> files,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            GuiContext guiContext,
            Consumer<Result> onFinish
    ) {
        this.files = files;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.guiContext = guiContext;
        this.onFinish = onFinish;
    }

    @Override
    protected Result run() {
        List<FileInfo> filesInfo = LoadDirectoryFilesTask.getFilesInfo(files, guiContext.getFfprobe(), this);
        List<GuiFileInfo> allGuiFilesInfo = LoadDirectoryFilesTask.convert(
                filesInfo,
                true,
                true,
                this,
                guiContext.getSettings()
        );

        boolean hideUnavailable = LoadDirectoryFilesTask.shouldHideUnavailable(filesInfo, this);
        List<GuiFileInfo> guiFilesToShowInfo = LoadDirectoryFilesTask.getFilesInfoToShow(
                allGuiFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                this
        );

        return new Result(filesInfo, allGuiFilesInfo, guiFilesToShowInfo, hideUnavailable);
    }

    @Override
    protected void onFinish(Result result) {
        onFinish.accept(result);
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
