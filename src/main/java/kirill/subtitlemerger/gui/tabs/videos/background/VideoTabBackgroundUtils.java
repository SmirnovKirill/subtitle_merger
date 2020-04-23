package kirill.subtitlemerger.gui.tabs.videos.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.gui.tabs.videos.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.tabs.videos.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.files.FileInfoGetter;
import kirill.subtitlemerger.logic.files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.files.entities.FileInfo;
import kirill.subtitlemerger.logic.files.entities.FileUnavailabilityReason;
import kirill.subtitlemerger.logic.files.entities.SubtitleOptionUnavailabilityReason;
import kirill.subtitlemerger.logic.settings.SortBy;
import kirill.subtitlemerger.logic.settings.SortDirection;
import kirill.subtitlemerger.logic.utils.Utils;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
class VideoTabBackgroundUtils {
    static final String FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT = "Subtitles seem to have an incorrect format";

    static List<FileInfo> getFilesInfo(
            List<File> files,
            Ffprobe ffprobe,
            BackgroundManager backgroundManager
    ) {
        List<FileInfo> result = new ArrayList<>();

        backgroundManager.updateProgress(0, files.size());

        int i = 0;
        for (File file : files) {
            backgroundManager.updateMessage("Getting file info " + file.getName() + "...");

            if (file.isFile() && file.exists()) {
                result.add(
                        FileInfoGetter.getWithoutLoadingSubtitles(
                                file,
                                LogicConstants.ALLOWED_VIDEO_EXTENSIONS,
                                ffprobe
                        )
                );
            }

            backgroundManager.updateProgress(i + 1, files.size());

            i++;
        }

        return result;
    }

    static List<TableFileInfo> tableFilesInfoFrom(
            List<FileInfo> filesInfo,
            boolean showFullPath,
            boolean selectByDefault,
            BackgroundManager backgroundManager,
            Settings settings
    ) {
        backgroundManager.setIndeterminateProgress();

        List<TableFileInfo> result = new ArrayList<>();

        for (FileInfo fileInfo : filesInfo) {
            backgroundManager.updateMessage("Creating object for " + fileInfo.getFile().getName() + "...");

            result.add(tableFileInfoFrom(fileInfo, showFullPath, selectByDefault, settings));
        }

        return result;
    }

    private static TableFileInfo tableFileInfoFrom(
            FileInfo fileInfo,
            boolean showFullPath,
            boolean selected,
            Settings settings
    ) {
        String pathToDisplay = showFullPath ? fileInfo.getFile().getAbsolutePath() : fileInfo.getFile().getName();

        List<TableSubtitleOption> subtitleOptions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
            boolean fileHasPreferredUpperLanguage = fileInfo.getFfmpegSubtitleStreams().stream()
                    .anyMatch(stream -> Utils.languagesEqual(stream.getLanguage(), settings.getUpperLanguage()));
            boolean fileHasPreferredLowerLanguage = fileInfo.getFfmpegSubtitleStreams().stream()
                    .anyMatch(stream -> Utils.languagesEqual(stream.getLanguage(), settings.getLowerLanguage()));
            boolean hideableOptionsPossible = fileHasPreferredUpperLanguage && fileHasPreferredLowerLanguage;

            subtitleOptions = fileInfo.getFfmpegSubtitleStreams().stream()
                    .map(subtitleStream -> tableSubtitleOptionFrom(subtitleStream, hideableOptionsPossible, settings))
                    .collect(Collectors.toList());
        }

        for (int i = 0; i < 2; i++) {
            subtitleOptions.add(
                    new TableSubtitleOption(
                            null,
                            null,
                            false,
                            true,
                            true,
                            TableSubtitleOption.UNKNOWN_SIZE,
                            null,
                            null,
                            false,
                            false,
                            LogicConstants.SUB_RIP_FORMAT
                    )
            );
        }

        boolean haveHideableOptions = subtitleOptions.stream().anyMatch(TableSubtitleOption::isHideable);

        return new TableFileInfo(
                fileInfo.getId(),
                selected,
                pathToDisplay,
                fileInfo.getSize(),
                fileInfo.getLastModified(),
                tableUnavailabilityReasonFrom(fileInfo.getUnavailabilityReason()),
                fileInfo.getFormat(),
                subtitleOptions,
                haveHideableOptions,
                null
        );
    }

    static TableSubtitleOption tableSubtitleOptionFrom(
            FfmpegSubtitleStream subtitleStream,
            boolean hideableOptionsPossible,
            Settings settings
    ) {
        return new TableSubtitleOption(
                subtitleStream.getId(),
                tableOptionTitleFrom(subtitleStream),
                hideableOptionsPossible && isOptionHideable(subtitleStream, settings),
                false,
                false,
                subtitleStream.getSize() != null
                        ? subtitleStream.getSize()
                        : TableSubtitleOption.UNKNOWN_SIZE,
                null,
                tableUnavailabilityReasonFrom(subtitleStream.getUnavailabilityReason()),
                false,
                false,
                subtitleStream.getFormat()
        );
    }

    private static String tableOptionTitleFrom(FfmpegSubtitleStream stream) {
        String result = GuiUtils.languageToString(stream.getLanguage()).toUpperCase();

        if (!StringUtils.isBlank(stream.getTitle())) {
            result += " (" + stream.getTitle() + ")";
        }

        return result;
    }

    private static boolean isOptionHideable(FfmpegSubtitleStream subtitleStream, Settings settings) {
        LanguageAlpha3Code streamLanguage = subtitleStream.getLanguage();

        return !Utils.languagesEqual(streamLanguage, settings.getUpperLanguage())
                && !Utils.languagesEqual(streamLanguage, settings.getLowerLanguage());
    }

    private static TableSubtitleOption.UnavailabilityReason tableUnavailabilityReasonFrom(
            SubtitleOptionUnavailabilityReason reason
    ) {
        if (reason == null) {
            return null;
        }

        return EnumUtils.getEnum(TableSubtitleOption.UnavailabilityReason.class, reason.toString());
    }

    private static TableFileInfo.UnavailabilityReason tableUnavailabilityReasonFrom(FileUnavailabilityReason reason) {
        if (reason == null) {
            return null;
        }

        return EnumUtils.getEnum(TableFileInfo.UnavailabilityReason.class, reason.toString());
    }

    static List<TableFileInfo> getOnlyAvailableFilesInfo(
            List<TableFileInfo> allFilesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Filtering unavailable...");

        return allFilesInfo.stream()
                .filter(fileInfo -> fileInfo.getUnavailabilityReason() == null)
                .collect(Collectors.toList());
    }

    static List<TableFileInfo> getSortedFilesInfo(
            List<TableFileInfo> allFilesInfo,
            SortBy sortBy,
            SortDirection sortDirection,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Sorting file list...");

        Comparator<TableFileInfo> comparator;
        switch (sortBy) {
            case NAME:
                comparator = Comparator.comparing(TableFileInfo::getFilePath);
                break;
            case MODIFICATION_TIME:
                comparator = Comparator.comparing(TableFileInfo::getLastModified);
                break;
            case SIZE:
                comparator = Comparator.comparing(TableFileInfo::getSize);
                break;
            default:
                throw new IllegalStateException();
        }

        if (sortDirection == SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        return allFilesInfo.stream().sorted(comparator).collect(Collectors.toList());
    }

    static int getAllSelectableCount(
            List<TableFileInfo> allFilesInfo,
            TableWithFiles.Mode mode,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Calculating number of files...");

        if (mode == TableWithFiles.Mode.SEPARATE_FILES) {
            return allFilesInfo.size();
        } else if (mode == TableWithFiles.Mode.DIRECTORY) {
            return (int) allFilesInfo.stream()
                    .filter(fileInfo -> fileInfo.getUnavailabilityReason() == null)
                    .count();
        } else {
            throw new IllegalStateException();
        }
    }

    static int getSelectedAvailableCount(
            List<TableFileInfo> allFilesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Calculating number of files...");

        return (int) allFilesInfo.stream()
                .filter(TableFileInfo::isSelected)
                .filter(fileInfo -> fileInfo.getUnavailabilityReason() == null)
                .count();

    }

    static int getSelectedUnavailableCount(
            List<TableFileInfo> allFilesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Calculating number of files...");

        return (int) allFilesInfo.stream()
                .filter(TableFileInfo::isSelected)
                .filter(fileInfo -> fileInfo.getUnavailabilityReason() != null)
                .count();

    }

    static String getLoadSubtitlesProgressMessage(
            int processedCount,
            int subtitlesToLoadCount,
            FfmpegSubtitleStream subtitleStream,
            File file
    ) {
        String progressPrefix = subtitlesToLoadCount > 1
                ? String.format("%d/%d ", processedCount + 1, subtitlesToLoadCount) + "getting subtitles "
                : "Getting subtitles ";

        return progressPrefix
                + GuiUtils.languageToString(subtitleStream.getLanguage()).toUpperCase()
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }

    static void clearActionResults(
            List<TableFileInfo> filesInfo,
            TableWithFiles tableWithFiles,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Clearing state...");

        for (TableFileInfo fileInfo : filesInfo) {
            Platform.runLater(() -> tableWithFiles.clearActionResult(fileInfo));
        }
    }

    static List<TableFileInfo> getSelectedFilesInfo(
            List<TableFileInfo> filesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting list of files to work with...");

        return filesInfo.stream().filter(TableFileInfo::isSelected).collect(Collectors.toList());
    }

    static ActionResult generateSubtitleLoadingActionResult(
            int subtitlesToLoadCount,
            int processedCount,
            int loadedSuccessfullyCount,
            int failedToLoadCount
    ) {
        String success = null;
        String warn = null;
        String error = null;

        if (subtitlesToLoadCount == 0) {
            warn = "There are no subtitles to load";
        } else if (processedCount == 0) {
            warn = "Task has been cancelled, nothing was loaded";
        } else if (loadedSuccessfullyCount == subtitlesToLoadCount) {
            success = GuiUtils.getTextDependingOnTheCount(
                    loadedSuccessfullyCount,
                    "Subtitles have been loaded successfully",
                    "All %d subtitles have been loaded successfully"
            );
        } else if (failedToLoadCount == subtitlesToLoadCount) {
            error = GuiUtils.getTextDependingOnTheCount(
                    failedToLoadCount,
                    "Failed to load subtitles",
                    "Failed to load all %d subtitles"
            );
        } else {
            if (loadedSuccessfullyCount != 0) {
                success = String.format(
                        "%d/%d subtitles have been loaded successfully",
                        loadedSuccessfullyCount,
                        subtitlesToLoadCount
                );
            }

            if (processedCount != subtitlesToLoadCount) {
                if (loadedSuccessfullyCount == 0) {
                    warn = GuiUtils.getTextDependingOnTheCount(
                            subtitlesToLoadCount - processedCount,
                            String.format(
                                    "1/%d subtitle loadings has been cancelled",
                                    subtitlesToLoadCount
                            ),
                            String.format(
                                    "%%d/%d subtitle loadings have been cancelled",
                                    subtitlesToLoadCount
                            )
                    );
                } else {
                    warn = String.format(
                            "%d/%d cancelled",
                            subtitlesToLoadCount - processedCount,
                            subtitlesToLoadCount
                    );
                }
            }

            if (failedToLoadCount != 0) {
                error = String.format(
                        "%d/%d failed",
                        failedToLoadCount,
                        subtitlesToLoadCount
                );
            }
        }

        return new ActionResult(success, warn, error);
    }

    static String failedToLoadReasonFrom(FfmpegException.Code code) {
        if (code == FfmpegException.Code.GENERAL_ERROR) {
            return "Fmpeg returned an error";
        } else {
            log.error("unexpected code: " + code);
            throw new IllegalStateException();
        }
    }

    static String getProcessFileProgressMessage(int processedCount, int allFileCount, TableFileInfo fileInfo) {
        String progressPrefix = allFileCount > 1
                ? String.format("%d/%d ", processedCount + 1, allFileCount) + " processing file "
                : "Processing file ";

        return progressPrefix + fileInfo.getFilePath() + "...";
    }
}
