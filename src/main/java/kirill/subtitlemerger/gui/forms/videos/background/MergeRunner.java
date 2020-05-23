package kirill.subtitlemerger.gui.forms.videos.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegInjectInfo;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.ffmpeg.json.JsonFfprobeVideoInfo;
import kirill.subtitlemerger.logic.settings.MergeMode;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.subtitles.SubtitleMerger;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndOutput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.Videos;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.ExternalSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.*;

@CommonsLog
@AllArgsConstructor
public class MergeRunner implements BackgroundRunner<ActionResult> {
    private List<TableVideo> tableVideos;

    private List<Video> videos;

    private List<File> confirmedFilesToOverwrite;

    private File largestFreeSpaceDirectory;

    private Ffprobe ffprobe;

    private Ffmpeg ffmpeg;

    private Settings settings;

    public MergeRunner(
            List<TableVideo> tableVideos,
            List<Video> videos,
            List<File> confirmedFilesToOverwrite,
            File largestFreeSpaceDirectory,
            GuiContext context
    ) {
        this.tableVideos = tableVideos;
        this.videos = videos;
        this.confirmedFilesToOverwrite = confirmedFilesToOverwrite;
        this.largestFreeSpaceDirectory = largestFreeSpaceDirectory;
        this.ffprobe = context.getFfprobe();
        this.ffmpeg = context.getFfmpeg();
        this.settings = context.getSettings();
    }

    @Override
    public ActionResult run(BackgroundManager backgroundManager) {
        int toProcessCount = tableVideos.size();
        int processedCount = 0;
        int successfulCount = 0;
        int noConfirmationCount = 0;
        int alreadyMergedCount = 0;
        int failedCount = 0;

        try {
            for (TableVideo tableVideo : tableVideos) {
                Video video = Video.getById(tableVideo.getId(), videos);
                String actionPrefix = getProgressAction(processedCount, toProcessCount, "Merge: ");

                IterationResult iterationResult = processVideo(tableVideo, video, actionPrefix, backgroundManager);
                if (iterationResult == IterationResult.NO_CONFIRMATION) {
                    noConfirmationCount++;
                } else if (iterationResult == IterationResult.FAILED) {
                    failedCount++;
                } else if (iterationResult == IterationResult.ALREADY_MERGED) {
                    alreadyMergedCount++;
                } else if (iterationResult == IterationResult.SUCCESS) {
                    successfulCount++;
                } else {
                    log.error("unexpected iteration result: " + iterationResult + ", most likely a bug");
                    throw new IllegalStateException();
                }

                processedCount++;
            }
        } catch (InterruptedException e) {
            /* Do nothing here, will just return the result based on the work done. */
        }

        return getActionResult(
                toProcessCount,
                processedCount,
                successfulCount,
                noConfirmationCount,
                alreadyMergedCount,
                failedCount
        );
    }

    private IterationResult processVideo(
            TableVideo tableVideo,
            Video video,
            String actionPrefix,
            BackgroundManager backgroundManager

    ) throws InterruptedException {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage(actionPrefix + "processing " + video.getFile() + "...");

        SubtitleOption upperOption = video.getOption(tableVideo.getUpperOption().getId());
        SubtitleOption lowerOption = video.getOption(tableVideo.getLowerOption().getId());

        if (noConfirmation(video, upperOption, lowerOption, confirmedFilesToOverwrite, settings)) {
            String message = "Merging is unavailable because you need to confirm the file overwriting";
            Platform.runLater(() -> tableVideo.setOnlyWarn(message));
            return IterationResult.NO_CONFIRMATION;
        }

        List<BuiltInSubtitleOption> optionsToLoad = getOptionsToLoad(video, upperOption, lowerOption, settings);
        boolean success = loadSubtitles(optionsToLoad, video, tableVideo, actionPrefix, ffmpeg, backgroundManager);
        if (!success) {
            return IterationResult.FAILED;
        }

        SubtitlesAndOutput merged = getMergedSubtitles(
                upperOption,
                lowerOption,
                actionPrefix,
                settings,
                backgroundManager
        );
        if (isDuplicate(merged, video, settings)) {
            String message = "Selected subtitles have already been merged";
            Platform.runLater(() -> tableVideo.setOnlyWarn(message));
            return IterationResult.ALREADY_MERGED;
        }

        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        if (settings.getMergeMode() == MergeMode.ORIGINAL_VIDEOS) {
            backgroundManager.updateMessage(actionPrefix + "injecting the result to the video...");
            return injectToVideo(video, upperOption, lowerOption, merged, tableVideo);
        } else if (settings.getMergeMode() == MergeMode.SEPARATE_SUBTITLE_FILES) {
            backgroundManager.updateMessage(actionPrefix + "writing the result to the file...");
            return saveToSubtitleFile(video, upperOption, lowerOption, merged, tableVideo);
        } else {
            log.error("unexpected merge mode: " + settings.getMergeMode() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private static boolean noConfirmation(
            Video video,
            SubtitleOption upperOption,
            SubtitleOption lowerOption,
            List<File> confirmedFilesToOverwrite,
            Settings settings
    ) {
        if (settings.getMergeMode() != MergeMode.SEPARATE_SUBTITLE_FILES) {
            return false;
        }

        File subtitleFile = new File(Utils.getMergedSubtitleFilePath(video, upperOption, lowerOption));
        return subtitleFile.exists() && !confirmedFilesToOverwrite.contains(subtitleFile);
    }

    private static List<BuiltInSubtitleOption> getOptionsToLoad(
            Video video,
            SubtitleOption upperOption,
            SubtitleOption lowerOption,
            Settings settings
    ) {
        List<SubtitleOption> notFilteredOptions = new ArrayList<>();

        notFilteredOptions.add(upperOption);
        notFilteredOptions.add(lowerOption);

        /*
         * When injecting into original videos we should load not only the selected options but also the merged ones in
         * order to check for duplicates later.
         */
        if (settings.getMergeMode() == MergeMode.ORIGINAL_VIDEOS) {
            notFilteredOptions.addAll(
                    video.getBuiltInOptions().stream()
                            .filter(BuiltInSubtitleOption::isMerged)
                            .collect(toList())
            );
        }

        return notFilteredOptions.stream()
                .filter(option -> option instanceof BuiltInSubtitleOption)
                .map(BuiltInSubtitleOption.class::cast)
                .filter(option -> option.getSubtitlesAndInput() == null)
                .filter(option -> option.isMerged() || option.getNotValidReason() == null)
                .collect(toList());
    }

    private static boolean loadSubtitles(
            List<BuiltInSubtitleOption> optionsToLoad,
            Video video,
            TableVideo tableVideo,
            String actionPrefix,
            Ffmpeg ffmpeg,
            BackgroundManager backgroundManager
    ) throws InterruptedException {
        if (CollectionUtils.isEmpty(optionsToLoad)) {
            return true;
        }

        backgroundManager.setCancelPossible(true);
        backgroundManager.setCancelDescription("Please be patient, this may take a while depending on the video.");
        backgroundManager.setIndeterminateProgress();

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
                String error = "Merging is not possible: "
                        + StringUtils.uncapitalize(getLoadSubtitlesError(toLoadCount, failedCount, incorrectCount));
                Platform.runLater(() -> tableVideo.setOnlyError(error));
            }
        }

        return failedCount == 0 && incorrectCount == 0;
    }

    private static SubtitlesAndOutput getMergedSubtitles(
            SubtitleOption upperOption,
            SubtitleOption lowerOption,
            String actionPrefix,
            Settings settings,
            BackgroundManager backgroundManager
    ) throws InterruptedException {
        backgroundManager.setCancelPossible(true);
        backgroundManager.setCancelDescription(null);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage(actionPrefix + "merging subtitles...");

        Subtitles subtitles = SubtitleMerger.mergeSubtitles(upperOption.getSubtitles(), lowerOption.getSubtitles());
        return SubtitlesAndOutput.from(subtitles, settings.isPlainTextSubtitles());
    }

    private static boolean isDuplicate(SubtitlesAndOutput merged, Video video, Settings settings) {
        if (settings.getMergeMode() != MergeMode.ORIGINAL_VIDEOS) {
            return false;
        }

        for (BuiltInSubtitleOption option : video.getBuiltInOptions()) {
            if (!option.isMerged()) {
                continue;
            }

            SubtitlesAndInput optionSubtitles = option.getSubtitlesAndInput();
            if (optionSubtitles == null) {
                log.error("option subtitles are null, shouldn't have gotten here, most likely a bug");
                throw new IllegalStateException();
            }

            String optionSubtitleText = new String(optionSubtitles.getRawData(), optionSubtitles.getEncoding());
            if (Objects.equals(merged.getText(), optionSubtitleText)) {
                return true;
            }
        }

        return false;
    }

    private IterationResult injectToVideo(
            Video video,
            SubtitleOption upperOption,
            SubtitleOption lowerOption,
            SubtitlesAndOutput merged,
            TableVideo tableVideo
    ) throws InterruptedException {
        try {
            ffmpeg.injectSubtitlesToFile(getFfmpegInjectInfo(video, upperOption, lowerOption, merged));
        } catch (FfmpegException e) {
            Platform.runLater(() -> tableVideo.setOnlyError(getInjectErrorText(e)));
            return IterationResult.FAILED;
        }

        BuiltInSubtitleOption newOption;
        try {
            newOption = getNewOption(video, merged, ffprobe);
        } catch (FfmpegException e) {
            String message = "The merge has finished successfully but failed to update the video info";
            Platform.runLater(() -> tableVideo.setOnlyError(message));
            return IterationResult.FAILED;
        }
        if (newOption  == null) {
            String message = "The merge has finished successfully but failed to find the new subtitle stream";
            Platform.runLater(() -> tableVideo.setOnlyError(message));
            return IterationResult.FAILED;
        }
        updateVideo(video, newOption);

        boolean haveHideableOptions = tableVideo.getOptions().stream().anyMatch(TableSubtitleOption::isHideable);
        TableSubtitleOption newTableOption = tableOptionFrom(newOption, haveHideableOptions, tableVideo, settings);

        Platform.runLater(() -> {
            tableVideo.setSizeAndLastModified(video.getSize(), video.getLastModified());
            tableVideo.addOption(newTableOption);
        });

        return IterationResult.SUCCESS;
    }

    private FfmpegInjectInfo getFfmpegInjectInfo(
            Video video,
            SubtitleOption upperOption,
            SubtitleOption lowerOption,
            SubtitlesAndOutput merged
    ) {
        String title = "merged-" + getMergedTitlePart(upperOption) + "-" + getMergedTitlePart(lowerOption);
        List<Integer> streamsToMakeNotDefaultIndices = video.getBuiltInOptions().stream()
                .filter(BuiltInSubtitleOption::isDefaultDisposition)
                .map(BuiltInSubtitleOption::getFfmpegIndex)
                .collect(toList());

        return new FfmpegInjectInfo(
                merged.getText(),
                video.getBuiltInOptions().size(),
                getMergedLanguage(upperOption, lowerOption),
                title,
                settings.isMakeMergedStreamsDefault(),
                streamsToMakeNotDefaultIndices,
                video.getFile(),
                largestFreeSpaceDirectory
        );
    }

    private static String getMergedTitlePart(SubtitleOption option) {
        if (option instanceof ExternalSubtitleOption) {
            return "external";
        } else if (option instanceof BuiltInSubtitleOption) {
            LanguageAlpha3Code language = ((BuiltInSubtitleOption) option).getLanguage();
            return language != null ? language.toString() : "unknown";
        } else {
            log.error("unexpected subtitle option class " + option.getClass() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private static LanguageAlpha3Code getMergedLanguage(SubtitleOption upperOption, SubtitleOption lowerOption) {
        LanguageAlpha3Code result = null;
        if (upperOption instanceof BuiltInSubtitleOption) {
            result = ((BuiltInSubtitleOption) upperOption).getLanguage();
        }

        if (result == null && lowerOption instanceof BuiltInSubtitleOption) {
            result = ((BuiltInSubtitleOption) lowerOption).getLanguage();
        }

        if (result == null) {
            result = LanguageAlpha3Code.undefined;
        }

        return result;
    }

    private static String getInjectErrorText(FfmpegException exception) {
        String result = "Merging has failed: ";

        if (exception.getCode() == FfmpegException.Code.FAILED_TO_MOVE_TEMP_VIDEO) {
            return result + "couldn't move the temporary video file";
        } else if (exception.getCode() == FfmpegException.Code.GENERAL_ERROR) {
            return result + "Ffmpeg returned an error";
        } else {
            log.error("unexpected inject exception code: " + exception.getCode() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    /*
     * I've decided that it's better to just add the new merged option instead of updating the whole option list because
     * you can't quickly recreate options - you either have to load subtitles with Ffmpeg which takes time or take them
     * from the old option list, and that is not a very "pure" approach in my opinion.
     */
    @Nullable
    private static BuiltInSubtitleOption getNewOption(
            Video video,
            SubtitlesAndOutput merged,
            Ffprobe ffprobe
    ) throws FfmpegException, InterruptedException {
        Set<Integer> originalOptionIndices = video.getBuiltInOptions().stream()
                .map(BuiltInSubtitleOption::getFfmpegIndex)
                .collect(toSet());

        JsonFfprobeVideoInfo ffprobeInfo = ffprobe.getVideoInfo(video.getFile());
        List<BuiltInSubtitleOption> updatedOptions = Videos.getSubtitleOptions(ffprobeInfo);

        List<BuiltInSubtitleOption> newOptions = updatedOptions.stream()
                .filter(option -> !originalOptionIndices.contains(option.getFfmpegIndex()))
                .collect(toList());
        if (newOptions.size() != 1) {
            log.warn(
                    "there should be exactly one new option but there are " + newOptions.size() + ", it can be a bug "
                            + "or the user working with the video manually"
            );
            return null;
        }

        BuiltInSubtitleOption result = newOptions.get(0);
        result.setSubtitlesAndInput(
                SubtitlesAndInput.from(merged.getText().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
        );

        return result;
    }

    private static void updateVideo(Video video, BuiltInSubtitleOption newOption) {
        if (newOption.isDefaultDisposition()) {
            for (BuiltInSubtitleOption option : video.getBuiltInOptions()) {
                option.disableDefaultDisposition();
            }
        }

        video.getOptions().add(newOption);
        video.setCurrentSizeAndLastModified();
    }

    private static IterationResult saveToSubtitleFile(
            Video video,
            SubtitleOption upperOption,
            SubtitleOption lowerOption,
            SubtitlesAndOutput merged,
            TableVideo tableVideo
    ) {
        try {
            File subtitleFile = new File(Utils.getMergedSubtitleFilePath(video, upperOption, lowerOption));

            FileUtils.writeStringToFile(
                    subtitleFile,
                    merged.getText(),
                    StandardCharsets.UTF_8
            );

            return IterationResult.SUCCESS;
        } catch (IOException e) {
            String message = "Failed to write the result, probably there is no access to the file";
            Platform.runLater(() -> tableVideo.setOnlyError(message));
            return IterationResult.FAILED;
        }
    }

    private static ActionResult getActionResult(
            int toProcessCount,
            int processedCount,
            int successfulCount,
            int noConfirmationCount,
            int alreadyMergedCount,
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
                    "The merge has finished successfully for the video",
                    "The merge has finished successfully for all %d videos"
            );
        } else if (noConfirmationCount == toProcessCount) {
            warn = Utils.getTextDependingOnCount(
                    noConfirmationCount,
                    "Merging is not possible because you didn't confirm the file overwriting",
                    "Merging is not possible because you didn't confirm the file overwriting for "
                            + "all %d videos"
            );
        } else if (alreadyMergedCount == toProcessCount) {
            warn = Utils.getTextDependingOnCount(
                    alreadyMergedCount,
                    "Selected subtitles have already been merged",
                    "Selected subtitles have already been merged for all %d videos"
            );
        } else if (failedCount == toProcessCount) {
            error = Utils.getTextDependingOnCount(
                    failedCount,
                    "Failed to merge subtitles for the video",
                    "Failed to merge subtitles for all %d videos"
            );
        } else {
            if (successfulCount != 0) {
                success = String.format(
                        "The merge has finished for %d/%d videos successfully",
                        successfulCount,
                        toProcessCount
                );
            }

            if (processedCount != toProcessCount) {
                if (StringUtils.isBlank(success)) {
                    warn = String.format(
                            "The merge has been cancelled for %d/%d videos",
                            toProcessCount - processedCount,
                            toProcessCount
                    );
                } else {
                    warn = String.format("cancelled for %d/%d", toProcessCount - processedCount, toProcessCount);
                }
            }

            if (noConfirmationCount != 0) {
                if (StringUtils.isBlank(success) && StringUtils.isBlank(warn)) {
                    warn = String.format(
                            "No file overwriting confirmation for %d/%d videos",
                            noConfirmationCount,
                            toProcessCount
                    );
                } else {
                    if (!StringUtils.isBlank(warn)) {
                        warn += ", ";
                    }
                    warn += String.format("no confirmation for %d/%d", noConfirmationCount, toProcessCount);
                }
            }

            if (alreadyMergedCount != 0) {
                if (StringUtils.isBlank(success) && StringUtils.isBlank(warn)) {
                    warn = String.format(
                            "Subtitles have already been merged for %d/%d videos",
                            alreadyMergedCount,
                            toProcessCount
                    );
                } else {
                    if (!StringUtils.isBlank(warn)) {
                        warn += ", ";
                    }
                    warn += String.format("already merged for %d/%d", alreadyMergedCount, toProcessCount);
                }
            }

            if (failedCount != 0) {
                error = String.format("failed for %d/%d", failedCount, toProcessCount);
            }
        }

        return new ActionResult(success, warn, error);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private ActionResult actionResult;
    }

    private enum IterationResult {
        NO_CONFIRMATION,
        FAILED,
        ALREADY_MERGED,
        SUCCESS
    }
}
