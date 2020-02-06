package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.FilePanes;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoveFilesTask extends BackgroundTask<RemoveFilesTask.Result> {
    private List<FileInfo> filesInfo;

    private List<GuiFileInfo> allGuiFilesInfo;

    private List<GuiFileInfo> guiFilesToShowInfo;

    private Map<String, FilePanes> filePanes;

    private List<Integer> selectedIndices;

    public RemoveFilesTask(
            List<FileInfo> filesInfo,
            List<GuiFileInfo> allGuiFilesInfo,
            List<GuiFileInfo> currentGuiFilesToShowInfo,
            Map<String, FilePanes> filePanes,
            List<Integer> selectedIndices,
            BooleanProperty cancelTaskPaneVisible
    ) {
        super(cancelTaskPaneVisible);
        this.filesInfo = filesInfo;
        this.allGuiFilesInfo = allGuiFilesInfo;
        this.guiFilesToShowInfo = new ArrayList<>(currentGuiFilesToShowInfo);
        this.filePanes = filePanes;
        this.selectedIndices = selectedIndices;
    }

    @Override
    protected Result call() {
        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        updateMessage("removing files...");

        List<String> selectedPaths = new ArrayList<>();
        for (int index : selectedIndices) {
            selectedPaths.add(guiFilesToShowInfo.get(index).getFullPath());
        }

        int originalSize = filesInfo.size();

        filesInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getFile().getAbsolutePath()));
        allGuiFilesInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getFullPath()));
        guiFilesToShowInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getFullPath()));
        filePanes.entrySet().removeIf(entry -> selectedPaths.contains(entry.getKey()));

        return new Result(originalSize - filesInfo.size(), filesInfo, allGuiFilesInfo, guiFilesToShowInfo, filePanes);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private int removedCount;

        private List<FileInfo> filesInfo;

        private List<GuiFileInfo> allGuiFilesInfo;

        private List<GuiFileInfo> guiFilesToShowInfo;

        private Map<String, FilePanes> filePanes;
    }
}
