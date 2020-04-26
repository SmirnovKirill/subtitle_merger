package kirill.subtitlemerger.logic.video_files;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.ffmpeg.json.JsonFfprobeFileInfo;
import kirill.subtitlemerger.logic.ffmpeg.json.JsonStream;
import kirill.subtitlemerger.logic.video_files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.video_files.entities.VideoFile;
import kirill.subtitlemerger.logic.video_files.entities.SubtitleOptionNotValidReason;
import kirill.subtitlemerger.logic.utils.file_validation.FileValidator;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileInfo;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileNotValidReason;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileValidationOptions;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static kirill.subtitlemerger.logic.video_files.entities.VideoFileNotValidReason.*;

@CommonsLog
public class VideoFiles {
    public static VideoFile getWithoutLoadingSubtitles(File file, List<String> allowedExtensions, Ffprobe ffprobe) {
        InputFileValidationOptions validationOptions = InputFileValidationOptions.builder()
                .allowedExtensions(allowedExtensions)
                .allowEmpty(true)
                .build();
        InputFileInfo inputFileInfo = FileValidator.getInputFileInfo(file.getAbsolutePath(), validationOptions);
        if (inputFileInfo.getNotValidReason() == InputFileNotValidReason.NO_EXTENSION) {
            return new VideoFile(file, null, NO_EXTENSION, null, null);
        } else if (inputFileInfo.getNotValidReason() == InputFileNotValidReason.NOT_ALLOWED_EXTENSION) {
            return new VideoFile(file, null, NOT_ALLOWED_EXTENSION, null, null);
        }
        /*
         * There can be other errors if the file is removed or turned into a directory between the selection and the
         * validation but we'll just let them be and let ffprobe return an error. Because these errors can happen only
         * if the user causes them on purpose, so I think it's not worthy to handle these situations in any special way.
         */

        JsonFfprobeFileInfo ffprobeInfo;
        try {
            ffprobeInfo = ffprobe.getFileInfo(file);
        } catch (FfmpegException e) {
            log.warn("failed to get ffprobe info: " + e.getCode() + ", console output " + e.getConsoleOutput());
            return new VideoFile(file, null, FFPROBE_FAILED, null, null);
        } catch (InterruptedException e) {
            log.error("the process can't be interrupted, most likely a bug");
            throw new IllegalStateException();
        }

        String format = ffprobeInfo.getFormat().getFormatName();
        if (!LogicConstants.ALLOWED_VIDEO_FORMATS.contains(format)) {
            return new VideoFile(file, format, NOT_ALLOWED_FORMAT, null, null);
        }

        return new VideoFile(
                file,
                format,
                null,
                new ArrayList<>(getSubtitleOptions(ffprobeInfo)),
                null
        );
    }

    public static List<FfmpegSubtitleStream> getSubtitleOptions(JsonFfprobeFileInfo ffprobeInfo) {
        List<FfmpegSubtitleStream> result = new ArrayList<>();

        for (JsonStream stream : ffprobeInfo.getStreams()) {
            if (!"subtitle".equals(stream.getCodecType())) {
                continue;
            }

            String format = stream.getCodecName();
            SubtitleOptionNotValidReason notValidReason = null;
            if (!LogicConstants.ALLOWED_SUBTITLE_FORMATS.contains(format)) {
                notValidReason = SubtitleOptionNotValidReason.NOT_ALLOWED_FORMAT;
            }

            result.add(
                    new FfmpegSubtitleStream(
                            stream.getIndex(),
                            null,
                            null,
                            notValidReason,
                            false,
                            false,
                            format,
                            getLanguage(stream).orElse(null),
                            getTitle(stream).orElse(null),
                            isDefaultDisposition(stream)
                    )
            );
        }

        return result;
    }

    private static Optional<LanguageAlpha3Code> getLanguage(JsonStream stream) {
        if (MapUtils.isEmpty(stream.getTags())) {
            return Optional.empty();
        }

        String languageRaw = stream.getTags().get("language");
        if (StringUtils.isBlank(languageRaw)) {
            return Optional.empty();
        }

        /*
         * https://www.ffmpeg.org/ffmpeg-formats.html#matroska says:
         * The language can be either the 3 letters bibliographic ISO-639-2 (ISO 639-2/B) form (like "fre" for French),
         * or a language code mixed with a country code for specialities in languages (like "fre-ca" for Canadian
         * French). So we can split by the hyphen and use the first part as an ISO-639 code.
         */
        languageRaw = languageRaw.split("-")[0];

        return Optional.ofNullable(LanguageAlpha3Code.getByCodeIgnoreCase(languageRaw));
    }

    private static Optional<String> getTitle(JsonStream stream) {
        if (MapUtils.isEmpty(stream.getTags())) {
            return Optional.empty();
        }

        return Optional.ofNullable(stream.getTags().get("title"));
    }

    private static boolean isDefaultDisposition(JsonStream stream) {
        return stream.getDisposition().getDefaultDisposition() == 1;
    }
}
