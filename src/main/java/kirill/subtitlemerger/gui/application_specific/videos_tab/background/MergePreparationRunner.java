package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.util.entities.ActionResult;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

@AllArgsConstructor
public class MergePreparationRunner implements BackgroundRunner<MergePreparationRunner.Result> {
    private List<TableFileInfo> displayedTableFilesInfo;

    private List<FileInfo> filesInfo;

    private TableWithFiles tableWithFiles;

    private GuiSettings settings;

    @Override
    public MergePreparationRunner.Result run(BackgroundRunnerManager runnerManager) {
        /*VideoTabBackgroundUtils.clearActionResults(displayedTableFilesInfo, tableWithFiles, runnerManager);

        List<TableFileInfo> selectedTableFilesInfo = VideoTabBackgroundUtils.getSelectedFilesInfo(
                displayedTableFilesInfo,
                runnerManager
        );

        int allFileCount = selectedTableFilesInfo.size();

        int filesWithoutSelectionCount = getFilesWithoutSelectionCount(selectedTableFilesInfo, runnerManager);
        if (filesWithoutSelectionCount != 0) {
            return getFilesWithoutSelectionResult(filesWithoutSelectionCount, allFileCount);
        }

        int processedCount = 0;
        int finishedSuccessfullyCount = 0;
        int notPossibleCount = 0;
        int failedCount = 0;

        runnerManager.setIndeterminateProgress();
        runnerManager.setCancellationPossible(true);

        for (TableFileInfo tableFileInfo : selectedTableFilesInfo) {
            if (runnerManager.isCancelled()) {
                break;
            }

            runnerManager.updateMessage(
                    VideoTabBackgroundUtils.getProcessFileProgressMessage(processedCount, allFileCount, tableFileInfo)
            );

            FileInfo fileInfo = FileInfo.getById(tableFileInfo.getId(), filesInfo);

            TableSubtitleOption tableUpperOption = tableFileInfo.getUpperOption();
            TableSubtitleOption tableLowerOption = tableFileInfo.getLowerOption();
            SubtitleOption upperOption = SubtitleOption.getById(
                    tableUpperOption.getId(),
                    fileInfo.getSubtitleOptions()
            );
            SubtitleOption lowerOption = SubtitleOption.getById(
                    tableLowerOption.getId(),
                    fileInfo.getSubtitleOptions()
            );

            SubtitleMerger.mergeSubtitles(upperOption.getSubtitles(), lowerOption.getSubtitles());

            finishedSuccessfullyCount++;
            processedCount++;
        }

        return ActionResult.onlySuccess("ok");*/
        return new Result(0L, 0L, 0L, null);
    }

    //todo better diagnostics
    private static int getFilesWithoutSelectionCount(
            List<TableFileInfo> filesInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("Getting file availability info...");

        return (int) filesInfo.stream()
                .filter(fileInfo -> fileInfo.getUpperOption() == null || fileInfo.getLowerOption() == null)
                .count();
    }

    private static ActionResult getFilesWithoutSelectionResult(
            int filesWithoutSelectionCount,
            int selectedFileCount
    ) {
        String message;
        if (selectedFileCount == 1) {
            message = "Merge for the file is unavailable because you have to select upper and lower subtitles first";
        } else {
            message = "Merge is unavailable because you have to select upper and lower subtitles for all the selected "
                    + "files (%d missing)";
        }

        return ActionResult.onlyError(String.format(message, filesWithoutSelectionCount));
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private Long requiredTempSpace;

        private Long availableTempSpace;

        private Long requiredPermanentSpace;

        private List<File> filesToOverwrite;
    }
}
