package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.FilePanes;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;
import java.util.Map;

public class LoadSeparateFilesTask extends BackgroundTask<LoadSeparateFilesTask.Result> {
    private List<File> files;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext guiContext;

    private LongProperty selected;

    private BooleanProperty allSelected;

    private IntegerProperty allAvailableCount;

    private FilePanes.AllFileSubtitleSizesLoader allFileSubtitleSizesLoader;

    private FilePanes.SingleFileSubtitleSizeLoader singleFileSubtitleSizeLoader;

    private FilePanes.AddExternalSubtitleFileHandler addExternalSubtitleFileHandler;

    private FilePanes.RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler;

    public LoadSeparateFilesTask(
            List<File> files,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            GuiContext guiContext,
            LongProperty selected,
            BooleanProperty allSelected,
            IntegerProperty allAvailableCount,
            FilePanes.AllFileSubtitleSizesLoader allFileSubtitleSizesLoader,
            FilePanes.SingleFileSubtitleSizeLoader singleFileSubtitleSizeLoader,
            FilePanes.AddExternalSubtitleFileHandler addExternalSubtitleFileHandler,
            FilePanes.RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler,
            BooleanProperty cancelTaskPaneVisible
    ) {
        super(cancelTaskPaneVisible);
        this.files = files;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.guiContext = guiContext;
        this.selected = selected;
        this.allSelected = allSelected;
        this.allAvailableCount = allAvailableCount;
        this.allFileSubtitleSizesLoader = allFileSubtitleSizesLoader;
        this.singleFileSubtitleSizeLoader = singleFileSubtitleSizeLoader;
        this.addExternalSubtitleFileHandler = addExternalSubtitleFileHandler;
        this.removeExternalSubtitleFileHandler = removeExternalSubtitleFileHandler;
    }

    @Override
    protected Result call() {
        List<FileInfo> filesInfo = getFilesInfo(files, guiContext.getFfprobe(), this);
        List<GuiFileInfo> allGuiFilesInfo = convert(
                filesInfo,
                true,
                true,
                this,
                guiContext.getSettings()
        );
        Map<String, FilePanes> filePanes = generateFilesPanes(
                allGuiFilesInfo,
                selected,
                allSelected,
                allAvailableCount,
                allFileSubtitleSizesLoader,
                singleFileSubtitleSizeLoader,
                addExternalSubtitleFileHandler,
                removeExternalSubtitleFileHandler,
                this
        );

        boolean hideUnavailable = shouldHideUnavailable(filesInfo, this);
        List<GuiFileInfo> guiFilesToShowInfo = getFilesInfoToShow(
                allGuiFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                this
        );

        return new Result(filesInfo, allGuiFilesInfo, guiFilesToShowInfo, hideUnavailable, filePanes);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<FileInfo> filesInfo;

        private List<GuiFileInfo> allGuiFilesInfo;

        private List<GuiFileInfo> guiFilesToShowInfo;

        private boolean hideUnavailable;

        private Map<String, FilePanes> filePanes;
    }
}
