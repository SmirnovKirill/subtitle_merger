package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitleFormat;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.MultiPartActionResult;
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
import java.util.List;
import java.util.Objects;

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.INCORRECT_FORMAT;

@CommonsLog
@AllArgsConstructor
public class ProcessSubtitleFileRunner implements BackgroundRunner<ProcessSubtitleFileRunner.Result> {
    private File subtitleFile;

    private Video video;

    private TableVideo tableVideo;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        backgroundManager.setCancelPossible(false);
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Processing " + subtitleFile.getName() + "...");

        InputFileValidationOptions validationOptions = InputFileValidationOptions.builder()
                .allowedExtensions(SubtitleFormat.SUB_RIP.getExtensions())
                .allowEmpty(false)
                .maxAllowedSize(LogicConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024L)
                .loadContent(true)
                .build();
        InputFileInfo fileInfo = FileValidator.getInputFileInfo(subtitleFile.getAbsolutePath(), validationOptions);

        if (fileInfo.getNotValidReason() != null) {
            return getFileNotValidResult(fileInfo.getNotValidReason(), fileInfo.getFile().getAbsolutePath());
        }

        if (isDuplicate(subtitleFile, video)) {
            String error = "This file has already been added";
            return new Result(MultiPartActionResult.onlyError(error), null, null);
        }

        SubtitlesAndInput subtitlesAndInput = SubtitlesAndInput.from(fileInfo.getContent(), StandardCharsets.UTF_8);
        ExternalSubtitleOption option = new ExternalSubtitleOption(subtitleFile, subtitlesAndInput);
        TableSubtitleOption tableOption = TableSubtitleOption.createExternal(
                option.getId(),
                tableVideo,
                subtitlesAndInput.isCorrectFormat() ? null : INCORRECT_FORMAT,
                option.getFile().getAbsolutePath(),
                subtitlesAndInput.getSize(),
                false,
                false
        );

        MultiPartActionResult actionResult;
        if (!subtitlesAndInput.isCorrectFormat()) {
            actionResult = MultiPartActionResult.onlyWarning(
                    "The file has been added but subtitles can't be parsed, it can happen if a file is not "
                            + "UTF-8-encoded; you can change the encoding after pressing the preview button"
            );
        } else {
            actionResult = MultiPartActionResult.onlySuccess("The file with subtitles has been added successfully");
        }

        return new Result(actionResult, option, tableOption);
    }

    private static Result getFileNotValidResult(InputFileNotValidReason notValidReason, String filePath) {
        String shortenedPath = Utils.getShortenedString(filePath, 20, 40);

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

        return new Result(MultiPartActionResult.onlyError(notValidReasonText), null, null);
    }

    private static boolean isDuplicate(File subtitleFile, Video video) {
        List<ExternalSubtitleOption> externalOptions = video.getExternalOptions();
        if (CollectionUtils.isEmpty(externalOptions)) {
            return false;
        } else if (externalOptions.size() == 1) {
            return Objects.equals(subtitleFile, externalOptions.get(0).getFile());
        } else {
            log.error("enough subtitle files already: " + externalOptions.size() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private MultiPartActionResult actionResult;

        private ExternalSubtitleOption option;

        private TableSubtitleOption tableOption;
    }
}
