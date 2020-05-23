package kirill.subtitlemerger.logic.videos;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.ffmpeg.json.JsonFfprobeVideoInfo;
import kirill.subtitlemerger.logic.ffmpeg.json.JsonStream;
import kirill.subtitlemerger.logic.utils.file_validation.FileValidator;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileInfo;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileNotValidReason;
import kirill.subtitlemerger.logic.utils.file_validation.InputFileValidationOptions;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOptionNotValidReason;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static kirill.subtitlemerger.logic.videos.entities.VideoNotValidReason.*;

@CommonsLog
public class Videos {
    /**
     * Returns the information on the given file. Not that it doesn't load the subtitles because it's a pretty
     * time-consuming operation.
     */
    public static Video getVideo(File file, List<String> allowedExtensions, Ffprobe ffprobe) {
        InputFileValidationOptions validationOptions = InputFileValidationOptions.builder()
                .allowedExtensions(allowedExtensions)
                .allowEmpty(true)
                .build();
        InputFileInfo fileInfo = FileValidator.getInputFileInfo(file.getAbsolutePath(), validationOptions);
        if (fileInfo.getNotValidReason() == InputFileNotValidReason.NO_EXTENSION) {
            return new Video(file, NO_EXTENSION, null, null);
        } else if (fileInfo.getNotValidReason() == InputFileNotValidReason.NOT_ALLOWED_EXTENSION) {
            return new Video(file, NOT_ALLOWED_EXTENSION, null, null);
        }
        /*
         * There can be other errors if the file was removed or turned into a directory between the selection and the
         * validation but we'll just let them be and let ffprobe return an error. Because these errors can happen only
         * if the user causes them on purpose, so I think it's not worthy to handle these situations in any special way.
         */

        JsonFfprobeVideoInfo ffprobeInfo;
        try {
            ffprobeInfo = ffprobe.getVideoInfo(file);
        } catch (FfmpegException e) {
            log.warn("failed to get ffprobe info: " + e.getCode() + ", console output " + e.getConsoleOutput());
            return new Video(file, FFPROBE_FAILED, null, null);
        } catch (InterruptedException e) {
            log.error("the process can't be interrupted, most likely a bug");
            throw new IllegalStateException();
        }

        String format = ffprobeInfo.getFormat().getFormatName();
        if (!LogicConstants.ALLOWED_VIDEO_FORMATS.contains(format)) {
            return new Video(file, NOT_ALLOWED_FORMAT, format, null);
        }

        return new Video(file, null, format, new ArrayList<>(getSubtitleOptions(ffprobeInfo)));
    }

    public static List<BuiltInSubtitleOption> getSubtitleOptions(JsonFfprobeVideoInfo ffprobeInfo) {
        List<BuiltInSubtitleOption> result = new ArrayList<>();

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
                    new BuiltInSubtitleOption(
                            stream.getIndex(),
                            null,
                            notValidReason,
                            format,
                            getLanguage(stream),
                            getTitle(stream),
                            isDefaultDisposition(stream)
                    )
            );
        }

        return result;
    }

    @Nullable
    private static LanguageAlpha3Code getLanguage(JsonStream stream) {
        if (MapUtils.isEmpty(stream.getTags())) {
            return null;
        }

        String languageRaw = stream.getTags().get("language");
        if (StringUtils.isBlank(languageRaw)) {
            return null;
        }

        /*
         * https://www.ffmpeg.org/ffmpeg-formats.html#matroska says:
         * The language can be either the 3 letters bibliographic ISO-639-2 (ISO 639-2/B) form (like "fre" for French),
         * or a language code mixed with a country code for specialities in languages (like "fre-ca" for Canadian
         * French). So we can split by the hyphen and use the first part as an ISO-639 code.
         */
        languageRaw = languageRaw.split("-")[0];

        return LanguageAlpha3Code.getByCodeIgnoreCase(languageRaw);
    }

    @Nullable
    private static String getTitle(JsonStream stream) {
        if (MapUtils.isEmpty(stream.getTags())) {
            return null;
        }

        return stream.getTags().get("title");
    }

    private static boolean isDefaultDisposition(JsonStream stream) {
        return stream.getDisposition().getDefaultDisposition() == 1;
    }
}
