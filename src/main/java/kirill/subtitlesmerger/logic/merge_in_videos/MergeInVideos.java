package kirill.subtitlesmerger.logic.merge_in_videos;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.core.Merger;
import kirill.subtitlesmerger.logic.core.Parser;
import kirill.subtitlesmerger.logic.core.entities.Subtitles;
import kirill.subtitlesmerger.logic.merge_in_videos.entities.*;
import kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.FfmpegException;
import kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.Ffprobe;
import kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.json.JsonFfprobeFileInfo;
import kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.json.JsonStream;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static kirill.subtitlesmerger.logic.merge_in_videos.entities.BriefFileInfo.UnavailabilityReason.*;

@CommonsLog
public class MergeInVideos {
    public static List<BriefFileInfo> getBriefFileInfos(
            File directory,
            List<String> allowedVideoExtensions,
            List<String> allowedVideoMimeTypes,
            Ffprobe ffprobe
    ) {
        File[] directoryFiles = getDirectoryFiles(directory);

        List<BriefFileInfo> result = new ArrayList<>();

        for (File file : directoryFiles) {
            if (!file.isFile()) {
                continue;
            }

            result.add(getBriefFileInfo(file, allowedVideoExtensions, allowedVideoMimeTypes, ffprobe));
        }

        return result;
    }

    private static File[] getDirectoryFiles(File directory) {
        if (!directory.isDirectory()) {
            log.error("passed file is not a directory: " + directory.getAbsolutePath());
            throw new IllegalArgumentException();
        }

        File[] result = directory.listFiles();
        if (result == null) {
            log.error("failed to get directory files, directory " + directory.getAbsolutePath());
            throw new IllegalArgumentException();
        }

        return result;
    }

    private static BriefFileInfo getBriefFileInfo(
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

    private static void main() {
        System.out.println("got brief files info");

        List<FullFileInfo> fullFilesInfo = new ArrayList<>();
        for (int i = 0; i < briefFilesInfo.size(); i++) {
            BriefFileInfo briefFileInfo = briefFilesInfo.get(i);

            System.out.println(
                    "start processing file " + briefFileInfo.getFile().getAbsolutePath() + ", "
                            + (i + 1) + "/" + briefFilesInfo.size()
            );

            fullFilesInfo.add(getFullFileInfo(briefFileInfo, ffmpeg));
        }

        System.out.println("start injecting subtitles");
        for (int i = 0; i < fullFilesInfo.size(); i++) {
            FullFileInfo fullFileInfo = fullFilesInfo.get(i);

            System.out.println("start processing file " + fullFileInfo.getBriefInfo().getFile().getAbsolutePath() + ", "
                    + (i + 1) + "/" + fullFilesInfo.size()
            );

            Subtitles merged = fullFileInfo.getMerged(config).orElse(null);
            if (merged == null) {
                System.out.println("no merged subtitles");
            } else {
                try {
                    ffmpeg.injectSubtitlesToFile(
                            merged,
                            fullFileInfo.getSubtitlesStreams().size(),
                            fullFileInfo.getBriefInfo().getFile()
                    );
                } catch (FfmpegException e) {
                    if (e.getCode() == FfmpegException.Code.FAILED_TO_MOVE_TEMP_VIDEO) {
                        System.out.println("failed to move temp file for "
                                + fullFileInfo.getBriefInfo().getFile().getAbsolutePath()
                        );
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    private static FullFileInfo getFullFileInfo(BriefFileInfo briefFileInfo, Ffmpeg ffmpeg) {
        if (briefFileInfo.getUnavailabilityReason() != null) {
            return new FullFileInfo(
                    briefFileInfo,
                    FullFileInfo.UnavailabilityReason.FAILED_BEFORE,
                    wrap(briefFileInfo.getSubtitlesStreams())
            );
        }

        try {
            List<FullSubtitlesStreamInfo> allSubtitles = new ArrayList<>();

            for (BriefSubtitlesStreamInfo briefSubtitlesStream : briefFileInfo.getSubtitlesStreams()) {
                if (briefFileInfo.getUnavailabilityReason() != null) {
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

                String subtitlesText = ffmpeg.getSubtitlesText(briefSubtitlesStream.getIndex(), briefFileInfo.getFile());
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

            return new FullFileInfo(briefFileInfo, null, allSubtitles);
        } catch (FfmpegException | Parser.IncorrectFormatException e) {
            return new FullFileInfo(
                    briefFileInfo,
                    FullFileInfo.UnavailabilityReason.FFMPEG_FAILED,
                    wrap(briefFileInfo.getSubtitlesStreams())
            );
        }
    }

    private static List<FullSubtitlesStreamInfo> wrap(List<BriefSubtitlesStreamInfo> briefSubtitlesStreams) {
        if (briefSubtitlesStreams == null) {
            return null;
        }

        List<FullSubtitlesStreamInfo> result = new ArrayList<>();

        for (BriefSubtitlesStreamInfo briefSubtitlesStream
                : briefSubtitlesStreams) {
            result.add(
                    new FullSubtitlesStreamInfo(
                            briefSubtitlesStream,
                            briefSubtitlesStream.getUnavailabilityReason() != null
                                    ? FullSubtitlesStreamInfo.UnavailabilityReason.FAILED_BEFORE
                                    : null,
                            null
                    ));
        }

        return result;
    }

    public Optional<Subtitles> getMerged(Config config) {
        if (unavailabilityReason != null || CollectionUtils.isEmpty(subtitlesStreams)) {
            return Optional.empty();
        }

        List<FullSubtitlesStreamInfo> streamsUpperLanguage = subtitlesStreams.stream()
                .filter(subtitles -> subtitles.getUnavailabilityReason() == null)
                .filter(subtitles -> subtitles.getBriefInfo().getLanguage() == config.getUpperLanguage())
                .collect(Collectors.toList());

        List<FullSubtitlesStreamInfo> streamsLowerLanguage = subtitlesStreams.stream()
                .filter(subtitles -> subtitles.getUnavailabilityReason() == null)
                .filter(subtitles -> subtitles.getBriefInfo().getLanguage() == config.getLowerLanguage())
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(streamsUpperLanguage) || CollectionUtils.isEmpty(streamsLowerLanguage)) {
            return Optional.empty();
        }

        streamsUpperLanguage.sort(
                Comparator.comparing((FullSubtitlesStreamInfo stream) -> stream.getSubtitles().toString().length())
                        .reversed()
        );

        streamsLowerLanguage.sort(
                Comparator.comparing((FullSubtitlesStreamInfo stream) -> stream.getSubtitles().toString().length())
                        .reversed()
        );

        return Optional.of(
                Merger.mergeSubtitles(
                        streamsUpperLanguage.get(0).getSubtitles(),
                        streamsLowerLanguage.get(0).getSubtitles()
                )
        );
    }

}
