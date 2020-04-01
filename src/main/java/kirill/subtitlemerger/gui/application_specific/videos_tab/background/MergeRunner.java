package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.util.entities.ActionResult;
import kirill.subtitlemerger.logic.core.SubtitleMerger;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleOption;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class MergeRunner implements BackgroundRunner<ActionResult> {
    private List<TableFileInfo> displayedTableFilesInfo;

    private List<FileInfo> filesInfo;

    private TableWithFiles tableWithFiles;

    private Ffmpeg ffmpeg;

    private GuiSettings settings;

    @Override
    public ActionResult run(BackgroundRunnerManager runnerManager) {
        VideoTabBackgroundUtils.clearActionResults(displayedTableFilesInfo, tableWithFiles, runnerManager);

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

            try {
                SubtitleMerger.mergeSubtitles(upperOption.getSubtitles(), lowerOption.getSubtitles());
            } catch (InterruptedException e) {
                //todo
                e.printStackTrace();
            }

            finishedSuccessfullyCount++;
            processedCount++;
        }

        return ActionResult.onlySuccess("ok");
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
}
