package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.core.SubtitleParser;
import kirill.subtitlemerger.logic.core.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.utils.FileValidator;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileWithSubtitles;
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

        FileValidator.InputFileInfo validatorFileInfo = FileValidator.getInputFileInfo(
                fileWithSubtitlesToAdd.getAbsolutePath(),
                Collections.singletonList("srt"),
                false,
                GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024,
                true
        ).orElseThrow(IllegalStateException::new);

        if (validatorFileInfo.getIncorrectFileReason() != null) {
            return Result.createUnavailable(unavailabilityReasonFrom(validatorFileInfo.getIncorrectFileReason()));
        }

        if (isDuplicate(fileWithSubtitlesToAdd, videoFileInfo)) {
            return Result.createUnavailable(TableWithFiles.FileWithSubtitlesUnavailabilityReason.DUPLICATE);
        }

        Subtitles subtitles;
        try {
            subtitles = SubtitleParser.fromSubRipText(
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
            FileValidator.IncorrectInputFileReason incorrectFileReason
    ) {
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
