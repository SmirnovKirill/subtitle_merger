package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.GuiUtils;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.work_with_files.FileInfoGetter;
import kirill.subtitlemerger.logic.work_with_files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffprobe;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class VideoTabBackgroundUtils {
    static List<FileInfo> getFilesInfo(
            List<File> files,
            Ffprobe ffprobe,
            BackgroundRunnerManager runnerManager
    ) {
        List<FileInfo> result = new ArrayList<>();

        runnerManager.updateProgress(0, files.size());

        int i = 0;
        for (File file : files) {
            runnerManager.updateMessage("getting file info " + file.getName() + "...");

            if (file.isFile() && file.exists()) {
                result.add(
                        FileInfoGetter.getFileInfoWithoutSubtitles(
                                file,
                                LogicConstants.ALLOWED_VIDEO_EXTENSIONS,
                                LogicConstants.ALLOWED_VIDEO_MIME_TYPES,
                                ffprobe
                        )
                );
            }

            runnerManager.updateProgress(i + 1, files.size());

            i++;
        }

        return result;
    }

    static List<TableFileInfo> tableFilesInfoFrom(
            List<FileInfo> filesInfo,
            boolean showFullPath,
            boolean selectByDefault,
            BackgroundRunnerManager runnerManager,
            GuiSettings settings
    ) {
        runnerManager.setIndeterminateProgress();

        List<TableFileInfo> result = new ArrayList<>();

        for (FileInfo fileInfo : filesInfo) {
            runnerManager.updateMessage("creating object for " + fileInfo.getFile().getName() + "...");

            result.add(tableFileInfoFrom(fileInfo, showFullPath, selectByDefault, settings));
        }

        return result;
    }

    private static TableFileInfo tableFileInfoFrom(
            FileInfo fileInfo,
            boolean showFullPath,
            boolean selected,
            GuiSettings settings
    ) {
        String pathToDisplay = showFullPath ? fileInfo.getFile().getAbsolutePath() : fileInfo.getFile().getName();

        List<TableSubtitleOption> subtitleOptions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
            boolean fileHasPreferredUpperLanguage = fileInfo.getFfmpegSubtitleStreams().stream()
                    .anyMatch(stream -> stream.getLanguage() == settings.getUpperLanguage());
            boolean fileHasPreferredLowerLanguage = fileInfo.getFfmpegSubtitleStreams().stream()
                    .anyMatch(stream -> stream.getLanguage() == settings.getLowerLanguage());
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
                            false
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
                subtitleOptions,
                haveHideableOptions,
                null
        );
    }

    private static TableSubtitleOption tableSubtitleOptionFrom(
            FfmpegSubtitleStream subtitleStream,
            boolean hideableOptionsPossible,
            GuiSettings settings
    ) {
        return new TableSubtitleOption(
                subtitleStream.getId(),
                tableOptionTitleFrom(subtitleStream),
                hideableOptionsPossible && isOptionHideable(subtitleStream, settings),
                false,
                false,
                subtitleStream.getSubtitles() != null
                        ? subtitleStream.getSubtitles().getSize()
                        : TableSubtitleOption.UNKNOWN_SIZE,
                null,
                tableUnavailabilityReasonFrom(subtitleStream.getUnavailabilityReason()),
                false,
                false
        );
    }

    private static String tableOptionTitleFrom(FfmpegSubtitleStream stream) {
        String result = GuiUtils.languageToString(stream.getLanguage()).toUpperCase();

        if (!StringUtils.isBlank(stream.getTitle())) {
            result += " (" + stream.getTitle() + ")";
        }

        return result;
    }

    private static boolean isOptionHideable(FfmpegSubtitleStream subtitleStream, GuiSettings settings) {
        LanguageAlpha3Code streamLanguage = subtitleStream.getLanguage();

        return streamLanguage != settings.getUpperLanguage() && streamLanguage != settings.getLowerLanguage();
    }

    private static TableSubtitleOption.UnavailabilityReason tableUnavailabilityReasonFrom(
            FfmpegSubtitleStream.UnavailabilityReason reason
    ) {
        if (reason == null) {
            return null;
        }

        return EnumUtils.getEnum(TableSubtitleOption.UnavailabilityReason.class, reason.toString());
    }

    private static TableFileInfo.UnavailabilityReason tableUnavailabilityReasonFrom(
            FileInfo.UnavailabilityReason reason
    ) {
        if (reason == null) {
            return null;
        }

        return EnumUtils.getEnum(TableFileInfo.UnavailabilityReason.class, reason.toString());
    }

    static List<TableFileInfo> getOnlyAvailableFilesInfo(
            List<TableFileInfo> allFilesInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("filtering unavailable...");

        return allFilesInfo.stream()
                .filter(fileInfo -> fileInfo.getUnavailabilityReason() == null)
                .collect(Collectors.toList());
    }

    static List<TableFileInfo> getSortedFilesInfo(
            List<TableFileInfo> allFilesInfo,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("sorting file list...");

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

        if (sortDirection == GuiSettings.SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        return allFilesInfo.stream().sorted(comparator).collect(Collectors.toList());
    }

    static int getAllSelectableCount(
            List<TableFileInfo> allFilesInfo,
            TableWithFiles.Mode mode,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("calculating number of files...");

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
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("calculating number of files...");

        return (int) allFilesInfo.stream()
                .filter(TableFileInfo::isSelected)
                .filter(fileInfo -> fileInfo.getUnavailabilityReason() == null)
                .count();

    }

    static int getSelectedUnavailableCount(
            List<TableFileInfo> allFilesInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("calculating number of files...");

        return (int) allFilesInfo.stream()
                .filter(TableFileInfo::isSelected)
                .filter(fileInfo -> fileInfo.getUnavailabilityReason() != null)
                .count();

    }

    static String getLoadSubtitlesProgressMessage(
            int subtitlesToLoadCount,
            int processedCount,
            FfmpegSubtitleStream subtitleStream,
            File file
    ) {
        String progressPrefix = subtitlesToLoadCount > 1
                ? String.format("%d/%d ", processedCount + 1, subtitlesToLoadCount)
                : "";

        return progressPrefix + "getting subtitle "
                + GuiUtils.languageToString(subtitleStream.getLanguage()).toUpperCase()
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }

    static void clearActionResults(
            List<TableFileInfo> filesInfo,
            TableWithFiles tableWithFiles,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("clearing state...");

        for (TableFileInfo fileInfo : filesInfo) {
            Platform.runLater(() -> tableWithFiles.clearActionResult(fileInfo));
        }
    }

    static List<TableFileInfo> getSelectedFilesInfo(
            List<TableFileInfo> filesInfo,
            BackgroundRunnerManager runnerManager
    ) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("getting list of files to work with...");

        return filesInfo.stream().filter(TableFileInfo::isSelected).collect(Collectors.toList());
    }
}
