package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.GuiUtils;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.util.entities.ActionResult;
import kirill.subtitlemerger.logic.file_info.entities.FileInfo;
import kirill.subtitlemerger.logic.settings.SortBy;
import kirill.subtitlemerger.logic.settings.SortDirection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
public class AddFilesRunner implements BackgroundRunner<AddFilesRunner.Result> {
    private List<FileInfo> filesInfo;

    private List<File> filesToAdd;

    private List<TableFileInfo> allTableFilesInfo;

    private TableWithFiles.Mode mode;

    private boolean hideUnavailable;

    private SortBy sortBy;

    private SortDirection sortDirection;

    private GuiContext context;

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        List<FileInfo> filesToAddInfo = VideoTabBackgroundUtils.getFilesInfo(
                filesToAdd,
                context.getFfprobe(),
                runnerManager
        );
        removeAlreadyAdded(filesToAddInfo, filesInfo, runnerManager);

        if (filesToAddInfo.size() + filesInfo.size() > GuiConstants.TABLE_FILE_LIMIT) {
            return new Result(
                    String.format("There will be too many files (>%d)", GuiConstants.TABLE_FILE_LIMIT),
                    filesToAdd.size(),
                    0,
                    null,
                    null,
                    null
            );
        }

        filesInfo.addAll(filesToAddInfo);

        List<TableFileInfo> tableFilesToAddInfo = VideoTabBackgroundUtils.tableFilesInfoFrom(
                filesToAddInfo,
                true,
                true,
                runnerManager,
                context.getSettings()
        );
        allTableFilesInfo.addAll(tableFilesToAddInfo);

        List<TableFileInfo> filesToShowInfo = null;
        if (hideUnavailable) {
            filesToShowInfo = VideoTabBackgroundUtils.getOnlyAvailableFilesInfo(allTableFilesInfo, runnerManager);
        }

        filesToShowInfo = VideoTabBackgroundUtils.getSortedFilesInfo(
                filesToShowInfo != null ? filesToShowInfo : allTableFilesInfo,
                sortBy,
                sortDirection,
                runnerManager
        );

        return new Result(
                null,
                filesToAdd.size(),
                filesToAddInfo.size(),
                filesInfo,
                allTableFilesInfo,
                new TableFilesToShowInfo(
                        filesToShowInfo,
                        VideoTabBackgroundUtils.getAllSelectableCount(filesToShowInfo, mode, runnerManager),
                        VideoTabBackgroundUtils.getSelectedAvailableCount(filesToShowInfo, runnerManager),
                        VideoTabBackgroundUtils.getSelectedUnavailableCount(filesToShowInfo, runnerManager)
                )
        );
    }

    private static void removeAlreadyAdded(
            List<FileInfo> filesToAddInfo,
            List<FileInfo> allFilesInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("Removing already added files...");

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

    public static ActionResult generateActionResult(Result taskResult) {
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

        return new ActionResult(success, null, null);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private String addFailedReason;

        private int filesToAddCount;

        private int actuallyAddedCount;

        private List<FileInfo> filesInfo;

        private List<TableFileInfo> allTableFilesInfo;

        private TableFilesToShowInfo tableFilesToShowInfo;
    }
}
