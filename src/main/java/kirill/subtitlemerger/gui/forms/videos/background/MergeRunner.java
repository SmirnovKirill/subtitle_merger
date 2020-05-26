package kirill.subtitlemerger.gui.forms.videos.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.gui.GuiConstants;
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
import kirill.subtitlemerger.logic.subtitles.entities.SubtitleFormat;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndOutput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.utils.entities.MultiPartActionResult;
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
import org.apache.commons.lang3.exception.ExceptionUtils;
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
public class MergeRunner implements BackgroundRunner<MultiPartActionResult> {
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
    public MultiPartActionResult run(BackgroundManager backgroundManager) {
        backgroundManager.setCancelPossible(true);
        backgroundManager.setIndeterminateProgress();

        int toProcessCount = tableVideos.size();
        int processedCount = 0;
        int successfulCount = 0;
        int noOverwriteConfirmationCount = 0;
        int alreadyMergedCount = 0;
        int failedCount = 0;

        try {
            for (TableVideo tableVideo : tableVideos) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                Video video = Video.getById(tableVideo.getId(), videos);
                String actionPrefix = getProgressAction(processedCount, toProcessCount, "Merging: ");

                try {
                    processVideo(tableVideo, video, actionPrefix, backgroundManager);
                    successfulCount++;
                } catch (BreakIterationException e) {
                    if (e.getIterationError() == IterationError.NO_OVERWRITE_CONFIRMATION) {
                        noOverwriteConfirmationCount++;
                    } else if (e.getIterationError() == IterationError.ALREADY_MERGED) {
                        alreadyMergedCount++;
                    } else if (e.getIterationError() == IterationError.GENERAL_ERROR) {
                        failedCount++;
                    } else {
                        log.error("unexpected iteration error: " + e.getIterationError() + ", most likely a bug");
                        throw new IllegalStateException();
                    }
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
                noOverwriteConfirmationCount,
                alreadyMergedCount,
                failedCount
        );
    }

    private void processVideo(
            TableVideo tableVideo,
            Video video,
            String actionPrefix,
            BackgroundManager backgroundManager
    ) throws InterruptedException, BreakIterationException {
        backgroundManager.setCancelDescription(null);
        backgroundManager.updateMessage(actionPrefix + "processing " + video.getFile().getName()+ "...");

        SubtitleOption upperOption = video.getOption(tableVideo.getUpperOption().getId());
        SubtitleOption lowerOption = video.getOption(tableVideo.getLowerOption().getId());

        if (settings.getMergeMode() == MergeMode.SEPARATE_SUBTITLE_FILES) {
            checkOverwriteConfirmation(video, tableVideo, upperOption, lowerOption, confirmedFilesToOverwrite);
        }

        List<BuiltInSubtitleOption> optionsToLoad = getOptionsToLoad(video, upperOption, lowerOption, settings);
        loadSubtitles(optionsToLoad, video, tableVideo, actionPrefix, backgroundManager);

        SubtitlesAndOutput merged = getMergedSubtitles(
                upperOption,
                lowerOption,
                actionPrefix,
                settings,
                backgroundManager
        );

        if (settings.getMergeMode() == MergeMode.ORIGINAL_VIDEOS) {
            backgroundManager.setCancelDescription(getMergeCancelDescription(video));

            backgroundManager.updateMessage(actionPrefix + "processing the text to inject...");
            String textToInject = getTextToInject(merged, tableVideo, ffmpeg);
            assertNotEmpty(textToInject, tableVideo);

            backgroundManager.updateMessage(actionPrefix + "checking for duplicates...");
            checkForDuplicates(textToInject, video, tableVideo);

            boolean injectionFinished = false;
            backgroundManager.updateMessage(actionPrefix + "injecting the result into the video...");
            try {
                injectToVideo(textToInject, video, tableVideo, upperOption, lowerOption);
                injectionFinished = true;
            } finally {
                updateVideo(video, tableVideo, textToInject, injectionFinished);
            }
        } else if (settings.getMergeMode() == MergeMode.SEPARATE_SUBTITLE_FILES) {
            backgroundManager.updateMessage(actionPrefix + "writing the result to the file...");
            saveToSubtitleFile(video, tableVideo, upperOption, lowerOption, merged);
        } else {
            log.error("unexpected merge mode: " + settings.getMergeMode() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private static void checkOverwriteConfirmation(
            Video video,
            TableVideo tableVideo,
            SubtitleOption upperOption,
            SubtitleOption lowerOption,
            List<File> confirmedFilesToOverwrite
    ) throws BreakIterationException {
        File subtitleFile = new File(Utils.getMergedSubtitleFilePath(video, upperOption, lowerOption));
        if (subtitleFile.exists() && !confirmedFilesToOverwrite.contains(subtitleFile)) {
            String warning = "Merging is unavailable because you need to confirm file overwriting";
            Platform.runLater(() -> tableVideo.setOnlyWarning(warning));
            throw new BreakIterationException(IterationError.NO_OVERWRITE_CONFIRMATION);
        }
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

    private void loadSubtitles(
            List<BuiltInSubtitleOption> optionsToLoad,
            Video video,
            TableVideo tableVideo,
            String actionPrefix,
            BackgroundManager backgroundManager
    ) throws InterruptedException, BreakIterationException {
        if (CollectionUtils.isEmpty(optionsToLoad)) {
            return;
        }

        backgroundManager.setCancelDescription(getLoadCancelDescription(video));

        int toLoadCount = optionsToLoad.size();
        int failedCount = 0;
        int incorrectCount = 0;

        for (BuiltInSubtitleOption option : optionsToLoad) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

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

        backgroundManager.setCancelDescription(null);

        if (failedCount != 0 || incorrectCount != 0) {
            throw new BreakIterationException(IterationError.GENERAL_ERROR);
        }
    }

    private static SubtitlesAndOutput getMergedSubtitles(
            SubtitleOption upperOption,
            SubtitleOption lowerOption,
            String actionPrefix,
            Settings settings,
            BackgroundManager backgroundManager
    ) throws InterruptedException {
        backgroundManager.updateMessage(actionPrefix + "merging the subtitles...");

        Subtitles merged = SubtitleMerger.mergeSubtitles(upperOption.getSubtitles(), lowerOption.getSubtitles());
        return SubtitlesAndOutput.from(merged, settings.isPlainTextSubtitles());
    }

    @Nullable
    private static String getMergeCancelDescription(Video video) {
        if (video.getFile().length() >= GuiConstants.MERGE_CANCEL_DESCRIPTION_THRESHOLD) {
            return "Please be patient, merging may take a while for this video.";
        } else {
            return null;
        }
    }

    /*
     * The text to inject may differ from the text we have because ffmpeg will make its own transformations with it. For
     * more details please see the comment in the Ffmpeg::getProcessedSubtitles method.
     */
    private static String getTextToInject(
            SubtitlesAndOutput merged,
            TableVideo tableVideo,
            Ffmpeg ffmpeg
    ) throws InterruptedException, BreakIterationException {
        try {
            byte[] rawSubtitles = ffmpeg.getProcessedSubtitles(
                    merged.getText(),
                    SubtitleFormat.SUB_RIP.getFfmpegCodecs().get(0)
            );
            return new String(rawSubtitles, StandardCharsets.UTF_8);
        } catch (FfmpegException e) {
            log.warn("failed to get processed subtitles: " + e.getCode() + ", console output " + e.getConsoleOutput());
            String error = "The merge has failed: failed to get the processed text to inject";
            Platform.runLater(() -> tableVideo.setOnlyError(error));
            throw new BreakIterationException(IterationError.GENERAL_ERROR);
        }
    }

    private static void assertNotEmpty(String subtitleText, TableVideo tableVideo) throws BreakIterationException {
        if (StringUtils.isBlank(subtitleText)) {
            Platform.runLater(() -> tableVideo.setOnlyError("Merging is unavailable because the result is empty"));
            throw new BreakIterationException(IterationError.GENERAL_ERROR);
        }
    }

    private static void checkForDuplicates(
            String textToInject,
            Video video,
            TableVideo tableVideo
    ) throws BreakIterationException {
        for (BuiltInSubtitleOption option : video.getBuiltInOptions()) {
            if (!option.isMerged()) {
                continue;
            }

            SubtitlesAndInput optionSubtitlesAndInput = option.getSubtitlesAndInput();
            if (optionSubtitlesAndInput == null) {
                log.error("option subtitles are null, shouldn't have gotten here, most likely a bug");
                throw new IllegalStateException();
            }

            String optionText = new String(optionSubtitlesAndInput.getRawData(), optionSubtitlesAndInput.getEncoding());
            if (Objects.equals(textToInject, optionText)) {
                Platform.runLater(() -> tableVideo.setOnlyWarning("Selected subtitles have already been merged"));
                throw new BreakIterationException(IterationError.ALREADY_MERGED);
            }
        }
    }

    private void injectToVideo(
            String subtitleText,
            Video video,
            TableVideo tableVideo,
            SubtitleOption upperOption,
            SubtitleOption lowerOption
    ) throws InterruptedException, BreakIterationException {
        String title = getMergedTitle(upperOption, lowerOption, settings.isPlainTextSubtitles());
        List<Integer> streamsToMakeNotDefaultIndices = video.getBuiltInOptions().stream()
                .filter(BuiltInSubtitleOption::isDefaultDisposition)
                .map(BuiltInSubtitleOption::getFfmpegIndex)
                .collect(toList());

        FfmpegInjectInfo injectInfo = new FfmpegInjectInfo(
                subtitleText,
                video.getBuiltInOptions().size(),
                getMergedLanguage(upperOption, lowerOption),
                title,
                settings.isMakeMergedStreamsDefault(),
                streamsToMakeNotDefaultIndices,
                video.getFile(),
                largestFreeSpaceDirectory
        );

        try {
            ffmpeg.injectSubtitlesToFile(injectInfo);
        } catch (FfmpegException e) {
            log.warn("failed to inject subtitles: " + e.getCode() + ", console output " + e.getConsoleOutput());
            Platform.runLater(() -> tableVideo.setOnlyError(getInjectErrorText(e)));
            throw new BreakIterationException(IterationError.GENERAL_ERROR);
        }
    }

    private static String getMergedTitle(SubtitleOption upperOption, SubtitleOption lowerOption, boolean plaintText) {
        String result = "merged-" + getMergedTitlePart(upperOption) + "-" + getMergedTitlePart(lowerOption);
        if (plaintText) {
            result += "-plain_text";
        }

        return result;
    }

    private static String getMergedTitlePart(SubtitleOption option) {
        if (option instanceof ExternalSubtitleOption) {
            return "external";
        } else if (option instanceof BuiltInSubtitleOption) {
            LanguageAlpha3Code language = ((BuiltInSubtitleOption) option).getLanguage();
            return language != null ? language.toString() : "unknown";
        } else {
            log.error("unexpected subtitle option class: " + option.getClass() + ", most likely a bug");
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
        String result = "The merge has failed: ";

        if (exception.getCode() == FfmpegException.Code.FAILED_TO_MOVE_TEMP_VIDEO) {
            return result + "couldn't move the temporary video file";
        } else if (exception.getCode() == FfmpegException.Code.PROCESS_FAILED) {
            return result + "ffmpeg returned an error";
        } else {
            return result + "unexpected error, please see the log";
        }
    }

    /*
     * This method should be called even if the merging process has been interrupted. And the method itself should
     * update the video even in case of interruption.
     */
    private void updateVideo(
            Video video,
            TableVideo tableVideo,
            String injectedText,
            boolean injectionFinished
    ) throws BreakIterationException, InterruptedException {
        JsonFfprobeVideoInfo ffprobeInfo;
        InterruptedException interruptedException = null;
        try {
            try {
                ffprobeInfo = ffprobe.getVideoInfo(video.getFile());
            } catch (InterruptedException e) {
                /* We catch an exception here to finish the job and rethrow it at the of the method. */
                interruptedException = e;
                try {
                    ffprobeInfo = ffprobe.getVideoInfo(video.getFile());
                } catch (InterruptedException ex) {
                    log.error("the process can't be interrupted twice, mostlikely a bug");
                    throw new IllegalStateException();
                }
            }
        } catch (FfmpegException e) {
            log.warn("failed to get ffprobe info: " + e.getCode() + ", console output " + e.getConsoleOutput());
            String warning = "The merge has finished successfully but failed to update the video info";
            Platform.runLater(() -> tableVideo.setOnlyWarning(warning));
            throw new BreakIterationException(IterationError.GENERAL_ERROR);
        }

        BuiltInSubtitleOption newOption = getNewOption(video, tableVideo, ffprobeInfo, injectedText, injectionFinished);
        if (newOption == null) {
            return;
        }

        modifyOldOptions(video.getBuiltInOptions(), newOption);
        video.getOptions().add(newOption);

        /* We pass canHideOptions=false because a merged option shouldn't be hidden anyway. */
        TableSubtitleOption newTableOption = tableOptionFrom(newOption, false, tableVideo, settings);

        Platform.runLater(() -> {
            tableVideo.setSizeAndLastModified(video.getSize(), video.getLastModified());
            tableVideo.addOption(newTableOption);
        });

        if (interruptedException != null) {
            throw interruptedException;
        }
    }

    /*
     * I've decided that it's better to just add the new merged option instead of updating the whole option list because
     * you can't quickly recreate options - you either have to load subtitles with ffmpeg which takes time or take them
     * from the old option list, and that is not a very "pure" approach in my opinion.
     */
    @Nullable
    private BuiltInSubtitleOption getNewOption(
            Video video,
            TableVideo tableVideo,
            JsonFfprobeVideoInfo ffprobeInfo,
            String injectedText,
            boolean injectionFinished
    ) throws BreakIterationException {
        Set<Integer> originalOptionIndices = video.getBuiltInOptions().stream()
                .map(BuiltInSubtitleOption::getFfmpegIndex)
                .collect(toSet());

        List<BuiltInSubtitleOption> updatedOptions = Videos.getSubtitleOptions(ffprobeInfo);
        List<BuiltInSubtitleOption> newOptions = updatedOptions.stream()
                .filter(option -> !originalOptionIndices.contains(option.getFfmpegIndex()))
                .collect(toList());

        if (newOptions.size() > 1) {
            log.warn("there are " + newOptions.size() + " new options for video " + video.getFile().getAbsolutePath());
            String error = "There are more than one new subtitles, please refresh the list with videos";
            Platform.runLater(() -> tableVideo.setOnlyError(error));
            throw new BreakIterationException(IterationError.GENERAL_ERROR);
        } else if (newOptions.size() == 1) {
            BuiltInSubtitleOption result = newOptions.get(0);

            result.setSubtitlesAndInput(
                    SubtitlesAndInput.from(injectedText.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
            );

            return result;
        } else {
            if (injectionFinished) {
                log.error("failed to find the new option for the video " + video.getFile().getAbsolutePath());
                String error = "Failed to find the new subtitle streams, please check the video manually";
                Platform.runLater(() -> tableVideo.setOnlyError(error));
                throw new BreakIterationException(IterationError.GENERAL_ERROR);
            } else {
                return null;
            }
        }
    }

    private void modifyOldOptions(List<BuiltInSubtitleOption> oldOptions, BuiltInSubtitleOption newOption) {
        if (newOption.isDefaultDisposition()) {
            for (BuiltInSubtitleOption oldOption : oldOptions) {
                oldOption.disableDefaultDisposition();
            }
        }
    }

    private static void saveToSubtitleFile(
            Video video,
            TableVideo tableVideo,
            SubtitleOption upperOption,
            SubtitleOption lowerOption,
            SubtitlesAndOutput merged
    ) throws BreakIterationException {
        try {
            File subtitleFile = new File(Utils.getMergedSubtitleFilePath(video, upperOption, lowerOption));

            FileUtils.writeStringToFile(
                    subtitleFile,
                    merged.getText(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            log.warn("failed to save subtitles to file: " + ExceptionUtils.getStackTrace(e));
            String error = "Failed to write the result, probably there is no access to the file";
            Platform.runLater(() -> tableVideo.setOnlyError(error));
            throw new BreakIterationException(IterationError.GENERAL_ERROR);
        }
    }

    private static MultiPartActionResult getActionResult(
            int toProcessCount,
            int processedCount,
            int successfulCount,
            int noOverwriteConfirmationCount,
            int alreadyMergedCount,
            int failedCount
    ) {
        String success = "";
        String warning = "";
        String error = "";

        if (processedCount == 0) {
            warning = "The task has been cancelled, nothing was done";
        } else if (successfulCount == toProcessCount) {
            success = Utils.getTextDependingOnCount(
                    successfulCount,
                    "The merge has finished successfully for the video",
                    "The merge has finished successfully for all %d videos"
            );
        } else if (noOverwriteConfirmationCount == toProcessCount) {
            warning = Utils.getTextDependingOnCount(
                    noOverwriteConfirmationCount,
                    "Merging is not possible because you didn't confirm the file overwriting",
                    "Merging is not possible because you didn't confirm the file overwriting for "
                            + "all %d videos"
            );
        } else if (alreadyMergedCount == toProcessCount) {
            warning = Utils.getTextDependingOnCount(
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
                    warning = String.format(
                            "The merge has been cancelled for %d/%d videos",
                            toProcessCount - processedCount,
                            toProcessCount
                    );
                } else {
                    warning = String.format("cancelled for %d/%d", toProcessCount - processedCount, toProcessCount);
                }
            }

            if (noOverwriteConfirmationCount != 0) {
                if (StringUtils.isBlank(success) && StringUtils.isBlank(warning)) {
                    warning = String.format(
                            "No file overwriting confirmation for %d/%d videos",
                            noOverwriteConfirmationCount,
                            toProcessCount
                    );
                } else {
                    if (!StringUtils.isBlank(warning)) {
                        warning += ", ";
                    }
                    warning += String.format("no confirmation for %d/%d", noOverwriteConfirmationCount, toProcessCount);
                }
            }

            if (alreadyMergedCount != 0) {
                if (StringUtils.isBlank(success) && StringUtils.isBlank(warning)) {
                    warning = String.format(
                            "Subtitles have already been merged for %d/%d videos",
                            alreadyMergedCount,
                            toProcessCount
                    );
                } else {
                    if (!StringUtils.isBlank(warning)) {
                        warning += ", ";
                    }
                    warning += String.format("already merged for %d/%d", alreadyMergedCount, toProcessCount);
                }
            }

            if (failedCount != 0) {
                error = String.format("failed for %d/%d", failedCount, toProcessCount);
            }
        }

        return new MultiPartActionResult(success, warning, error);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private ActionResult actionResult;
    }

    @AllArgsConstructor
    @Getter
    private static class BreakIterationException extends Exception {
        private IterationError iterationError;
    }

    private enum IterationError {
        NO_OVERWRITE_CONFIRMATION,
        ALREADY_MERGED,
        GENERAL_ERROR,
    }
}
