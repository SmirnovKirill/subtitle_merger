package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.file_validation.FileValidator;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileInfo;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileNotValidReason;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileValidationOptions;
import kirill.subtitlemerger.logic.videos.entities.ExternalSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@CommonsLog
@AllArgsConstructor
public class ProcessSubtitleFileRunner implements BackgroundRunner<ProcessSubtitleFileRunner.Result> {
    private File subtitleFile;

    private Video video;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        backgroundManager.setCancellationPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Processing the file " + subtitleFile.getName() + "...");

        InputFileValidationOptions validationOptions = InputFileValidationOptions.builder()
                .allowedExtensions( Collections.singletonList("srt"))
                .allowEmpty(false)
                .maxAllowedSize(LogicConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024L)
                .loadContent(true)
                .build();
        InputFileInfo fileInfo = FileValidator.getInputFileInfo(subtitleFile.getAbsolutePath(), validationOptions);

        if (fileInfo.getNotValidReason() != null) {
            InputFileNotValidReason notValidReason = fileInfo.getNotValidReason();

            String shortenedPath = Utils.getShortenedString(
                    subtitleFile.getAbsolutePath(),
                    20,
                    40
            );

            String notValidReasonText;
            switch (notValidReason) {
                case PATH_IS_TOO_LONG:
                    notValidReasonText = "The file path is too long";
                    break;
                case INVALID_PATH:
                    notValidReasonText = "The file path is invalid";
                    break;
                case IS_A_DIRECTORY:
                    notValidReasonText = "'" + shortenedPath + "' is a directory, not a file";
                    break;
                case DOES_NOT_EXIST:
                    notValidReasonText = "The file '" + shortenedPath + "' doesn't exist";
                    break;
                case NO_EXTENSION:
                    notValidReasonText = "The file '" + shortenedPath + "' has no extension";
                    break;
                case NOT_ALLOWED_EXTENSION:
                    notValidReasonText = "The file '" + shortenedPath + "' has an incorrect extension";
                    break;
                case FILE_IS_EMPTY:
                    notValidReasonText = "The file '" + shortenedPath + "' is empty";
                    break;
                case FILE_IS_TOO_BIG:
                    notValidReasonText = "The file '" + shortenedPath + "' is too big (>"
                            + LogicConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES + " megabytes)";
                    break;
                case FAILED_TO_READ_CONTENT:
                    notValidReasonText = shortenedPath + ": failed to read the file";
                    break;
                default:
                    log.error("unexpected subtitle file not valid reason: " + notValidReason + ", most likely a bug");
                    throw new IllegalStateException();
            }

            return new Result(notValidReasonText, null);
        }

        if (isDuplicate(subtitleFile, video)) {
            return new Result("This file has already been added", null);
        }

        ExternalSubtitleOption subtitleOption = new ExternalSubtitleOption(
                subtitleFile,
                SubtitlesAndInput.from(fileInfo.getContent(), StandardCharsets.UTF_8)
        );

        return new Result(null, subtitleOption);
    }

    private static boolean isDuplicate(File subtitleFile, Video video) {
        List<ExternalSubtitleOption> externalSubtitleOptions = video.getExternalSubtitleOptions();
        if (CollectionUtils.isEmpty(externalSubtitleOptions)) {
            return false;
        } else if (externalSubtitleOptions.size() == 1) {
            return Objects.equals(subtitleFile, externalSubtitleOptions.get(0).getFile());
        } else {
            log.error("enough subtitle files already: " + externalSubtitleOptions.size() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private String notValidReason;

        private ExternalSubtitleOption subtitleOption;
    }
}
