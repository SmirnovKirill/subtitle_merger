package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class AddFilesTask extends BackgroundTask<AddFilesTask.Result> {
    private List<FileInfo> filesInfo;

    private List<File> filesToAdd;

    private List<GuiFileInfo> allGuiFilesInfo;

    private boolean hideUnavailable;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext guiContext;

    public AddFilesTask(
            List<FileInfo> filesInfo,
            List<File> filesToAdd,
            List<GuiFileInfo> allGuiFilesInfo,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            GuiContext guiContext
    ) {
        super();
        this.filesInfo = filesInfo;
        this.filesToAdd = filesToAdd;
        this.allGuiFilesInfo = allGuiFilesInfo;
        this.hideUnavailable = hideUnavailable;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.guiContext = guiContext;
    }

    @Override
    protected Result call() {
        List<FileInfo> filesToAddInfo = getFilesInfo(filesToAdd, guiContext.getFfprobe(), this);
        removeAlreadyAdded(filesToAddInfo, filesInfo);
        List<GuiFileInfo> guiFilesToAddInfo = convert(
                filesToAddInfo,
                true,
                true,
                this,
                guiContext.getSettings()
        );
        filesInfo.addAll(filesToAddInfo);
        allGuiFilesInfo.addAll(guiFilesToAddInfo);

        List<GuiFileInfo> guiFilesToShowInfo = getFilesInfoToShow(
                allGuiFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                this
        );

        return new Result(filesInfo, allGuiFilesInfo, guiFilesToShowInfo);
    }

    private static void removeAlreadyAdded(List<FileInfo> filesToAddInfo, List<FileInfo> allFilesInfo) {
        Iterator<FileInfo> iterator = filesToAddInfo.iterator();

        while (iterator.hasNext()) {
            FileInfo fileToAddInfo = iterator.next();

            boolean alreadyAdded = allFilesInfo.stream()
                    .anyMatch(fileInfo -> Objects.equals(fileInfo.getFile(), fileToAddInfo.getFile()));
            if (alreadyAdded) {
                iterator.remove();
            }
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<FileInfo> filesInfo;

        private List<GuiFileInfo> allGuiFilesInfo;

        private List<GuiFileInfo> guiFilesToShowInfo;
    }
}