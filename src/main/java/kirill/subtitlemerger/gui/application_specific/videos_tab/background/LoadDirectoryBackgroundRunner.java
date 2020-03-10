package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommonsLog
public class LoadDirectoryBackgroundRunner implements BackgroundRunner<LoadDirectoryBackgroundRunner.Result> {
    private File directory;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext guiContext;

    public LoadDirectoryBackgroundRunner(
            File directory,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            GuiContext guiContext
    ) {
        this.directory = directory;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.guiContext = guiContext;
    }

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        List<File> files = getDirectoryFiles(directory, runnerManager);
        List<FileInfo> filesInfo = BackgroundHelperMethods.getFilesInfo(files, guiContext.getFfprobe(), runnerManager);
        List<TableFileInfo> allTableFilesInfo = BackgroundHelperMethods.tableFilesInfoFrom(
                filesInfo,
                false,
                false,
                runnerManager,
                guiContext.getSettings()
        );

        boolean hideUnavailable = shouldHideUnavailable(filesInfo, runnerManager);
        List<TableFileInfo> tableFilesToShowInfo = BackgroundHelperMethods.getFilesToShowInfo(
                allTableFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                runnerManager
        );

        int allSelectableCount = getAllSelectableCount(tableFilesToShowInfo, runnerManager);

        return new Result(filesInfo, allTableFilesInfo, tableFilesToShowInfo, hideUnavailable, allSelectableCount);
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

    private static int getAllSelectableCount(
            List<TableFileInfo> tableFilesToShowInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.updateMessage("getting number of all selectable files...");

        return (int) tableFilesToShowInfo.stream()
                .filter(fileInfo -> StringUtils.isBlank(fileInfo.getUnavailabilityReason()))
                .count();
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private List<FileInfo> filesInfo;

        private List<TableFileInfo> allTableFilesInfo;

        private List<TableFileInfo> tableFilesToShowInfo;

        private boolean hideUnavailable;

        private int allSelectableCount;
    }
}
