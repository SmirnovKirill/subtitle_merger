package kirill.subtitlemerger.gui.application_specific.videos_tab.background_tasks;

import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.utils.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RemoveFilesTask extends BackgroundTask<RemoveFilesTask.Result> {
    private List<FileInfo> filesInfo;

    private List<GuiFileInfo> allGuiFilesInfo;

    private List<GuiFileInfo> displayedGuiFilesInfo;

    private Consumer<Result> onFinish;

    public RemoveFilesTask(
            List<FileInfo> filesInfo,
            List<GuiFileInfo> allGuiFilesInfo,
            List<GuiFileInfo> displayedGuiFilesInfo,
            Consumer<Result> onFinish
    ) {
        this.filesInfo = filesInfo;
        this.allGuiFilesInfo = allGuiFilesInfo;
        this.displayedGuiFilesInfo = new ArrayList<>(displayedGuiFilesInfo);
        this.onFinish = onFinish;
    }

    @Override
    protected Result run() {
        List<String> selectedPaths = getPathsOfFilesToRemove(displayedGuiFilesInfo, this);
        int originalSize = filesInfo.size();

        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        updateMessage("removing files...");

        filesInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getFile().getAbsolutePath()));
        allGuiFilesInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getFullPath()));
        displayedGuiFilesInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getFullPath()));

        return new Result(
                originalSize - filesInfo.size(),
                filesInfo,
                allGuiFilesInfo,
                displayedGuiFilesInfo
        );
    }

    private static List<String> getPathsOfFilesToRemove(
            List<GuiFileInfo> displayedGuiFilesInfo,
            RemoveFilesTask task
    ) {
        task.updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);
        task.updateMessage("getting list of files to remove...");

        return displayedGuiFilesInfo.stream()
                .filter(GuiFileInfo::isSelected)
                .map(GuiFileInfo::getFullPath)
                .collect(Collectors.toList());
    }

    @Override
    protected void onFinish(Result result) {
        onFinish.accept(result);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private int removedCount;

        private List<FileInfo> filesInfo;

        private List<GuiFileInfo> allGuiFilesInfo;

        private List<GuiFileInfo> guiFilesToShowInfo;
    }
}
