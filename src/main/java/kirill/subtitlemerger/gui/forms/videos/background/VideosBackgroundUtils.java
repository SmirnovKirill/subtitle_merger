package kirill.subtitlemerger.gui.forms.videos.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.*;
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
import kirill.subtitlemerger.logic.videos.entities.Video;
import kirill.subtitlemerger.logic.videos.entities.VideoNotValidReason;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class VideosBackgroundUtils {
    static final String FAILED_TO_LOAD_INCORRECT_FORMAT = "Subtitles seem to have an incorrect format";

    static List<Video> getVideos(List<File> files, Ffprobe ffprobe, BackgroundManager backgroundManager) {
        backgroundManager.setCancellationPossible(false);
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
        backgroundManager.setCancellationPossible(false);
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

        List<TableSubtitleOption> subtitleOptions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(video.getBuiltInSubtitleOptions())) {
            boolean hasUpperLanguage = video.getBuiltInSubtitleOptions().stream()
                    .anyMatch(option -> Utils.languagesEqual(option.getLanguage(), settings.getUpperLanguage()));
            boolean hasLowerLanguage = video.getBuiltInSubtitleOptions().stream()
                    .anyMatch(option -> Utils.languagesEqual(option.getLanguage(), settings.getLowerLanguage()));
            boolean canHideOptions = hasUpperLanguage && hasLowerLanguage;

            subtitleOptions = video.getBuiltInSubtitleOptions().stream()
                    .map(option -> tableSubtitleOptionFrom(option, canHideOptions, result, settings))
                    .collect(Collectors.toList());
        }

        result.setSubtitleOptions(subtitleOptions);

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

    static TableSubtitleOption tableSubtitleOptionFrom(
            BuiltInSubtitleOption subtitleOption,
            boolean canHideOptions,
            TableVideo video,
            Settings settings
    ) {
        return TableSubtitleOption.createBuiltIn(
                subtitleOption.getId(),
                video,
                getTextualReason(subtitleOption.getNotValidReason(), subtitleOption.getFormat()),
                getOptionTitle(subtitleOption),
                canHideOptions && isOptionHideable(subtitleOption, settings),
                subtitleOption.isMerged(),
                subtitleOption.getSize(),
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

    private static boolean isOptionHideable(BuiltInSubtitleOption subtitleOption, Settings settings) {
        LanguageAlpha3Code optionLanguage = subtitleOption.getLanguage();

        return !Utils.languagesEqual(optionLanguage, settings.getUpperLanguage())
                && !Utils.languagesEqual(optionLanguage, settings.getLowerLanguage());
    }

    public static List<TableVideo> getOnlyValidVideos(List<TableVideo> allVideos, BackgroundManager backgroundManager) {
        backgroundManager.setCancellationPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Filtering unavailable...");

        return allVideos.stream()
                .filter(video -> StringUtils.isBlank(video.getNotValidReason()))
                .collect(Collectors.toList());
    }

    public static List<TableVideo> getSortedVideos(
            List<TableVideo> unsortedVideos,
            SortBy sortBy,
            SortDirection sortDirection,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setCancellationPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Sorting the video list...");

        Comparator<TableVideo> comparator;
        switch (sortBy) {
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
                log.error("unexpected sortBy value: " + sortBy + ", most likely a bug");
                throw new IllegalStateException();
        }

        if (sortDirection == SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        return unsortedVideos.stream().sorted(comparator).collect(Collectors.toList());
    }

    public static TableData getTableData(
            TableMode mode,
            List<TableVideo> videos,
            SortBy sortBy,
            SortDirection sortDirection,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setCancellationPossible(false);
        backgroundManager.setIndeterminateProgress();

        int selectableCount = 0;
        int selectedAvailableCount = 0;
        int selectedUnavailableCount = 0;

        backgroundManager.updateMessage("Calculating the number of videos...");
        for (TableVideo video : videos) {
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
                mode,
                videos,
                selectableCount,
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

    static String getLoadSubtitlesProgressMessage(
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

    static void clearActionResults(List<TableVideo> videos, BackgroundManager backgroundManager) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Clearing state...");

        for (TableVideo video : videos) {
            Platform.runLater(video::clearActionResult);
        }
    }

    static List<TableVideo> getSelectedVideos(List<TableVideo> allVideos, BackgroundManager backgroundManager) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting list of videos to work with...");

        return allVideos.stream().filter(TableVideo::isSelected).collect(Collectors.toList());
    }

    static ActionResult getSubtitleLoadingActionResult(
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

    static String failedToLoadReasonFrom(FfmpegException.Code code) {
        if (code == FfmpegException.Code.GENERAL_ERROR) {
            return "Fmpeg returned an error";
        } else {
            log.error("unexpected ffmpeg code when loading subtitles: " + code + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    static String getProcessFileProgressMessage(int processedCount, int allFileCount, TableVideo video) {
        String progressPrefix = allFileCount > 1
                ? String.format("%d/%d ", processedCount + 1, allFileCount) + " processing file "
                : "Processing file ";

        return progressPrefix + video.getFilePath() + "...";
    }
}
