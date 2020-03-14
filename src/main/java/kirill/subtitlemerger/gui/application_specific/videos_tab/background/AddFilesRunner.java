package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.GuiHelperMethods;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.gui.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
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

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext context;

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        List<FileInfo> filesToAddInfo = BackgroundHelperMethods.getFilesInfo(
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

        List<TableFileInfo> tableFilesToAddInfo = BackgroundHelperMethods.tableFilesInfoFrom(
                filesToAddInfo,
                true,
                true,
                runnerManager,
                context.getSettings()
        );
        allTableFilesInfo.addAll(tableFilesToAddInfo);

        List<TableFileInfo> filesToShowInfo = null;
        if (hideUnavailable) {
            filesToShowInfo = BackgroundHelperMethods.getOnlyAvailableFilesInfo(allTableFilesInfo, runnerManager);
        }

        filesToShowInfo = BackgroundHelperMethods.getSortedFilesInfo(
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
                        BackgroundHelperMethods.getAllSelectableCount(filesToShowInfo, mode, runnerManager),
                        BackgroundHelperMethods.getSelectedAvailableCount(filesToShowInfo, runnerManager),
                        BackgroundHelperMethods.getSelectedUnavailableCount(filesToShowInfo, runnerManager)
                )
        );
    }

    private static void removeAlreadyAdded(
            List<FileInfo> filesToAddInfo,
            List<FileInfo> allFilesInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
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

    public static ActionResult generateActionResult(Result taskResult) {
        String success;

        int filesToAdd = taskResult.getFilesToAddCount();
        int actuallyAdded = taskResult.getActuallyAddedCount();

        if (actuallyAdded == 0) {
            success = GuiHelperMethods.getTextDependingOnTheCount(
                    filesToAdd,
                    "File has been added already",
                    "All %d files have been added already"
            );
        } else if (filesToAdd == actuallyAdded) {
            success = GuiHelperMethods.getTextDependingOnTheCount(
                    actuallyAdded,
                    "File has been added successfully",
                    "All %d files have been added successfully"
            );
        } else {
            success = GuiHelperMethods.getTextDependingOnTheCount(
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
