package kirill.subtitlesmerger;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.Constants;
import kirill.subtitlesmerger.logic.core.Parser;
import kirill.subtitlesmerger.logic.core.entities.Subtitles;
import kirill.subtitlesmerger.logic.merge_in_videos.*;
import kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.FfmpegException;
import kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.Ffprobe;
import kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.json.JsonFfprobeFileInfo;
import kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.json.JsonStream;
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
import java.util.Scanner;

import static kirill.subtitlesmerger.logic.merge_in_videos.BriefFileInfo.UnavailabilityReason.*;

@CommonsLog
public class Main {
    public static void main(String[] args) throws FfmpegException {
        Scanner scanner = new Scanner(System.in);

        Config config = new Config();
        File directoryWithVideos = getDirectoryWithVideos(scanner);

        File[] directoryFiles = directoryWithVideos.listFiles();
        if (directoryFiles == null) {
            log.error("something is wrong with the directory " + directoryWithVideos.getAbsolutePath());
            throw new IllegalStateException();
        }

        Ffprobe ffprobe = new Ffprobe(config.getFfprobeFile());
        Ffmpeg ffmpeg = new Ffmpeg(config.getFfmpegFile());

        List<BriefFileInfo> briefFilesInfo = getBriefFilesInfo(directoryFiles, ffprobe);

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

    private static File getDirectoryWithVideos(Scanner scanner) {
        File result = null;

        while (result == null) {
            System.out.println("please provide full path to the directory with video files");

            result = new File(scanner.nextLine());
            if (!result.exists()) {
                System.out.println("directory does not exist");
                result = null;
            } else if (!result.isDirectory()) {
                System.out.println("specified path contains a file not a directory");
                result = null;
            }
        }

        return result;
    }

    private static List<BriefFileInfo> getBriefFilesInfo(File[] files, Ffprobe ffprobe) {
        List<BriefFileInfo> result = new ArrayList<>();

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String extension = FilenameUtils.getExtension(file.getName());
            if (StringUtils.isBlank(extension)) {
                result.add(new BriefFileInfo(file, NO_EXTENSION, null, null));
                continue;
            }

            if (!Constants.ALLOWED_VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
                result.add(new BriefFileInfo(file, NOT_ALLOWED_EXTENSION, null, null));
                continue;
            }

            String mimeType;
            try {
                mimeType = Files.probeContentType(file.toPath());
            } catch (IOException e) {
                log.error("failed to get mime type of file " + file.getAbsolutePath() + ": "
                        + ExceptionUtils.getStackTrace(e)
                );
                result.add(new BriefFileInfo(file, FAILED_TO_GET_MIME_TYPE, null, null));
                continue;
            }

            if (!Constants.ALLOWED_VIDEO_MIME_TYPES.contains(mimeType)) {
                result.add(new BriefFileInfo(file, NOT_ALLOWED_MIME_TYPE, null, null));
                continue;
            }

            JsonFfprobeFileInfo ffprobeInfo;
            try {
                ffprobeInfo = ffprobe.getFileInfo(file);
            } catch (Exception e) {
                log.error("failed to get ffprobe info for file " + file.getAbsolutePath() + ": "
                        + ExceptionUtils.getStackTrace(e)
                );
                result.add(new BriefFileInfo(file, FAILED_TO_GET_FFPROBE_INFO, null, null));
                continue;
            }

            VideoFormat videoFormat = VideoFormat.from(ffprobeInfo.getFormat().getFormatName()).orElse(null);
            if (videoFormat == null) {
                result.add(new BriefFileInfo(file, NOT_ALLOWED_CONTAINER, null, null));
                continue;
            }

            result.add(
                    new BriefFileInfo(file, null, videoFormat, getBriefSubtitlesInfo(ffprobeInfo))
            );
        }

        return result;
    }

    private static List<BriefSubtitlesStreamInfo> getBriefSubtitlesInfo(JsonFfprobeFileInfo ffprobeInfo) {
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
                                Parser.parseSubtitles(
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
}
