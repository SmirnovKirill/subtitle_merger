package kirill.subtitlemerger.logic.work_with_files;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.work_with_files.entities.*;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.json.JsonFfprobeFileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.json.JsonStream;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static kirill.subtitlemerger.logic.work_with_files.entities.FileInfo.UnavailabilityReason.*;

@CommonsLog
public class FileInfoGetter {
    public static FileInfo getFileInfoWithoutSubtitles(
            File file,
            List<String> allowedVideoExtensions,
            List<String> allowedVideoMimeTypes,
            Ffprobe ffprobe
    ) {
        if (!file.isFile()) {
            log.error("passed file is not an actual file: " + file.getAbsolutePath());
            throw new IllegalArgumentException();
        }

        String extension = FilenameUtils.getExtension(file.getName());
        if (StringUtils.isBlank(extension)) {
            return new FileInfo(file, NO_EXTENSION, null, null);
        }

        if (!allowedVideoExtensions.contains(extension.toLowerCase())) {
            return new FileInfo(file, NOT_ALLOWED_EXTENSION, null, null);
        }

        String mimeType;
        try {
            mimeType = Files.probeContentType(file.toPath());
        } catch (IOException e) {
            log.warn("failed to get mime type of file " + file.getAbsolutePath() + ": "
                    + ExceptionUtils.getStackTrace(e)
            );

            return new FileInfo(file, FAILED_TO_GET_MIME_TYPE, null, null);
        }

        if (!allowedVideoMimeTypes.contains(mimeType)) {
            return new FileInfo(file, NOT_ALLOWED_MIME_TYPE, null, null);
        }

        JsonFfprobeFileInfo ffprobeInfo;
        try {
            ffprobeInfo = ffprobe.getFileInfo(file);
        } catch (FfmpegException e) {
            return new FileInfo(file, FAILED_TO_GET_FFPROBE_INFO, null, null);
        } catch (Exception e) {
            log.warn("failed to get ffprobe info for file " + file.getAbsolutePath() + ": "
                    + ExceptionUtils.getStackTrace(e)
            );

            return new FileInfo(file, FAILED_TO_GET_FFPROBE_INFO, null, null);
        }

        VideoFormat videoFormat = VideoFormat.from(ffprobeInfo.getFormat().getFormatName()).orElse(null);
        if (videoFormat == null) {
            return new FileInfo(file, NOT_ALLOWED_CONTAINER, null, null);
        }

        return new FileInfo(file, null, videoFormat, getStreamsWithoutSubtitles(ffprobeInfo));
    }

    private static List<SubtitleStream> getStreamsWithoutSubtitles(JsonFfprobeFileInfo ffprobeInfo) {
        List<SubtitleStream> result = new ArrayList<>();

        for (JsonStream stream : ffprobeInfo.getStreams()) {
            if (!"subtitle".equals(stream.getCodecType())) {
                continue;
            }

            SubtitleCodec codec = SubtitleCodec.from(stream.getCodecName()).orElse(null);
            if (codec == null) {
                result.add(
                        new FfmpegSubtitleStream(
                                null,
                                null,
                                stream.getIndex(),
                                FfmpegSubtitleStream.UnavailabilityReason.NOT_ALLOWED_CODEC,
                                getLanguage(stream).orElse(null),
                                getTitle(stream).orElse(null),
                                isDefaultDisposition(stream)
                        )
                );
                continue;
            }

            result.add(
                    new FfmpegSubtitleStream(
                            codec,
                            null,
                            stream.getIndex(),
                            null,
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
         * or a language code mixed with a country code for specialities in languages
         * (like "fre-ca" for Canadian French).
         * So we can split by the hyphen and use the first part as an ISO-639 code.
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
