package kirill.subtitlemerger.gui.application_specific.videos_tab.background_tasks;

import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.utils.entities.MultiPartResult;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class AddFilesTask implements BackgroundRunner<AddFilesTask.Result> {
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
        this.filesInfo = filesInfo;
        this.filesToAdd = filesToAdd;
        this.allGuiFilesInfo = allGuiFilesInfo;
        this.hideUnavailable = hideUnavailable;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.guiContext = guiContext;
    }

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        List<FileInfo> filesToAddInfo = LoadDirectoryFilesTask.getFilesInfo(filesToAdd, guiContext.getFfprobe(), runnerManager);
        int allFilesToAdd = filesToAddInfo.size();
        removeAlreadyAdded(filesToAddInfo, filesInfo, runnerManager);
        List<GuiFileInfo> guiFilesToAddInfo = LoadDirectoryFilesTask.convert(
                filesToAddInfo,
                true,
                true,
                runnerManager,
                guiContext.getSettings()
        );
        filesInfo.addAll(filesToAddInfo);
        allGuiFilesInfo.addAll(guiFilesToAddInfo);

        List<GuiFileInfo> guiFilesToShowInfo = LoadDirectoryFilesTask.getFilesInfoToShow(
                allGuiFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                runnerManager
        );

        return new Result(allFilesToAdd, filesToAddInfo.size(), filesInfo, allGuiFilesInfo, guiFilesToShowInfo);
    }

    private static void removeAlreadyAdded(
            List<FileInfo> filesToAddInfo,
            List<FileInfo> allFilesInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        runnerManager.updateMessage("removing already added files...");

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

    public static MultiPartResult generateMultiPartResult(Result taskResult) {
        String success;

        int filesToAdd = taskResult.getFilesToAddCount();
        int actuallyAdded = taskResult.getActuallyAddedCount();

        if (actuallyAdded == 0) {
            success = GuiUtils.getTextDependingOnTheCount(
                    filesToAdd,
                    "File has been added already",
                    "All %d files have been added already"
            );
        } else if (filesToAdd == actuallyAdded) {
            success = GuiUtils.getTextDependingOnTheCount(
                    actuallyAdded,
                    "File has been added successfully",
                    "All %d files have been added successfully"
            );
        } else {
            success = GuiUtils.getTextDependingOnTheCount(
                    actuallyAdded,
                    String.format("1/%d files has been added successfully, ", filesToAdd),
                    String.format("%%d/%d files have been added successfully, ", filesToAdd)
            );
            success += (filesToAdd - actuallyAdded) + "/" + filesToAdd + " added before";
        }

        return new MultiPartResult(success, null, null);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private int filesToAddCount;

        private int actuallyAddedCount;

        private List<FileInfo> filesInfo;

        private List<GuiFileInfo> allGuiFilesInfo;

        private List<GuiFileInfo> guiFilesToShowInfo;
    }
}
