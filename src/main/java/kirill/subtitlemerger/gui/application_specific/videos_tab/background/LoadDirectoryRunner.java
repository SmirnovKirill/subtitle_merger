package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.GuiUtils;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.utils.FileValidator;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommonsLog
@AllArgsConstructor
public class LoadDirectoryRunner implements BackgroundRunner<LoadDirectoryRunner.Result> {
    private String path;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private GuiContext context;

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        DirectoryInfo directoryInfo = getDirectoryInfo(path, runnerManager);
        if (directoryInfo.getUnavailabilityReason() != null) {
            return new Result(
                    unavailabilityReasonToString(directoryInfo.getUnavailabilityReason(), path),
                    disableRefresh(directoryInfo.getUnavailabilityReason()),
                    null,
                    null,
                    false,
                    null
            );
        }

        List<FileInfo> filesInfo = VideoTabBackgroundUtils.getFilesInfo(
                directoryInfo.getDirectoryFiles(),
                context.getFfprobe(),
                runnerManager
        );
        List<TableFileInfo> allTableFilesInfo = VideoTabBackgroundUtils.tableFilesInfoFrom(
                filesInfo,
                false,
                false,
                runnerManager,
                context.getSettings()
        );

        boolean hideUnavailable = shouldHideUnavailable(filesInfo, runnerManager);

        List<TableFileInfo> tableFilesToShowInfo = null;
        if (hideUnavailable) {
            tableFilesToShowInfo = VideoTabBackgroundUtils.getOnlyAvailableFilesInfo(allTableFilesInfo, runnerManager);
        }

        tableFilesToShowInfo = VideoTabBackgroundUtils.getSortedFilesInfo(
                tableFilesToShowInfo != null ? tableFilesToShowInfo : allTableFilesInfo,
                sortBy,
                sortDirection,
                runnerManager
        );

        return new Result(
                null,
                false,
                filesInfo,
                allTableFilesInfo,
                hideUnavailable,
                new TableFilesToShowInfo(
                        tableFilesToShowInfo,
                        VideoTabBackgroundUtils.getAllSelectableCount(
                                tableFilesToShowInfo,
                                TableWithFiles.Mode.DIRECTORY,
                                runnerManager
                        ),
                        0,
                        0
                )
        );
    }

    private static DirectoryInfo getDirectoryInfo(String path, BackgroundRunnerManager runnerManager) {
        if (StringUtils.isBlank(path)) {
            return new DirectoryInfo(null, null, DirectoryUnavailabilityReason.PATH_EMPTY);
        }

        if (path.length() > FileValidator.PATH_LENGTH_LIMIT) {
            return new DirectoryInfo(null, null, DirectoryUnavailabilityReason.PATH_IS_TOO_LONG);
        }

        try {
            Path.of(path);
        } catch (InvalidPathException e) {
            return new DirectoryInfo(null, null, DirectoryUnavailabilityReason.INVALID_PATH);
        }

        File file = new File(path);
        if (file.isFile()) {
            return new DirectoryInfo(file, null, DirectoryUnavailabilityReason.IS_A_FILE);
        }

        if (!file.exists()) {
            return new DirectoryInfo(file, null, DirectoryUnavailabilityReason.DOES_NOT_EXIST);
        }

        List<File> directoryFiles = getDirectoryFiles(path, runnerManager);
        if (directoryFiles.size() > GuiConstants.TABLE_FILE_LIMIT) {
            return new DirectoryInfo(file, null, DirectoryUnavailabilityReason.TOO_MANY_FILES);
        }

        return new DirectoryInfo(file, directoryFiles, null);
    }

    private static List<File> getDirectoryFiles(String directoryPath, BackgroundRunnerManager runnerManager) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("Getting file list...");

        File directory = new File(directoryPath);
        File[] directoryFiles = directory.listFiles();

        if (directoryFiles == null) {
            log.warn("failed to get directory files, directory " + directory.getAbsolutePath());
            return new ArrayList<>();
        } else {
            return Arrays.asList(directoryFiles);
        }
    }

    private static boolean disableRefresh(DirectoryUnavailabilityReason unavailabilityReason) {
        return unavailabilityReason == DirectoryUnavailabilityReason.PATH_EMPTY
                || unavailabilityReason == DirectoryUnavailabilityReason.PATH_IS_TOO_LONG
                || unavailabilityReason == DirectoryUnavailabilityReason.INVALID_PATH;
    }

    private static String unavailabilityReasonToString(DirectoryUnavailabilityReason reason, String path) {
        path = GuiUtils.getShortenedStringIfNecessary(path, 20, 40);

        switch (reason) {
            case PATH_EMPTY:
                return "Directory is not selected!";
            case PATH_IS_TOO_LONG:
                return "Directory path is too long";
            case INVALID_PATH:
                return "Directory path is invalid";
            case IS_A_FILE:
                return path + " is a file, not a directory";
            case DOES_NOT_EXIST:
                return "Directory '" + path + "' doesn't exist";
            case TOO_MANY_FILES:
                return String.format("Directory has too many files (>%d)", GuiConstants.TABLE_FILE_LIMIT);
            default:
                throw new IllegalStateException();
        }
    }

    /*
     * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
     * not be checked because the user will see just an empty file list which isn't very user friendly.
     */
    private static boolean shouldHideUnavailable(List<FileInfo> filesInfo, BackgroundRunnerManager runnerManager) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("Calculating whether to hide unavailable files by default...");

        return filesInfo.stream().anyMatch(fileInfo -> fileInfo.getUnavailabilityReason() == null);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private String unavailabilityReason;

        private boolean disableRefresh;

        private List<FileInfo> filesInfo;

        private List<TableFileInfo> allTableFilesInfo;

        private boolean hideUnavailable;

        private TableFilesToShowInfo tableFilesToShowInfo;
    }

    @AllArgsConstructor
    @Getter
    private static class DirectoryInfo {
        private File directory;

        private List<File> directoryFiles;
        
        private DirectoryUnavailabilityReason unavailabilityReason;
    }
    
    private enum DirectoryUnavailabilityReason {
        PATH_EMPTY,
        PATH_IS_TOO_LONG,
        INVALID_PATH,
        IS_A_FILE,
        DOES_NOT_EXIST,
        TOO_MANY_FILES
    }
}
