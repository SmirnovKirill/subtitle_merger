package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.TableWithFiles;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommonsLog
public class LoadDirectoryFilesTask extends BackgroundTask<LoadDirectoryFilesTask.Result> {
    private File directory;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private TableWithFiles.AllFileSubtitleSizesLoader allSizesLoader;

    private TableWithFiles.SingleFileSubtitleSizeLoader singleSizeLoader;

    private TableWithFiles.AddExternalSubtitleFileHandler addExternalSubtitleFileHandler;

    private TableWithFiles.RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler;

    private GuiContext guiContext;

    public LoadDirectoryFilesTask(
            File directory,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            TableWithFiles.AllFileSubtitleSizesLoader allSizesLoader,
            TableWithFiles.SingleFileSubtitleSizeLoader singleSizeLoader,
            TableWithFiles.AddExternalSubtitleFileHandler addExternalSubtitleFileHandler,
            TableWithFiles.RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler,
            GuiContext guiContext
    ) {
        super();
        this.directory = directory;
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
        List<File> files = getDirectoryFiles(directory, this);
        List<FileInfo> filesInfo = getFilesInfo(files, guiContext.getFfprobe(), this);
        List<GuiFileInfo> allGuiFilesInfo = convert(
                filesInfo,
                false,
                false,
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

    private static List<File> getDirectoryFiles(File directory, LoadDirectoryFilesTask task) {
        task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        task.updateMessage("getting file list...");

        File[] directoryFiles = directory.listFiles();

        if (directoryFiles == null) {
            log.warn("failed to get directory files, directory " + directory.getAbsolutePath());
            return new ArrayList<>();
        } else {
            return Arrays.asList(directoryFiles);
        }
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
