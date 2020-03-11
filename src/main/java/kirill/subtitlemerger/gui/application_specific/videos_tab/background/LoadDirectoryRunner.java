package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommonsLog
@AllArgsConstructor
public class LoadDirectoryRunner implements BackgroundRunner<LoadDirectoryRunner.Result> {
    private File directory;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext context;

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        List<File> files = getDirectoryFiles(directory, runnerManager);
        List<FileInfo> filesInfo = BackgroundHelperMethods.getFilesInfo(files, context.getFfprobe(), runnerManager);
        List<TableFileInfo> allTableFilesInfo = BackgroundHelperMethods.tableFilesInfoFrom(
                filesInfo,
                false,
                false,
                runnerManager,
                context.getSettings()
        );

        boolean hideUnavailable = shouldHideUnavailable(filesInfo, runnerManager);

        List<TableFileInfo> tableFilesToShowInfo = null;
        if (hideUnavailable) {
            tableFilesToShowInfo = BackgroundHelperMethods.getOnlyAvailableFilesInfo(allTableFilesInfo, runnerManager);
        }

        tableFilesToShowInfo = BackgroundHelperMethods.getSortedFilesInfo(
                tableFilesToShowInfo != null ? tableFilesToShowInfo : allTableFilesInfo,
                sortBy,
                sortDirection,
                runnerManager
        );

        return new Result(
                filesInfo,
                hideUnavailable,
                allTableFilesInfo,
                new TableFilesToShowInfo(
                        tableFilesToShowInfo,
                        BackgroundHelperMethods.getAllSelectableCount(
                                tableFilesToShowInfo,
                                TableWithFiles.Mode.DIRECTORY,
                                runnerManager
                        ),
                        0,
                        0
                )
        );
    }

    private static List<File> getDirectoryFiles(File directory, BackgroundRunnerManager runnerManager) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("getting file list...");

        File[] directoryFiles = directory.listFiles();

        if (directoryFiles == null) {
            log.warn("failed to get directory files, directory " + directory.getAbsolutePath());
            return new ArrayList<>();
        } else {
            return Arrays.asList(directoryFiles);
        }
    }

    /*
     * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
     * not be checked because the user will see just an empty file list which isn't very user friendly.
     */
    private static boolean shouldHideUnavailable(List<FileInfo> filesInfo, BackgroundRunnerManager runnerManager) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("calculating whether to hide unavailable files by default...");

        return filesInfo.stream().anyMatch(fileInfo -> fileInfo.getUnavailabilityReason() == null);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<FileInfo> filesInfo;

        private boolean hideUnavailable;

        private List<TableFileInfo> allTableFilesInfo;

        private TableFilesToShowInfo tableFilesToShowInfo;
    }
}
