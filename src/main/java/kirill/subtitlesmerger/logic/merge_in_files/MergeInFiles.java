package kirill.subtitlesmerger.logic.merge_in_files;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.core.Merger;
import kirill.subtitlesmerger.logic.core.Parser;
import kirill.subtitlesmerger.logic.core.Writer;
import kirill.subtitlesmerger.logic.core.entities.Subtitles;
import kirill.subtitlesmerger.logic.merge_in_files.entities.*;
import kirill.subtitlesmerger.logic.merge_in_files.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.merge_in_files.ffmpeg.FfmpegException;
import kirill.subtitlesmerger.logic.merge_in_files.ffmpeg.Ffprobe;
import kirill.subtitlesmerger.logic.merge_in_files.ffmpeg.json.JsonFfprobeFileInfo;
import kirill.subtitlesmerger.logic.merge_in_files.ffmpeg.json.JsonStream;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static kirill.subtitlesmerger.logic.merge_in_files.entities.BriefFileInfo.UnavailabilityReason.*;

@CommonsLog
public class MergeInFiles {
    public static BriefFileInfo getBriefFileInfo(
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
            return new BriefFileInfo(file, NO_EXTENSION, null, null);
        }

        if (!allowedVideoExtensions.contains(extension.toLowerCase())) {
            return new BriefFileInfo(file, NOT_ALLOWED_EXTENSION, null, null);
        }

        String mimeType;
        try {
            mimeType = Files.probeContentType(file.toPath());
        } catch (IOException e) {
            log.warn("failed to get mime type of file " + file.getAbsolutePath() + ": "
                    + ExceptionUtils.getStackTrace(e)
            );

            return new BriefFileInfo(file, FAILED_TO_GET_MIME_TYPE, null, null);
        }

        if (!allowedVideoMimeTypes.contains(mimeType)) {
            return new BriefFileInfo(file, NOT_ALLOWED_MIME_TYPE, null, null);
        }

        JsonFfprobeFileInfo ffprobeInfo;
        try {
            ffprobeInfo = ffprobe.getFileInfo(file);
        } catch (Exception e) {
            log.warn("failed to get ffprobe info for file " + file.getAbsolutePath() + ": "
                    + ExceptionUtils.getStackTrace(e)
            );

            return new BriefFileInfo(file, FAILED_TO_GET_FFPROBE_INFO, null, null);
        }

        VideoFormat videoFormat = VideoFormat.from(ffprobeInfo.getFormat().getFormatName()).orElse(null);
        if (videoFormat == null) {
            return new BriefFileInfo(file, NOT_ALLOWED_CONTAINER, null, null);
        }

        return new BriefFileInfo(file, null, videoFormat, getBriefStreamInfos(ffprobeInfo));
    }

    private static List<BriefSubtitlesStreamInfo> getBriefStreamInfos(JsonFfprobeFileInfo ffprobeInfo) {
        List<BriefSubtitlesStreamInfo> result = new ArrayList<>();

        for (JsonStream stream : ffprobeInfo.getStreams()) {
            if (!"subtitle".equals(stream.getCodecType())) {
                continue;
            }

            SubtitlesCodec codec = SubtitlesCodec.from(stream.getCodecName()).orElse(null);
            if (codec == null) {
                result.add(
                        new BriefSubtitlesStreamInfo(
                                stream.getIndex(),
                                null,
                                BriefSubtitlesStreamInfo.UnavailabilityReason.NOT_ALLOWED_CODEC,
                                getLanguage(stream).orElse(null),
                                getTitle(stream).orElse(null)
                        )
                );
                continue;
            }

            result.add(
                    new BriefSubtitlesStreamInfo(
                            stream.getIndex(),
                            codec,
                            null,
                            getLanguage(stream).orElse(null),
                            getTitle(stream).orElse(null)
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

    public static FullFileInfo getFullFileInfo(BriefFileInfo briefInfo, Ffmpeg ffmpeg) {
        if (briefInfo.getUnavailabilityReason() != null) {
            return new FullFileInfo(
                    briefInfo,
                    FullFileInfo.UnavailabilityReason.FAILED_BEFORE,
                    wrap(briefInfo.getSubtitlesStreams())
            );
        }

        try {
            List<FullSubtitlesStreamInfo> allSubtitles = new ArrayList<>();

            for (BriefSubtitlesStreamInfo briefSubtitlesStream : briefInfo.getSubtitlesStreams()) {
                if (briefInfo.getUnavailabilityReason() != null) {
                    allSubtitles.add(
                            new FullSubtitlesStreamInfo(
                                    briefSubtitlesStream,
                                    briefSubtitlesStream.getUnavailabilityReason() != null
                                            ? FullSubtitlesStreamInfo.UnavailabilityReason.FAILED_BEFORE
                                            : null,
                                    null
                            )
                    );
                    continue;
                }

                String subtitlesText = ffmpeg.getSubtitlesText(briefSubtitlesStream.getIndex(), briefInfo.getFile());
                allSubtitles.add(
                        new FullSubtitlesStreamInfo(
                                briefSubtitlesStream,
                                null,
                                Parser.fromSubRipText(
                                        subtitlesText,
                                        "subs-" + briefSubtitlesStream.getIndex(),
                                        briefSubtitlesStream.getLanguage()
                                )
                        )
                );
            }

            return new FullFileInfo(briefInfo, null, allSubtitles);
        } catch (FfmpegException | Parser.IncorrectFormatException e) {
            return new FullFileInfo(
                    briefInfo,
                    FullFileInfo.UnavailabilityReason.FFMPEG_FAILED,
                    wrap(briefInfo.getSubtitlesStreams())
            );
        }
    }

    private static List<FullSubtitlesStreamInfo> wrap(List<BriefSubtitlesStreamInfo> briefInfos) {
        if (briefInfos == null) {
            return null;
        }

        List<FullSubtitlesStreamInfo> result = new ArrayList<>();

        for (BriefSubtitlesStreamInfo briefInfo : briefInfos) {
            result.add(
                    new FullSubtitlesStreamInfo(
                            briefInfo,
                            briefInfo.getUnavailabilityReason() != null
                                    ? FullSubtitlesStreamInfo.UnavailabilityReason.FAILED_BEFORE
                                    : null,
                            null
                    ));
        }

        return result;
    }

    public static void mergeAndInjectSubtitlesToFile(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles,
            FullFileInfo fullFileInfo,
            Ffmpeg ffmpeg
    ) throws SubtitlesAlreadyInjectedException, FfmpegException {
        Subtitles result = Merger.mergeSubtitles(upperSubtitles, lowerSubtitles);
        checkForDuplicates(result, fullFileInfo);

        LanguageAlpha3Code mainLanguage = getMergedSubtitlesMainLanguage(upperSubtitles, lowerSubtitles);
        String title = getMergedSubtitlesTitle(upperSubtitles, lowerSubtitles);

        ffmpeg.injectSubtitlesToFile(
                result,
                title,
                mainLanguage,
                fullFileInfo.getSubtitlesStreams().size(),
                fullFileInfo.getBriefInfo().getFile()
        );
    }

    private static LanguageAlpha3Code getMergedSubtitlesMainLanguage(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles
    ) {
        if (!CollectionUtils.isEmpty(upperSubtitles.getLanguages())) {
           return upperSubtitles.getLanguages().get(0);
        } else if (!CollectionUtils.isEmpty(lowerSubtitles.getLanguages())) {
            return lowerSubtitles.getLanguages().get(0);
        } else {
            return null;
        }
    }

    private static String getMergedSubtitlesTitle(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        String result = "Merged subtitles ";

        if (!CollectionUtils.isEmpty(upperSubtitles.getLanguages())) {
            result += StringUtils.join(upperSubtitles.getLanguages(), '-');
        } else {
            result += "file";
        }

        result += '-';

        if (!CollectionUtils.isEmpty(lowerSubtitles.getLanguages())) {
            result += StringUtils.join(lowerSubtitles.getLanguages(), '-');
        } else {
            result += "file";
        }

        return result;
    }

    private static void checkForDuplicates(
            Subtitles result,
            FullFileInfo fullFileInfo
    ) throws SubtitlesAlreadyInjectedException {
        String resultText = Writer.toSubRipText(result);

        if (!CollectionUtils.isEmpty(fullFileInfo.getSubtitlesStreams())) {
            for (FullSubtitlesStreamInfo streamInfo : fullFileInfo.getSubtitlesStreams()) {
                if (Objects.equals(Writer.toSubRipText(streamInfo.getSubtitles()), resultText)) {
                    throw new SubtitlesAlreadyInjectedException();
                }
            }
        }
    }

    public static class SubtitlesAlreadyInjectedException extends Exception {
    }
}
