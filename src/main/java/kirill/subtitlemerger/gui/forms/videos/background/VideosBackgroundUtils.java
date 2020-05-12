package kirill.subtitlemerger.gui.forms.videos.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.*;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.settings.Sort;
import kirill.subtitlemerger.logic.settings.SortBy;
import kirill.subtitlemerger.logic.settings.SortDirection;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.Videos;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOptionNotValidReason;
import kirill.subtitlemerger.logic.videos.entities.Video;
import kirill.subtitlemerger.logic.videos.entities.VideoNotValidReason;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class VideosBackgroundUtils {
    static final String INCORRECT_FORMAT = "The subtitles have an incorrect format";

    static List<Video> getVideos(List<File> files, Ffprobe ffprobe, BackgroundManager backgroundManager) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.updateProgress(0, files.size());

        List<Video> result = new ArrayList<>();

        int i = 0;
        for (File file : files) {
            backgroundManager.updateMessage("Getting the video info " + file.getName() + "...");

            if (file.isFile() && file.exists()) {
                result.add(Videos.getVideo(file, LogicConstants.ALLOWED_VIDEO_EXTENSIONS, ffprobe));
            }

            backgroundManager.updateProgress(i + 1, files.size());

            i++;
        }

        return result;
    }

    static List<TableVideo> tableVideosFrom(
            List<Video> videos,
            boolean showFullPath,
            boolean selectByDefault,
            TableWithVideos table,
            Settings settings,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();

        List<TableVideo> result = new ArrayList<>();

        for (Video video : videos) {
            backgroundManager.updateMessage("Creating an object for " + video.getFile().getName() + "...");

            result.add(tableVideoFrom(video, showFullPath, selectByDefault, table, settings));
        }

        return result;
    }

    private static TableVideo tableVideoFrom(
            Video video,
            boolean showFullPath,
            boolean selected,
            TableWithVideos table,
            Settings settings
    ) {
        String pathToDisplay = showFullPath ? video.getFile().getAbsolutePath() : video.getFile().getName();

        TableVideo result = new TableVideo(
                video.getId(),
                table,
                selected,
                pathToDisplay,
                video.getSize(),
                video.getLastModified(),
                getTextualReason(video.getNotValidReason(), video.getFormat()),
                video.getFormat(),
                null
        );

        List<TableSubtitleOption> options = new ArrayList<>();
        if (!CollectionUtils.isEmpty(video.getBuiltInOptions())) {
            boolean hasUpperLanguage = video.getBuiltInOptions().stream()
                    .anyMatch(option -> Utils.languagesEqual(option.getLanguage(), settings.getUpperLanguage()));
            boolean hasLowerLanguage = video.getBuiltInOptions().stream()
                    .anyMatch(option -> Utils.languagesEqual(option.getLanguage(), settings.getLowerLanguage()));
            boolean canHideOptions = hasUpperLanguage && hasLowerLanguage;

            options = video.getBuiltInOptions().stream()
                    .map(option -> tableOptionFrom(option, canHideOptions, result, settings))
                    .collect(Collectors.toList());
        }

        result.setOptions(options);

        return result;
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

    static TableSubtitleOption tableOptionFrom(
            BuiltInSubtitleOption option,
            boolean canHideOptions,
            TableVideo video,
            Settings settings
    ) {
        return TableSubtitleOption.createBuiltIn(
                option.getId(),
                video,
                getTextualReason(option.getNotValidReason(), option.getFormat()),
                getOptionTitle(option),
                canHideOptions && isOptionHideable(option, settings),
                option.isMerged(),
                option.getSize(),
                null,
                false,
                false
        );
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

    private static String getOptionTitle(BuiltInSubtitleOption stream) {
        if (stream.isMerged()) {
            String upperCode = stream.getMergedUpperCode().toUpperCase();
            if ("EXTERNAL".equals(upperCode)) {
                upperCode = "FILE";
            }
            String lowerCode = stream.getMergedLowerCode().toUpperCase();
            if ("EXTERNAL".equals(lowerCode)) {
                lowerCode = "FILE";
            }
            return upperCode + "+" + lowerCode + " merged subtitles";
        } else {
            String result = Utils.languageToString(stream.getLanguage()).toUpperCase();

            if (!StringUtils.isBlank(stream.getTitle())) {
                result += " (" + stream.getTitle() + ")";
            }

            return result;
        }
    }

    private static boolean isOptionHideable(BuiltInSubtitleOption option, Settings settings) {
        LanguageAlpha3Code optionLanguage = option.getLanguage();

        return !Utils.languagesEqual(optionLanguage, settings.getUpperLanguage())
                && !Utils.languagesEqual(optionLanguage, settings.getLowerLanguage());
    }

    public static List<TableVideo> getSortedVideos(
            List<TableVideo> unsortedVideos,
            Sort sort,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Sorting the videos...");

        Comparator<TableVideo> comparator;
        switch (sort.getSortBy()) {
            case NAME:
                comparator = Comparator.comparing(TableVideo::getFilePath);
                break;
            case MODIFICATION_TIME:
                comparator = Comparator.comparing(TableVideo::getLastModified);
                break;
            case SIZE:
                comparator = Comparator.comparing(TableVideo::getSize);
                break;
            default:
                log.error("unexpected sortBy value: " + sort.getSortBy() + ", most likely a bug");
                throw new IllegalStateException();
        }

        if (sort.getSortDirection() == SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        return unsortedVideos.stream().sorted(comparator).collect(Collectors.toList());
    }

    public static TableData getTableData(
            List<TableVideo> allVideos,
            boolean hideUnavailable,
            TableMode mode,
            Sort sort,
            BackgroundManager backgroundManager
    ) {
        List<TableVideo> videosToShow = getVideosToShow(allVideos, hideUnavailable, backgroundManager);

        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();

        int selectableCount = 0;
        int selectedAvailableCount = 0;
        int selectedUnavailableCount = 0;

        backgroundManager.updateMessage("Calculating the number of videos...");
        for (TableVideo video : videosToShow) {
            if (video.isSelected()) {
                if (StringUtils.isBlank(video.getNotValidReason())) {
                    selectedAvailableCount++;
                } else {
                    selectedUnavailableCount++;
                }
            }

            if (mode == TableMode.SEPARATE_VIDEOS) {
                selectableCount++;
            } else if (mode == TableMode.WHOLE_DIRECTORY) {
                if (StringUtils.isBlank(video.getNotValidReason())) {
                    selectableCount++;
                }
            } else {
                log.error("unexpected mode " + mode + ", most likely a bug");
                throw new IllegalStateException();
            }
        }

        return new TableData(
                videosToShow,
                mode,
                selectableCount,
                selectedAvailableCount,
                selectedUnavailableCount,
                getTableSortBy(sort.getSortBy()),
                getTableSortDirection(sort.getSortDirection())
        );
    }

    private static List<TableVideo> getVideosToShow(
            List<TableVideo> allVideos,
            boolean hideUnavailable,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting videos to show...");

        if (!hideUnavailable) {
            return allVideos;
        } else {
            return allVideos.stream()
                    .filter(video -> StringUtils.isBlank(video.getNotValidReason()))
                    .collect(Collectors.toList());
        }
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

    /**
     * Returns the progress counters and the uncapitalized action title if the number of iterations is more than one or
     * the original action title otherwise.
     *
     * getProgressTitle(0, 5, "Processing the video") = "1/5 processing the video"
     * getProgressTitle(0, 1, "Processing the video") = "Processing the video"
     */
    public static String getProgressAction(int processed, int toProcessCount, String action) {
        if (toProcessCount == 1) {
            return action;
        } else {
            return (processed + 1) + "/" + toProcessCount + " " + StringUtils.uncapitalize(action);
        }
    }

    public static LoadSubtitlesResult loadSubtitles(
            BuiltInSubtitleOption option,
            Video video,
            TableSubtitleOption tableOption,
            Ffmpeg ffmpeg
    ) throws InterruptedException {
        try {
            String subtitleText = ffmpeg.getSubtitleText(option.getFfmpegIndex(), option.getFormat(), video.getFile());
            SubtitlesAndInput subtitlesAndInput = SubtitlesAndInput.from(
                    subtitleText.getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );

            option.setSubtitlesAndInput(subtitlesAndInput);
            if (subtitlesAndInput.isCorrectFormat()) {
                Platform.runLater(() -> tableOption.loadedSuccessfully(subtitlesAndInput.getSize(), null));
                return LoadSubtitlesResult.SUCCESS;
            } else {
                Platform.runLater(() -> tableOption.loadedSuccessfully(subtitlesAndInput.getSize(), INCORRECT_FORMAT));
                return LoadSubtitlesResult.INCORRECT_FORMAT;
            }
        } catch (FfmpegException e) {
            log.warn("failed to get subtitle text: " + e.getCode() + ", console output " + e.getConsoleOutput());
            Platform.runLater(() -> tableOption.failedToLoad(failedToLoadReasonFrom(e.getCode())));
            return LoadSubtitlesResult.FAILED;
        }
    }

    static String failedToLoadReasonFrom(FfmpegException.Code code) {
        if (code == FfmpegException.Code.GENERAL_ERROR) {
            return "Fmpeg returned an error";
        } else {
            log.error("unexpected ffmpeg code when loading subtitles: " + code + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    static void clearActionResults(List<TableVideo> videos, BackgroundManager backgroundManager) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Clearing state...");

        for (TableVideo video : videos) {
            Platform.runLater(video::clearActionResult);
        }
    }

    static List<TableVideo> getSelectedVideos(List<TableVideo> videos, BackgroundManager backgroundManager) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting the list of videos to work with...");

        return videos.stream().filter(TableVideo::isSelected).collect(Collectors.toList());
    }

    public static ActionResult getLoadSubtitlesResult(
            int toLoadCount,
            int processedCount,
            int successfulCount,
            int failedCount,
            int incorrectCount
    ) {
        String success = "";
        String warn = "";
        String error = "";

        if (toLoadCount == 0) {
            warn = "There are no subtitles to load";
        } else if (processedCount == 0) {
            warn = "The task has been cancelled, nothing was loaded";
        } else if (successfulCount == toLoadCount) {
            success = Utils.getTextDependingOnCount(
                    successfulCount,
                    "The subtitles have been loaded successfully",
                    "All %d subtitles have been loaded successfully"
            );
        } else if (failedCount == toLoadCount) {
            error = Utils.getTextDependingOnCount(
                    failedCount,
                    "Failed to load the subtitles",
                    "Failed to load all %d subtitles"
            );
        } else if (incorrectCount == toLoadCount) {
            error = Utils.getTextDependingOnCount(
                    incorrectCount,
                    "The subtitles have an incorrect format",
                    "All %d subtitles have an incorrect format"
            );
        } else {
            if (successfulCount != 0) {
                success = String.format("%d/%d subtitles have been loaded successfully", successfulCount, toLoadCount);
            }

            if (processedCount != toLoadCount) {
                if (StringUtils.isBlank(success)) {
                    warn = Utils.getTextDependingOnCount(
                            toLoadCount - processedCount,
                            String.format("1/%d subtitle loadings has been cancelled", toLoadCount),
                            String.format("%%d/%d subtitle loadings have been cancelled", toLoadCount)
                    );
                } else {
                    warn = String.format("%d/%d cancelled", toLoadCount - processedCount, toLoadCount);
                }
            }

            if (failedCount != 0) {
                if (StringUtils.isBlank(success) && StringUtils.isBlank(warn)) {
                    error = String.format ("Failed to load %d/%d subtitles", failedCount, toLoadCount);
                } else {
                    error = String.format("failed to load %d/%d", failedCount, toLoadCount);
                }
            }

            if (incorrectCount != 0) {
                if (!StringUtils.isBlank(error)) {
                    error += ", ";
                }
                error += String.format("%d/%d have an incorrect format", incorrectCount, toLoadCount);
            }
        }

        return new ActionResult(success, warn, error);
    }

    static String getLoadSubtitlesError(int toLoadCount, int failedCount, int incorrectCount) {
        String result = "";

        if (toLoadCount == 1) {
            if (failedCount != 0) {
                result = "Failed to load the subtitles";
            } else if (incorrectCount != 0) {
                result = "The subtitles have an incorrect format";
            }
        } else {
            if (failedCount != 0) {
                result = String.format("Failed to load %d/%d subtitles", failedCount, toLoadCount);
            }

            if (incorrectCount != 0) {
                if (StringUtils.isBlank(result)) {
                    result = String.format("%d/%d subtitles have an incorrect format", incorrectCount, toLoadCount);
                } else {
                    result += ", " + String.format("%d/%d have an incorrect format", incorrectCount, toLoadCount);
                }
            }
        }

        return result;
    }
}
