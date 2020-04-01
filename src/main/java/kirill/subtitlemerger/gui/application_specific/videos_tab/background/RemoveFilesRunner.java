package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class RemoveFilesRunner implements BackgroundRunner<RemoveFilesRunner.Result> {
    private List<FileInfo> originalFilesInfo;

    private TableWithFiles.Mode mode;

    private List<TableFileInfo> originalAllTableFilesInfo;

    private List<TableFileInfo> originalTableFilesToShowInfo;

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        List<String> selectedFileIds =getSelectedFileIds(originalTableFilesToShowInfo, runnerManager);

        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("Removing files...");

        List<FileInfo> filesInfo = originalFilesInfo.stream()
                .filter(fileInfo -> !selectedFileIds.contains(fileInfo.getId()))
                .collect(Collectors.toList());
        List<TableFileInfo> allTableFilesInfo = originalAllTableFilesInfo.stream()
                .filter(fileInfo -> !selectedFileIds.contains(fileInfo.getId()))
                .collect(Collectors.toList());
        List<TableFileInfo> tableFilesToShowInfo = originalTableFilesToShowInfo.stream()
                .filter(fileInfo -> !selectedFileIds.contains(fileInfo.getId()))
                .collect(Collectors.toList());

        return new Result(
                originalFilesInfo.size() - filesInfo.size(),
                filesInfo,
                allTableFilesInfo,
                new TableFilesToShowInfo(
                        tableFilesToShowInfo,
                        VideoTabBackgroundUtils.getAllSelectableCount(tableFilesToShowInfo, mode, runnerManager),
                        VideoTabBackgroundUtils.getSelectedAvailableCount(tableFilesToShowInfo, runnerManager),
                        VideoTabBackgroundUtils.getSelectedUnavailableCount(tableFilesToShowInfo, runnerManager)
                )
        );
    }

    private static List<String> getSelectedFileIds(
            List<TableFileInfo> filesInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("Getting list of files to remove...");

        return filesInfo.stream()
                .filter(TableFileInfo::isSelected)
                .map(TableFileInfo::getId)
                .collect(Collectors.toList());
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private int removedCount;

        private List<FileInfo> filesInfo;

        private List<TableFileInfo> allTableFilesInfo;

        private TableFilesToShowInfo tableFilesToShowInfo;
    }
}
