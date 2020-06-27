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
import kirill.subtitlemerger.logic.utils.entities.MultiPartActionResult;
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
public class AutoSelectRunner implements BackgroundRunner<MultiPartActionResult> {
    private List<TableVideo> tableVideos;

    private List<Video> videos;

    private Ffmpeg ffmpeg;

    private Settings settings;

    @Override
    public MultiPartActionResult run(BackgroundManager backgroundManager) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();

        clearActionResults(tableVideos, backgroundManager);

        List<TableVideo> selectedVideos = getSelectedVideos(tableVideos, backgroundManager);
        int toProcessCount = selectedVideos.size();
        int processedCount = 0;
        int successfulCount = 0;
        int notPossibleCount = 0;
        int failedCount = 0;

        backgroundManager.setCancelPossible(true);
        try {
            for (TableVideo tableVideo : selectedVideos) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                Video video = Video.getById(tableVideo.getId(), videos);

                backgroundManager.setCancelDescription(null);
                String actionPrefix = getProgressAction(processedCount, toProcessCount, "Auto-selection: ");
                backgroundManager.updateMessage(actionPrefix + "processing " + video.getFile().getName() + "...");

                boolean success = loadSubtitles(
                        video,
                        tableVideo,
                        actionPrefix,
                        ffmpeg,
                        settings,
                        backgroundManager
                );
                if (!success) {
                    failedCount++;
                    processedCount++;
                    continue;
                }

                String notPossibleMessage = autoSelectOptions(video, tableVideo, settings);
                if (!StringUtils.isBlank(notPossibleMessage)) {
                    Platform.runLater(() -> tableVideo.setOnlyWarning(notPossibleMessage));
                    notPossibleCount++;
                } else {
                    successfulCount++;
                }

                processedCount++;
            }
        } catch (InterruptedException e) {
            /* Do nothing here, will just return a result based on the work done. */
        }

        return getActionResult(toProcessCount, processedCount, successfulCount, notPossibleCount, failedCount);
    }

    private static boolean loadSubtitles(
            Video video,
            TableVideo tableVideo,
            String actionPrefix,
            Ffmpeg ffmpeg,
            Settings settings,
            BackgroundManager backgroundManager
    ) throws InterruptedException {
        List<BuiltInSubtitleOption> optionsToLoad = getOptionsToLoad(video, settings);
        if (CollectionUtils.isEmpty(optionsToLoad)) {
            return true;
        }

        backgroundManager.setCancelDescription(getLoadingCancelDescription(video));

        int toLoadCount = optionsToLoad.size();
        int incorrectCount = 0;
        int failedCount = 0;
        for (BuiltInSubtitleOption option : optionsToLoad) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            TableSubtitleOption tableOption = tableVideo.getOption(option.getId());

            String action = actionPrefix + "loading " + tableOption.getTitle() + " in " + video.getFile().getName()
                    + "...";
            backgroundManager.updateMessage(action);

            LoadSubtitlesResult loadResult = VideosBackgroundUtils.loadSubtitles(option, video, tableOption, ffmpeg);
            if (loadResult == LoadSubtitlesResult.INCORRECT_FORMAT) {
                incorrectCount++;
            } else if (loadResult == LoadSubtitlesResult.FAILED) {
                failedCount++;
            }

            if (failedCount != 0 || incorrectCount != 0) {
                String error = "Auto-selecting is not possible: "
                        + StringUtils.uncapitalize(getLoadSubtitlesError(toLoadCount, failedCount, incorrectCount));
                Platform.runLater(() -> tableVideo.setOnlyError(error));
            }
        }

        backgroundManager.setCancelDescription(null);

        return failedCount == 0 && incorrectCount == 0;
    }

    private static List<BuiltInSubtitleOption> getOptionsToLoad(Video video, Settings settings) {
        List<BuiltInSubtitleOption> selectableUpperOptions = getSelectableOptions(video, settings.getUpperLanguage());
        List<BuiltInSubtitleOption> selectableLowerOptions = getSelectableOptions(video, settings.getLowerLanguage());
        /*
         * If there are no options for at least one language than auto-selecting won't work so there is no point in
         * loading any subtitles.
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

    @Nullable
    private static String autoSelectOptions(Video video, TableVideo tableVideo, Settings settings) {
        if (CollectionUtils.isEmpty(video.getOptions())) {
            return "Auto-selecting is not possible because there are no subtitles";
        }

        List<BuiltInSubtitleOption> selectableUpperOptions = getSelectableOptions(video, settings.getUpperLanguage());
        List<BuiltInSubtitleOption> selectableLowerOptions = getSelectableOptions(video, settings.getLowerLanguage());

        String missingLanguages = getMissingLanguages(selectableUpperOptions, selectableLowerOptions, settings);
        if (!StringUtils.isBlank(missingLanguages)) {
            return "Auto-selecting is not possible because there are no " + missingLanguages + " subtitles";
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

        return StringUtils.join(missingLanguages, " and ");
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

    static MultiPartActionResult getActionResult(
            int toProcessCount,
            int processedCount,
            int successfulCount,
            int notPossibleCount,
            int failedCount
    ) {
        String success = null;
        String warning = null;
        String error = null;

        int canceled = toProcessCount - processedCount;
        if (processedCount == 0) {
            warning = "The task has been canceled, nothing was done";
        } else if (successfulCount == toProcessCount) {
            success = Utils.getTextDependingOnCount(
                    successfulCount,
                    "Auto-selecting has finished successfully for the video",
                    "Auto-selecting has finished successfully for all %d videos"
            );
        } else if (notPossibleCount == toProcessCount) {
            warning = Utils.getTextDependingOnCount(
                    notPossibleCount,
                    "Auto-selecting is not possible for the video",
                    "Auto-selecting is not possible for all %d videos"
            );
        } else if (failedCount == toProcessCount) {
            error = Utils.getTextDependingOnCount(
                    failedCount,
                    "Auto-selecting has failed for the video",
                    "Auto-selecting has failed for all %d videos"
            );
        } else {
            if (successfulCount != 0) {
                success = String.format(
                        "Auto-selecting has finished for %d/%d videos successfully",
                        successfulCount,
                        toProcessCount
                );
            }

            /*
             * If there is a phrase "is not possible" and no present perfect before, we should put a phrase with present
             * perfect after that once.
             */
            boolean problemsWithPresentTense = false;
            if (notPossibleCount != 0) {
                if (StringUtils.isBlank(success)) {
                    warning = String.format(
                            "Auto-selecting is not possible for %d/%d videos",
                            notPossibleCount,
                            toProcessCount
                    );
                    problemsWithPresentTense = true;
                } else {
                    warning = String.format("is not possible for %d/%d", notPossibleCount, toProcessCount);
                }
            }

            if (canceled != 0) {
                if (StringUtils.isBlank(success) && StringUtils.isBlank(warning)) {
                    warning = String.format(
                            "Auto-selecting has been canceled for %d/%d videos",
                            canceled,
                            toProcessCount
                    );
                } else {
                    if (!StringUtils.isBlank(warning)) {
                        warning += ", ";
                    } else {
                        warning = ""; // To prevent further concatenation with null.
                    }
                    if (problemsWithPresentTense) {
                        warning += String.format("has been canceled for %d/%d", canceled, toProcessCount);
                        problemsWithPresentTense = false;
                    } else {
                        warning += String.format("canceled for %d/%d", canceled, toProcessCount);
                    }
                }
            }

            if (failedCount != 0) {
                if (problemsWithPresentTense) {
                    error = String.format("has failed for %d/%d", failedCount, toProcessCount);
                } else {
                    error = String.format("failed for %d/%d", failedCount, toProcessCount);
                }
            }
        }

        return new MultiPartActionResult(success, warning, error);
    }
}