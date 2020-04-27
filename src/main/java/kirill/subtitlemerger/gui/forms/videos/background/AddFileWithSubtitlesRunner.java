package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableWithVideos;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.utils.file_validation.FileValidator;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileInfo;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileNotValidReason;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileValidationOptions;
import kirill.subtitlemerger.logic.videos.entities.ExternalSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@CommonsLog
@AllArgsConstructor
public class AddFileWithSubtitlesRunner implements BackgroundRunner<AddFileWithSubtitlesRunner.Result> {
    private File fileWithSubtitlesToAdd;

    private VideoInfo videoFileInfo;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Processing file " + fileWithSubtitlesToAdd.getAbsolutePath() + "...");

        InputFileValidationOptions validationOptions = InputFileValidationOptions.builder()
                .allowedExtensions( Collections.singletonList("srt"))
                .allowEmpty(false)
                .maxAllowedSize(LogicConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024L)
                .loadContent(true)
                .build();
        InputFileInfo validatorFileInfo = FileValidator.getInputFileInfo(
                fileWithSubtitlesToAdd.getAbsolutePath(),
                validationOptions
        );

        if (validatorFileInfo.getNotValidReason() != null) {
            return Result.createUnavailable(unavailabilityReasonFrom(validatorFileInfo.getNotValidReason()));
        }

        if (isDuplicate(fileWithSubtitlesToAdd, videoFileInfo)) {
            return Result.createUnavailable(TableWithVideos.FileWithSubtitlesUnavailabilityReason.DUPLICATE);
        }

        return new Result(
                new ExternalSubtitleOption(
                        fileWithSubtitlesToAdd,
                        SubtitlesAndInput.from(validatorFileInfo.getContent(), StandardCharsets.UTF_8),
                        false,
                        false
                ),
                (int) fileWithSubtitlesToAdd.length(),
                null
        );
    }

    private static TableWithVideos.FileWithSubtitlesUnavailabilityReason unavailabilityReasonFrom(
            InputFileNotValidReason incorrectFileReason
    ) {
        if (incorrectFileReason == InputFileNotValidReason.PATH_IS_EMPTY) {
            throw new IllegalStateException();
        }

        //todo switch

        return EnumUtils.getEnum(
                TableWithVideos.FileWithSubtitlesUnavailabilityReason.class,
                incorrectFileReason.toString()
        );
    }

    private static boolean isDuplicate(File fileToAdd, VideoInfo fileInfo) {
        List<ExternalSubtitleOption> filesWithSubtitles = fileInfo.getExternalSubtitles();
        if (CollectionUtils.isEmpty(filesWithSubtitles)) {
            return false;
        } else if (filesWithSubtitles.size() == 1) {
            return Objects.equals(fileToAdd, filesWithSubtitles.get(0).getFile());
        } else {
            log.error(
                    String.format("there are already enough files (%d), most likely a bug", filesWithSubtitles.size())
            );
            throw new IllegalStateException();
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private ExternalSubtitleOption fileWithSubtitles;

        private int size;

        private TableWithVideos.FileWithSubtitlesUnavailabilityReason unavailabilityReason;

        static Result createUnavailable(TableWithVideos.FileWithSubtitlesUnavailabilityReason reason) {
            return new Result(null, 0, reason);
        }
    }
}
