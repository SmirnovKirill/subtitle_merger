package kirill.subtitlemerger.gui.forms.videos.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.*;

@CommonsLog
@AllArgsConstructor
public class AutoSelectRunner implements BackgroundRunner<ActionResult> {
    private List<TableVideo> tableVideos;

    private List<Video> videos;

    private Ffmpeg ffmpeg;

    private Settings settings;

    @Override
    public ActionResult run(BackgroundManager backgroundManager) {
        clearActionResults(tableVideos, backgroundManager);

        List<TableVideo> selectedVideos = getSelectedVideos(tableVideos, backgroundManager);
        int toProcessCount = selectedVideos.size();
        int processedCount = 0;
        int successfulCount = 0;
        int notPossibleCount = 0;
        int failedCount = 0;

        backgroundManager.setCancelPossible(true);
        backgroundManager.setCancelDescription("Please be patient, this may take a while depending on the video.");
        backgroundManager.setIndeterminateProgress();
        try {
            for (TableVideo tableVideo : selectedVideos) {
                Video video = Video.getById(tableVideo.getId(), videos);

                String actionPrefix = getProgressAction(processedCount, toProcessCount, "Auto-selection: ");
                backgroundManager.updateMessage(actionPrefix + "processing " + video.getFile() + "...");

                List<BuiltInSubtitleOption> optionsToLoad = getOptionsToLoad(video, settings);
                boolean success = loadSubtitles(
                        optionsToLoad,
                        video,
                        tableVideo,
                        actionPrefix,
                        ffmpeg,
                        backgroundManager
                );
                if (!success) {
                    failedCount++;
                    processedCount++;
                    continue;
                }

                String notPossibleMessage = autoSelectOptions(video, tableVideo, settings);
                if (!StringUtils.isBlank(notPossibleMessage)) {
                    Platform.runLater(() -> tableVideo.setOnlyWarn(notPossibleMessage));
                    notPossibleCount++;
                } else {
                    successfulCount++;
                }

                processedCount++;
            }
        } catch (InterruptedException e) {
            /* Do nothing here, will just return the result based on the work done. */
        }

        return getActionResult(toProcessCount, processedCount, successfulCount, notPossibleCount, failedCount);
    }

    private static List<BuiltInSubtitleOption> getOptionsToLoad(Video video, Settings settings) {
        List<BuiltInSubtitleOption> selectableUpperOptions = getSelectableOptions(video, settings.getUpperLanguage());
        List<BuiltInSubtitleOption> selectableLowerOptions = getSelectableOptions(video, settings.getLowerLanguage());
        /*
         * If there are no options for at least one language than the auto-selection won't work so there is no point
         * in loading any subtitles.
         */
        if (CollectionUtils.isEmpty(selectableUpperOptions) || CollectionUtils.isEmpty(selectableLowerOptions)) {
            return new ArrayList<>();
        }

        List<BuiltInSubtitleOption> upperOptionsToLoad = selectableUpperOptions.stream()
                .filter(option -> option.getSubtitlesAndInput() == null)
                .collect(Collectors.toList());
        List<BuiltInSubtitleOption> lowerOptionsToLoad = selectableLowerOptions.stream()
                .filter(option -> option.getSubtitlesAndInput() == null)
                .collect(Collectors.toList());

        List<BuiltInSubtitleOption> result = new ArrayList<>();

        /*
         * If there is just one option for the language then it shouldn't be loaded because it's the only one and will
         * be auto-selected anyway.
         */
        if (selectableUpperOptions.size() > 1) {
            result.addAll(upperOptionsToLoad);
        }
        if (selectableLowerOptions.size() > 1) {
            result.addAll(lowerOptionsToLoad);
        }

        return result;
    }

    private static List<BuiltInSubtitleOption> getSelectableOptions(Video video, LanguageAlpha3Code language) {
        return video.getBuiltInOptions().stream()
                .filter(option -> option.getLanguage() == language)
                .filter(option -> !option.isMerged())
                .filter(option -> option.getNotValidReason() == null)
                .collect(Collectors.toList());
    }

    private static boolean loadSubtitles(
            List<BuiltInSubtitleOption> optionsToLoad,
            Video video,
            TableVideo tableVideo,
            String actionPrefix,
            Ffmpeg ffmpeg,
            BackgroundManager backgroundManager
    ) throws InterruptedException {
        int toLoadCount = optionsToLoad.size();
        int failedCount = 0;
        int incorrectCount = 0;

        for (BuiltInSubtitleOption option : optionsToLoad) {
            TableSubtitleOption tableOption = tableVideo.getOption(option.getId());

            String action = actionPrefix + "loading " + tableOption.getTitle() + " in " + video.getFile().getName()
                    + "...";
            backgroundManager.updateMessage(action);

            LoadSubtitlesResult loadResult = VideosBackgroundUtils.loadSubtitles(option, video, tableOption, ffmpeg);
            if (loadResult == LoadSubtitlesResult.FAILED) {
                failedCount++;
            } else if (loadResult == LoadSubtitlesResult.INCORRECT_FORMAT) {
                incorrectCount++;
            }

            if (failedCount != 0 || incorrectCount != 0) {
                String error = "Auto-selection is not possible: "
                        + StringUtils.uncapitalize(getLoadSubtitlesError(toLoadCount, failedCount, incorrectCount));
                Platform.runLater(() -> tableVideo.setOnlyError(error));
            }
        }

        return failedCount == 0 && incorrectCount == 0;
    }

    @Nullable
    private static String autoSelectOptions(Video video, TableVideo tableVideo, Settings settings) {
        if (CollectionUtils.isEmpty(video.getOptions())) {
            return "Auto-selection is not possible because there are no subtitles";
        }

        List<BuiltInSubtitleOption> selectableUpperOptions = getSelectableOptions(video, settings.getUpperLanguage());
        List<BuiltInSubtitleOption> selectableLowerOptions = getSelectableOptions(video, settings.getLowerLanguage());

        String missingLanguages = getMissingLanguages(selectableUpperOptions, selectableLowerOptions, settings);
        if (!StringUtils.isBlank(missingLanguages)) {
            return "Auto-selection is not possible because there are no " + missingLanguages + " subtitles";
        }

        BuiltInSubtitleOption upperOption = getMatchingOption(selectableUpperOptions);
        TableSubtitleOption tableUpperOption = tableVideo.getOption(upperOption.getId());
        Platform.runLater(() -> tableUpperOption.setSelectedAsUpper(true));

        BuiltInSubtitleOption lowerOption = getMatchingOption(selectableLowerOptions);
        TableSubtitleOption tableLowerOption = tableVideo.getOption(lowerOption.getId());
        Platform.runLater(() -> tableLowerOption.setSelectedAsLower(true));

        return null;
    }

    @Nullable
    private static String getMissingLanguages(
            List<BuiltInSubtitleOption> selectableUpperOptions,
            List<BuiltInSubtitleOption> selectableLowerOptions,
            Settings settings
    ) {
        List<String> missingLanguages = new ArrayList<>();

        if (CollectionUtils.isEmpty(selectableUpperOptions)) {
            missingLanguages.add(Utils.languageToString(settings.getUpperLanguage()).toUpperCase());
        }
        if (CollectionUtils.isEmpty(selectableLowerOptions)) {
            missingLanguages.add(Utils.languageToString(settings.getLowerLanguage()).toUpperCase());
        }

        if (CollectionUtils.isEmpty(missingLanguages)) {
            return null;
        }

        return StringUtils.join(missingLanguages, "and");
    }

    private static BuiltInSubtitleOption getMatchingOption(List<BuiltInSubtitleOption> selectableOptions) {
        if (selectableOptions.size() == 1) {
            return selectableOptions.get(0);
        } else {
            /*
             * If there is more than one option then they all are loaded at this point. The extra check for nullness
             * is here just so that there are no warnings because of the comparator.
             */
            BuiltInSubtitleOption result = selectableOptions.stream()
                    .filter(option -> option.getSubtitlesAndInput() != null)
                    .max(Comparator.comparing(option -> option.getSubtitlesAndInput().getSize()))
                    .orElse(null);
            if (result == null) {
                log.error("can't find option with the biggest size, most likely a bug");
                throw new IllegalStateException();
            }

            return result;
        }
    }

    static ActionResult getActionResult(
            int toProcessCount,
            int processedCount,
            int successfulCount,
            int notPossibleCount,
            int failedCount
    ) {
        String success = "";
        String warn = "";
        String error = "";

        if (processedCount == 0) {
            warn = "The task has been cancelled, nothing was done";
        } else if (successfulCount == toProcessCount) {
            success = Utils.getTextDependingOnCount(
                    successfulCount,
                    "Auto-selection has finished successfully for the video",
                    "Auto-selection has finished successfully for all %d videos"
            );
        } else if (notPossibleCount == toProcessCount) {
            warn = Utils.getTextDependingOnCount(
                    notPossibleCount,
                    "Auto-selection is not possible for the video",
                    "Auto-selection is not possible for all %d videos"
            );
        } else if (failedCount == toProcessCount) {
            error = Utils.getTextDependingOnCount(
                    failedCount,
                    "Auto-selection has failed for the video",
                    "Auto-selection has failed for all %d videos"
            );
        } else {
            if (successfulCount != 0) {
                success = String.format(
                        "Auto-selection has finished for %d/%d videos successfully",
                        successfulCount,
                        toProcessCount
                );
            }

            if (processedCount != toProcessCount) {
                if (StringUtils.isBlank(success)) {
                    warn = String.format(
                            "Auto-selection has been cancelled for %d/%d videos",
                            toProcessCount - processedCount,
                            toProcessCount
                    );
                } else {
                    warn = String.format("cancelled for %d/%d", toProcessCount - processedCount, toProcessCount);
                }
            }

            if (notPossibleCount != 0) {
                if (StringUtils.isBlank(success) && StringUtils.isBlank(warn)) {
                    warn = String.format(
                            "Auto-selection is not possible for %d/%d videos",
                            notPossibleCount,
                            toProcessCount
                    );
                } else {
                    if (!StringUtils.isBlank(warn)) {
                        warn += ", ";
                    }
                    warn += String.format("not possible for %d/%d", notPossibleCount, toProcessCount);

                }
            }

            if (failedCount != 0) {
                error = String.format("failed for %d/%d", failedCount, toProcessCount);
            }
        }

        return new ActionResult(success, warn, error);
    }
}