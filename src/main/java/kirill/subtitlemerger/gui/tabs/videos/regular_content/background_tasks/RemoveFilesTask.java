package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class RemoveFilesTask extends BackgroundTask<RemoveFilesTask.Result> {
    private List<FileInfo> filesInfo;

    private List<GuiFileInfo> allGuiFilesInfo;

    private List<GuiFileInfo> guiFilesToShowInfo;

    private List<Integer> selectedIndices;

    public RemoveFilesTask(
            List<FileInfo> filesInfo,
            List<GuiFileInfo> allGuiFilesInfo,
            List<GuiFileInfo> guiFilesToShowInfo,
            List<Integer> selectedIndices
    ) {
        super();
        this.filesInfo = filesInfo;
        this.allGuiFilesInfo = allGuiFilesInfo;
        this.guiFilesToShowInfo = guiFilesToShowInfo;
        this.selectedIndices = selectedIndices;
    }

    @Override
    protected Result call() {
        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        updateMessage("removing files...");

        List<String> selectedPaths = new ArrayList<>();
        for (int index : selectedIndices) {
            selectedPaths.add(guiFilesToShowInfo.get(index).getPath());
        }

        filesInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getFile().getAbsolutePath()));
        allGuiFilesInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getPath()));
        guiFilesToShowInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getPath()));

        return new Result(filesInfo, allGuiFilesInfo, guiFilesToShowInfo);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<FileInfo> filesInfo;

        private List<GuiFileInfo> allGuiFilesInfo;

        private List<GuiFileInfo> guiFilesToShowInfo;
    }
}
