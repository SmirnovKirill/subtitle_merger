package kirill.subtitlemerger.gui.forms.videos.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.*;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.settings.SortBy;
import kirill.subtitlemerger.logic.settings.SortDirection;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.Videos;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOptionNotValidReason;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import kirill.subtitlemerger.logic.videos.entities.VideoNotValidReason;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class VideoBackgroundUtils {
    static final String FAILED_TO_LOAD_STREAM_INCORRECT_FORMAT = "Subtitles seem to have an incorrect format";

    static List<VideoInfo> getVideosInfo(List<File> files, Ffprobe ffprobe, BackgroundManager backgroundManager) {
        List<VideoInfo> result = new ArrayList<>();

        backgroundManager.setCancellationPossible(false);
        backgroundManager.updateProgress(0, files.size());

        int i = 0;
        for (File file : files) {
            backgroundManager.updateMessage("Getting video info " + file.getName() + "...");

            if (file.isFile() && file.exists()) {
                result.add(Videos.getVideoInfo(file, LogicConstants.ALLOWED_VIDEO_EXTENSIONS, ffprobe));
            }

            backgroundManager.updateProgress(i + 1, files.size());

            i++;
        }

        return result;
    }

    static List<TableVideoInfo> tableVideosInfoFrom(
            List<VideoInfo> videosInfo,
            boolean showFullPath,
            boolean selectByDefault,
            Settings settings,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setCancellationPossible(false);
        backgroundManager.setIndeterminateProgress();

        List<TableVideoInfo> result = new ArrayList<>();

        for (VideoInfo videoInfo : videosInfo) {
            backgroundManager.updateMessage("Creating an object for " + videoInfo.getFile().getName() + "...");

            result.add(tableVideoInfoFrom(videoInfo, showFullPath, selectByDefault, settings));
        }

        return result;
    }

    private static TableVideoInfo tableVideoInfoFrom(
            VideoInfo videoInfo,
            boolean showFullPath,
            boolean selected,
            Settings settings
    ) {
        String pathToDisplay = showFullPath ? videoInfo.getFile().getAbsolutePath() : videoInfo.getFile().getName();

        List<TableSubtitleOption> subtitleOptions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(videoInfo.getBuiltInSubtitleOptions())) {
            boolean hasUpperLanguage = videoInfo.getBuiltInSubtitleOptions().stream()
                    .anyMatch(option -> Utils.languagesEqual(option.getLanguage(), settings.getUpperLanguage()));
            boolean hasLowerLanguage = videoInfo.getBuiltInSubtitleOptions().stream()
                    .anyMatch(option -> Utils.languagesEqual(option.getLanguage(), settings.getLowerLanguage()));
            boolean canHideOptions = hasUpperLanguage && hasLowerLanguage;

            subtitleOptions = videoInfo.getBuiltInSubtitleOptions().stream()
                    .map(option -> tableSubtitleOptionFrom(option, canHideOptions, settings))
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

        return new TableVideoInfo(
                videoInfo.getId(),
                selected,
                pathToDisplay,
                videoInfo.getSize(),
                videoInfo.getLastModified(),
                getTextualReason(videoInfo.getNotValidReason(), videoInfo.getFormat()),
                videoInfo.getFormat(),
                subtitleOptions,
                haveHideableOptions,
                null
        );
    }

    static TableSubtitleOption tableSubtitleOptionFrom(
            BuiltInSubtitleOption subtitleOption,
            boolean canHideOptions,
            Settings settings
    ) {
        return new TableSubtitleOption(
                subtitleOption.getId(),
                getOptionTitle(subtitleOption),
                canHideOptions && isOptionHideable(subtitleOption, settings),
                false,
                false,
                ObjectUtils.firstNonNull(subtitleOption.getSize(), TableSubtitleOption.UNKNOWN_SIZE),
                null,
                getTextualReason(subtitleOption.getNotValidReason(), subtitleOption.getFormat()),
                false,
                false,
                subtitleOption.getFormat()
        );
    }

    private static String getOptionTitle(BuiltInSubtitleOption stream) {
        String result = Utils.languageToString(stream.getLanguage()).toUpperCase();

        if (!StringUtils.isBlank(stream.getTitle())) {
            result += " (" + stream.getTitle() + ")";
        }

        return result;
    }

    private static boolean isOptionHideable(BuiltInSubtitleOption subtitleOption, Settings settings) {
        LanguageAlpha3Code optionLanguage = subtitleOption.getLanguage();

        return !Utils.languagesEqual(optionLanguage, settings.getUpperLanguage())
                && !Utils.languagesEqual(optionLanguage, settings.getLowerLanguage());
    }

    @Nullable
    private static String getTextualReason(SubtitleOptionNotValidReason reason, String format) {
        if (reason == null) {
            return null;
        }

        if (reason == SubtitleOptionNotValidReason.NOT_ALLOWED_FORMAT) {
            return "The subtitles have a not allowed format (" + format + ")";
        } else {
            log.error("unexpected subtitle option unavailability reason: " + reason + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    @Nullable
    private static String getTextualReason(VideoNotValidReason reason, String format) {
        if (reason == null) {
            return null;
        }

        switch (reason) {
            case NO_EXTENSION:
                return "The file has no extension";
            case NOT_ALLOWED_EXTENSION:
                return "The file has a not allowed extension";
            case FFPROBE_FAILED:
                return "Failed to get the video information with ffprobe";
            case NOT_ALLOWED_FORMAT:
                return "The video has a format that is not allowed (" + format + ")";
            default:
                log.error("unexpected video unavailability reason: " + reason + ", most likely a bug");
                throw new IllegalStateException();
        }
    }

    public static List<TableVideoInfo> getSortedVideosInfo(
            List<TableVideoInfo> unsortedVideosInfo,
            SortBy sortBy,
            SortDirection sortDirection,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setCancellationPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Sorting video list...");

        Comparator<TableVideoInfo> comparator;
        switch (sortBy) {
            case NAME:
                comparator = Comparator.comparing(TableVideoInfo::getFilePath);
                break;
            case MODIFICATION_TIME:
                comparator = Comparator.comparing(TableVideoInfo::getLastModified);
                break;
            case SIZE:
                comparator = Comparator.comparing(TableVideoInfo::getSize);
                break;
            default:
                log.error("unexpected sortBy value: " + sortBy + ", most likely a bug");
                throw new IllegalStateException();
        }

        if (sortDirection == SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        return unsortedVideosInfo.stream().sorted(comparator).collect(Collectors.toList());
    }

    public static List<TableVideoInfo> getOnlyAvailableFilesInfo(
            List<TableVideoInfo> allFilesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setCancellationPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Filtering unavailable...");

        return allFilesInfo.stream()
                .filter(fileInfo -> StringUtils.isBlank(fileInfo.getNotValidReason()))
                .collect(Collectors.toList());
    }

    public static TableData getTableData(
            TableWithVideos.Mode mode,
            List<TableVideoInfo> videosInfo,
            SortBy sortBy,
            SortDirection sortDirection,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setCancellationPossible(false);
        backgroundManager.setIndeterminateProgress();

        int allSelectableCount = 0;
        int selectedAvailableCount = 0;
        int selectedUnavailableCount = 0;

        backgroundManager.updateMessage("Calculating the number of videos...");
        for (TableVideoInfo videoInfo : videosInfo) {
            if (videoInfo.isSelected()) {
                if (StringUtils.isBlank(videoInfo.getNotValidReason())) {
                    selectedAvailableCount++;
                } else {
                    selectedUnavailableCount++;
                }
            }

            if (mode == TableWithVideos.Mode.SEPARATE_FILES) {
                allSelectableCount++;
            } else if (mode == TableWithVideos.Mode.DIRECTORY) {
                if (StringUtils.isBlank(videoInfo.getNotValidReason())) {
                    allSelectableCount++;
                }
            } else {
                log.error("unexpected mode " + mode + ", most likely a bug");
                throw new IllegalStateException();
            }
        }

        return new TableData(
                mode,
                videosInfo,
                allSelectableCount,
                selectedAvailableCount,
                selectedUnavailableCount,
                getTableSortBy(sortBy),
                getTableSortDirection(sortDirection)
        );
    }

    private static TableSortBy getTableSortBy(SortBy sortBy) {
        switch (sortBy) {
            case NAME:
                return TableSortBy.NAME;
            case MODIFICATION_TIME:
                return TableSortBy.MODIFICATION_TIME;
            case SIZE:
                return TableSortBy.SIZE;
            default:
                log.error("unexpected sort by: " + sortBy + ", most likely a bug");
                throw new IllegalStateException();
        }
    }

    private static TableSortDirection getTableSortDirection(SortDirection sortDirection) {
        switch (sortDirection) {
            case ASCENDING:
                return TableSortDirection.ASCENDING;
            case DESCENDING:
                return TableSortDirection.DESCENDING;
            default:
                log.error("unexpected sort direction: " + sortDirection + ", most likely a bug");
                throw new IllegalStateException();
        }
    }

    public static String getLoadSubtitlesProgressMessage(
            int processedCount,
            int subtitlesToLoadCount,
            BuiltInSubtitleOption subtitleStream,
            File file
    ) {
        String progressPrefix = subtitlesToLoadCount > 1
                ? String.format("%d/%d ", processedCount + 1, subtitlesToLoadCount) + "getting subtitles "
                : "Getting subtitles ";

        return progressPrefix
                + Utils.languageToString(subtitleStream.getLanguage()).toUpperCase()
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }

    public static void clearActionResults(
            List<TableVideoInfo> filesInfo,
            TableWithVideos tableWithFiles,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Clearing state...");

        for (TableVideoInfo fileInfo : filesInfo) {
            Platform.runLater(() -> tableWithFiles.clearActionResult(fileInfo));
        }
    }

    public static List<TableVideoInfo> getSelectedFilesInfo(
            List<TableVideoInfo> filesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting list of files to work with...");

        return filesInfo.stream().filter(TableVideoInfo::isSelected).collect(Collectors.toList());
    }

    public static ActionResult getSubtitleLoadingActionResult(
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
            success = Utils.getTextDependingOnCount(
                    loadedSuccessfullyCount,
                    "The subtitles have been loaded successfully",
                    "All %d subtitles have been loaded successfully"
            );
        } else if (failedToLoadCount == subtitlesToLoadCount) {
            error = Utils.getTextDependingOnCount(
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
                    warn = Utils.getTextDependingOnCount(
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

    public static String failedToLoadReasonFrom(FfmpegException.Code code) {
        if (code == FfmpegException.Code.GENERAL_ERROR) {
            return "Fmpeg returned an error";
        } else {
            log.error("unexpected code: " + code);
            throw new IllegalStateException();
        }
    }

    public static String getProcessFileProgressMessage(int processedCount, int allFileCount, TableVideoInfo fileInfo) {
        String progressPrefix = allFileCount > 1
                ? String.format("%d/%d ", processedCount + 1, allFileCount) + " processing file "
                : "Processing file ";

        return progressPrefix + fileInfo.getFilePath() + "...";
    }
}
