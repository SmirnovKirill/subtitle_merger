package kirill.subtitlemerger.gui.forms.videos.background;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.application.Platform;
import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.settings.MergeMode;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.subtitles.SubRipWriter;
import kirill.subtitlemerger.logic.subtitles.SubtitleMerger;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.ExternalSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.*;

@CommonsLog
@AllArgsConstructor
public class MergePrepareRunner implements BackgroundRunner<MergePrepareRunner.Result> {
    private List<TableVideo> tableVideos;

    private List<Video> videos;

    private Ffmpeg ffmpeg;

    private Settings settings;

    @Override
    public MergePrepareRunner.Result run(BackgroundManager backgroundManager) {
        clearActionResults(tableVideos, backgroundManager);

        List<TableVideo> selectedVideos = getSelectedVideos(tableVideos, backgroundManager);

        int filesWithoutSelectionCount = getFilesWithoutSelectionCount(selectedVideos, backgroundManager);
        if (filesWithoutSelectionCount != 0) {
            return new Result(
                    false,
                    filesWithoutSelectionCount,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        backgroundManager.setIndeterminateProgress();
        backgroundManager.setCancelPossible(true);

        List<FileMergeInfo> filesMergeInfo = new ArrayList<>();

        int processedCount = 0;
        for (TableVideo tableFileInfo : selectedVideos) {
            try {
                filesMergeInfo.add(
                        getFileMergeInfo(tableFileInfo, processedCount, selectedVideos.size(), backgroundManager)
                );
                processedCount++;
            } catch (InterruptedException e) {
                return new Result(
                        true,
                        filesWithoutSelectionCount,
                        null,
                        null,
                        null,
                        null,
                        filesMergeInfo
                );
            }
        }

        RequiredAndAvailableSpace requiredAndAvailableSpace = getRequiredAndAvailableTempSpace(
                filesMergeInfo,
                videos,
                settings,
                backgroundManager
        );

        return new Result(
                false,
                filesWithoutSelectionCount,
                requiredAndAvailableSpace != null ? requiredAndAvailableSpace.getRequiredSpace() : null,
                requiredAndAvailableSpace != null ? requiredAndAvailableSpace.getAvailableSpace() : null,
                requiredAndAvailableSpace != null ? requiredAndAvailableSpace.getDirectoryForTempFile() : null,
                getFilesToOverwrite(filesMergeInfo, settings, backgroundManager),
                filesMergeInfo
        );
    }

    private static int getFilesWithoutSelectionCount(
            List<TableVideo> filesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting file availability info...");

        return (int) filesInfo.stream()
                .filter(fileInfo -> fileInfo.getUpperOption() == null || fileInfo.getLowerOption() == null)
                .count();
    }

    private FileMergeInfo getFileMergeInfo(
            TableVideo tableFileInfo,
            int processedCount,
            int allCount,
            BackgroundManager backgroundManager
    ) throws InterruptedException {
        String progressMessagePrefix = getProgressMessagePrefix(processedCount, allCount, tableFileInfo);
        backgroundManager.updateMessage(progressMessagePrefix + "...");

        Video video = Video.getById(tableFileInfo.getId(), videos);
        SubtitleOption upperSubtitles = video.getOption(tableFileInfo.getUpperOption().getId());
        SubtitleOption lowerSubtitles = video.getOption(tableFileInfo.getLowerOption().getId());

        Set<LanguageAlpha3Code> languagesToCheck = getLanguagesToCheck(upperSubtitles, lowerSubtitles);

        int failedToLoadCount = loadStreams(
                tableFileInfo,
                video,
                languagesToCheck,
                progressMessagePrefix,
                backgroundManager
        );
        if (failedToLoadCount != 0) {
            return new FileMergeInfo(
                    video.getId(),
                    FileMergeStatus.FAILED_TO_LOAD_SUBTITLES,
                    failedToLoadCount,
                    upperSubtitles,
                    lowerSubtitles,
                    null,
                    getFileWithResult(video, upperSubtitles, lowerSubtitles, settings)
            );
        }

        backgroundManager.updateMessage(progressMessagePrefix + ": merging subtitles...");
        Subtitles mergedSubtitles = SubtitleMerger.mergeSubtitles(
                upperSubtitles.getSubtitles(),
                lowerSubtitles.getSubtitles()
        );
        String mergesSubtitleText = SubRipWriter.toText(mergedSubtitles, settings.isPlainTextSubtitles());

        if (isDuplicate(mergesSubtitleText, languagesToCheck, video, settings.isPlainTextSubtitles())) {
            return new FileMergeInfo(
                    video.getId(),
                    FileMergeStatus.DUPLICATE,
                    0,
                    upperSubtitles,
                    lowerSubtitles,
                    mergesSubtitleText,
                    getFileWithResult(video, upperSubtitles, lowerSubtitles, settings)
            );
        } else {
            return new FileMergeInfo(
                    video.getId(),
                    FileMergeStatus.OK,
                    0,
                    upperSubtitles,
                    lowerSubtitles,
                    mergesSubtitleText,
                    getFileWithResult(video, upperSubtitles, lowerSubtitles, settings)
            );
        }
    }

    private static String getProgressMessagePrefix(int processedCount, int allFileCount, TableVideo fileInfo) {
        String progressPrefix = allFileCount > 1
                ? String.format("%d/%d ", processedCount + 1, allFileCount) + "stage 1 of 2: processing file "
                : "Stage 1 of 2: processing file ";

        return progressPrefix + fileInfo.getFilePath();
    }

    private static Set<LanguageAlpha3Code> getLanguagesToCheck(SubtitleOption upperOption, SubtitleOption lowerOption) {
        Set<LanguageAlpha3Code> result = new HashSet<>();

        if (upperOption instanceof BuiltInSubtitleOption) {
            LanguageAlpha3Code language = ((BuiltInSubtitleOption) upperOption).getLanguage();
            if (language != null) {
                result.add(language);
                result.add(language.getSynonym());
            }
        }

        if (lowerOption instanceof BuiltInSubtitleOption) {
            LanguageAlpha3Code language = ((BuiltInSubtitleOption) lowerOption).getLanguage();
            if (language != null) {
                result.add(language);
                result.add(language.getSynonym());
            }
        }

        result.add(LanguageAlpha3Code.undefined);

        return result;
    }

    private int loadStreams(
            TableVideo tableFileInfo,
            Video fileInfo,
            Set<LanguageAlpha3Code> languagesToCheck,
            String progressMessagePrefix,
            BackgroundManager backgroundManager
    ) throws InterruptedException {
        int failedToLoad = 0;

        List<BuiltInSubtitleOption> streamsToLoad = fileInfo.getBuiltInOptions().stream()
                .filter(stream -> stream.getLanguage() == null || languagesToCheck.contains(stream.getLanguage()))
                .filter(stream -> stream.getSubtitles() == null)
                .collect(Collectors.toList());

        for (BuiltInSubtitleOption ffmpegStream : streamsToLoad) {
            backgroundManager.updateMessage(
                    progressMessagePrefix + ": loading subtitles "
                            + Utils.languageToString(ffmpegStream.getLanguage()).toUpperCase()
                            + (StringUtils.isBlank(ffmpegStream.getTitle()) ? "" : " " + ffmpegStream.getTitle())
                            + "..."
            );

            TableSubtitleOption tableSubtitleOption = tableFileInfo.getOption(ffmpegStream.getId());
            try {
                String subtitleText = ffmpeg.getSubtitleText(
                        ffmpegStream.getFfmpegIndex(),
                        ffmpegStream.getFormat(),
                        fileInfo.getFile()
                );
                SubtitlesAndInput subtitlesAndInput = SubtitlesAndInput.from(
                        subtitleText.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8
                );

                if (subtitlesAndInput.isCorrectFormat()) {
                    ffmpegStream.setSubtitlesAndInput(subtitlesAndInput);
                    Platform.runLater(() -> tableSubtitleOption.loadedSuccessfully(subtitlesAndInput.getSize(), null)); //todo temp
                } else {
                    Platform.runLater(() -> tableSubtitleOption.failedToLoad(INCORRECT_FORMAT));
                    failedToLoad++;
                }
            } catch (FfmpegException e) {
                log.warn("failed to get subtitle text: " + e.getCode() + ", console output " + e.getConsoleOutput());
                Platform.runLater(() -> tableSubtitleOption.failedToLoad(failedToLoadReasonFrom(e.getCode())));
                failedToLoad++;
            }
        }

        return failedToLoad;
    }

    private static boolean isDuplicate(
            String mergedText,
            Set<LanguageAlpha3Code> languagesToCheck,
            Video fileInfo,
            boolean plainText
    ) {
        for (BuiltInSubtitleOption subtitleStream : fileInfo.getBuiltInOptions()) {
            if (subtitleStream.getLanguage() == null || languagesToCheck.contains(subtitleStream.getLanguage())) {
                String subtitleText = SubRipWriter.toText(subtitleStream.getSubtitles(), plainText);
                if (Objects.equals(mergedText, subtitleText)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static File getFileWithResult(
            Video fileInfo,
            SubtitleOption upperOption,
            SubtitleOption lowerOption,
            Settings settings
    ) {
        if (settings.getMergeMode() == MergeMode.ORIGINAL_VIDEOS) {
            return fileInfo.getFile();
        } else if (settings.getMergeMode() == MergeMode.SEPARATE_SUBTITLE_FILES) {
            return new File(
                    FilenameUtils.removeExtension(fileInfo.getFile().getAbsolutePath())
                            + "_" + getOptionTitleForFile(upperOption) + "-" + getOptionTitleForFile(lowerOption)
                            + ".srt"
            );
        } else {
            throw new IllegalStateException();
        }
    }

    private static String getOptionTitleForFile(SubtitleOption subtitleOption) {
        if (subtitleOption instanceof ExternalSubtitleOption) {
            return "external";
        } else if (subtitleOption instanceof BuiltInSubtitleOption) {
            LanguageAlpha3Code language = ((BuiltInSubtitleOption) subtitleOption).getLanguage();
            return language != null ? language.toString() : "unknown";
        } else {
            throw new IllegalStateException();
        }
    }

    @Nullable
    private static RequiredAndAvailableSpace getRequiredAndAvailableTempSpace(
            List<FileMergeInfo> filesMergeInfo,
            List<Video> filesInfo,
            Settings settings,
            BackgroundManager backgroundManager
    ) {
        if (settings.getMergeMode() != MergeMode.ORIGINAL_VIDEOS) {
            return null;
        }

        backgroundManager.setIndeterminateProgress();
        backgroundManager.setCancelPossible(false);
        backgroundManager.updateMessage("Calculating required temporary space...");

        Long requiredSpace = null;
        Long availableSpace = null;
        File directoryForTempFile = null;

        for (FileMergeInfo fileMergeInfo : filesMergeInfo) {
            if (fileMergeInfo.getStatus() != FileMergeStatus.OK) {
                continue;
            }

            Video fileInfo = Video.getById(fileMergeInfo.getId(), filesInfo);

            long currentRequiredSpace = fileInfo.getFile().length();
            long currentAvailableSpace = fileInfo.getFile().getFreeSpace();

            if (requiredSpace == null || requiredSpace < currentAvailableSpace) {
                requiredSpace = currentRequiredSpace;
            }

            if (availableSpace == null || availableSpace < currentAvailableSpace) {
                availableSpace = currentAvailableSpace;
                directoryForTempFile = fileInfo.getFile().getParentFile();
            }
        }

        if (requiredSpace == null) {
            return null;
        }

        return new RequiredAndAvailableSpace(requiredSpace, availableSpace, directoryForTempFile);
    }

    private static List<File> getFilesToOverwrite(
            List<FileMergeInfo> filesMergeInfo,
            Settings settings,
            BackgroundManager backgroundManager
    ) {
        if (settings.getMergeMode() == MergeMode.ORIGINAL_VIDEOS) {
            return new ArrayList<>();
        }

        backgroundManager.setIndeterminateProgress();
        backgroundManager.setCancelPossible(false);
        backgroundManager.updateMessage("Getting files to overwrite...");

        List<File> result = new ArrayList<>();
        for (FileMergeInfo fileMergeInfo : filesMergeInfo) {
            if (fileMergeInfo.getFileWithResult().exists()) {
                result.add(fileMergeInfo.getFileWithResult());
            }
        }

        return result;
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private boolean canceled;

        private int filesWithoutSelectionCount;

        private Long requiredTempSpace;

        private Long availableTempSpace;

        private File directoryForTempFile;

        private List<File> filesToOverwrite;

        private List<FileMergeInfo> filesMergeInfo;
    }

    @AllArgsConstructor
    @Getter
    public static class FileMergeInfo {
        private String id;

        private FileMergeStatus status;

        private int failedToLoadSubtitlesCount;

        private SubtitleOption upperSubtitles;

        private SubtitleOption lowerSubtitles;

        private String mergedSubtitleText;

        private File fileWithResult;
    }

    public enum FileMergeStatus {
        FAILED_TO_LOAD_SUBTITLES,
        DUPLICATE,
        OK
    }

    @AllArgsConstructor
    @Getter
    private static class RequiredAndAvailableSpace {
        private long requiredSpace;

        private long availableSpace;

        private File directoryForTempFile;
    }
}
