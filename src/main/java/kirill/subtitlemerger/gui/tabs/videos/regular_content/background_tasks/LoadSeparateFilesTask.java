package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.TableWithFiles;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

public class LoadSeparateFilesTask extends BackgroundTask<LoadSeparateFilesTask.Result> {
    private List<File> files;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private TableWithFiles.AllFileSubtitleSizesLoader allSizesLoader;

    private TableWithFiles.SingleFileSubtitleSizeLoader singleSizeLoader;

    private TableWithFiles.AddExternalSubtitleFileHandler addExternalSubtitleFileHandler;

    private TableWithFiles.RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler;

    private GuiContext guiContext;

    public LoadSeparateFilesTask(
            List<File> files,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            TableWithFiles.AllFileSubtitleSizesLoader allSizesLoader,
            TableWithFiles.SingleFileSubtitleSizeLoader singleSizeLoader,
            TableWithFiles.AddExternalSubtitleFileHandler addExternalSubtitleFileHandler,
            TableWithFiles.RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler,
            GuiContext guiContext
    ) {
        super();
        this.files = files;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.allSizesLoader = allSizesLoader;
        this.singleSizeLoader = singleSizeLoader;
        this.addExternalSubtitleFileHandler = addExternalSubtitleFileHandler;
        this.removeExternalSubtitleFileHandler = removeExternalSubtitleFileHandler;
        this.guiContext = guiContext;
    }

    @Override
    protected Result call() {
        List<FileInfo> filesInfo = getFilesInfo(files, guiContext.getFfprobe(), this);
        List<GuiFileInfo> allGuiFilesInfo = convert(
                filesInfo,
                true,
                true,
                this,
                allSizesLoader,
                singleSizeLoader,
                addExternalSubtitleFileHandler,
                removeExternalSubtitleFileHandler,
                guiContext.getSettings()
        );

        boolean hideUnavailable = shouldHideUnavailable(filesInfo, this);
        List<GuiFileInfo> guiFilesToShowInfo = getFilesInfoToShow(
                allGuiFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                this
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
