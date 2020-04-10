package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.core.SubRipParser;
import kirill.subtitlemerger.logic.core.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.utils.file_validation.FileValidator;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileNotValidReason;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileInfo;
import kirill.subtitlemerger.logic.file_info.entities.FileInfo;
import kirill.subtitlemerger.logic.file_info.entities.FileWithSubtitles;
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

    private FileInfo videoFileInfo;

    @Override
    public Result run(BackgroundRunnerManager runnerManager) {
        runnerManager.setIndeterminateProgress();
        runnerManager.updateMessage("Processing file " + fileWithSubtitlesToAdd.getAbsolutePath() + "...");

        InputFileInfo validatorFileInfo = FileValidator.getInputFileInfo(
                fileWithSubtitlesToAdd.getAbsolutePath(),
                Collections.singletonList("srt"),
                false,
                GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024,
                true
        );

        if (validatorFileInfo.getNotValidReason() != null) {
            return Result.createUnavailable(unavailabilityReasonFrom(validatorFileInfo.getNotValidReason()));
        }

        if (isDuplicate(fileWithSubtitlesToAdd, videoFileInfo)) {
            return Result.createUnavailable(TableWithFiles.FileWithSubtitlesUnavailabilityReason.DUPLICATE);
        }

        Subtitles subtitles;
        try {
            subtitles = SubRipParser.from(
                    new String(validatorFileInfo.getContent(), StandardCharsets.UTF_8),
                    null
            );
        } catch (SubtitleFormatException e) {
            subtitles = null;
        }

        return new Result(
                new FileWithSubtitles(
                        fileWithSubtitlesToAdd,
                        subtitles,
                        StandardCharsets.UTF_8,
                        false,
                        false,
                        validatorFileInfo.getContent()
                ),
                (int) fileWithSubtitlesToAdd.length(),
                null
        );
    }

    private static TableWithFiles.FileWithSubtitlesUnavailabilityReason unavailabilityReasonFrom(
            InputFileNotValidReason incorrectFileReason
    ) {
        if (incorrectFileReason == InputFileNotValidReason.PATH_IS_EMPTY) {
            throw new IllegalStateException();
        }

        return EnumUtils.getEnum(
                TableWithFiles.FileWithSubtitlesUnavailabilityReason.class,
                incorrectFileReason.toString()
        );
    }

    private static boolean isDuplicate(File fileToAdd, FileInfo fileInfo) {
        List<FileWithSubtitles> filesWithSubtitles = fileInfo.getFilesWithSubtitles();
        if (CollectionUtils.isEmpty(filesWithSubtitles)) {
            return false;
        } else if (filesWithSubtitles.size() == 1) {
            return Objects.equals(fileToAdd, filesWithSubtitles.get(0).getFile());
        } else {
            log.error(
                    String.format("there are already enough files (%d), shouldn't happen", filesWithSubtitles.size())
            );
            throw new IllegalStateException();
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private FileWithSubtitles fileWithSubtitles;

        private int size;

        private TableWithFiles.FileWithSubtitlesUnavailabilityReason unavailabilityReason;

        static Result createUnavailable(TableWithFiles.FileWithSubtitlesUnavailabilityReason reason) {
            return new Result(null, 0, reason);
        }
    }
}
