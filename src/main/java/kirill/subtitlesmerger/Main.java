package kirill.subtitlesmerger;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.*;
import kirill.subtitlesmerger.logic.data.*;
import kirill.subtitlesmerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlesmerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlesmerger.logic.ffmpeg.json.JsonFfprobeFileInfo;
import kirill.subtitlesmerger.logic.ffmpeg.json.JsonStream;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.prefs.Preferences;

import static kirill.subtitlesmerger.logic.data.BriefFileInfo.UnavailabilityReason.*;

@CommonsLog
public class Main {
    private static final String PREFERENCES_ROOT_NODE = "subtitlesmerger";

    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Collections.singletonList("mkv");

    private static final List<String> ALLOWED_VIDEO_MIME_TYPES = Collections.singletonList("video/x-matroska");

    public static void main(String[] args) throws FfmpegException {
        Scanner scanner = new Scanner(System.in);

        Config config = getConfig(scanner);
        File directoryWithVideos = getDirectoryWithVideos(scanner);

        File[] directoryFiles = directoryWithVideos.listFiles();
        if (directoryFiles == null) {
            log.error("something is wrong with the directory " + directoryWithVideos.getAbsolutePath());
            throw new IllegalStateException();
        }

        Ffprobe ffprobe = new Ffprobe(config.getFfprobePath());
        Ffmpeg ffmpeg = new Ffmpeg(config.getFfmpegPath());

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

    private static Config getConfig(Scanner scanner) {
        Preferences preferences = Preferences.userRoot().node(PREFERENCES_ROOT_NODE);
        Config fromPreferences = getValidatedConfigFromPreferences();

        String ffmpegPath = fromPreferences.getFfmpegPath();
        String ffprobePath = fromPreferences.getFfprobePath();
        LanguageAlpha3Code upperLanguage = fromPreferences.getUpperLanguage();
        LanguageAlpha3Code lowerLanguage = fromPreferences.getLowerLanguage();

        while (StringUtils.isBlank(ffprobePath)) {
            System.out.println("please provide path to the ffprobe executable file");

            ffprobePath = scanner.nextLine();
            if (!StringUtils.isBlank(ffprobePath)) {
                try {
                    Ffprobe.validate(ffprobePath);
                } catch (FfmpegException e) {
                    System.out.println("path to the ffprobe executable file is incorrect");
                    ffprobePath = null;
                }
            }
        }

        while (StringUtils.isBlank(ffmpegPath)) {
            System.out.println("please provide path to the ffmpeg executable file");

            ffmpegPath = scanner.nextLine();
            if (!StringUtils.isBlank(ffmpegPath)) {
                try {
                    Ffmpeg.validate(ffmpegPath);
                } catch (FfmpegException e) {
                    System.out.println("path to the ffmpeg executable file is incorrect");
                    ffmpegPath = null;
                }
            }
        }

        while (upperLanguage == null) {
            System.out.println("please provide preferred language (ISO 639-2) to upper subtitles");

            upperLanguage = LanguageAlpha3Code.getByCodeIgnoreCase(scanner.nextLine());
            if (upperLanguage == null) {
                System.out.println("incorrect language format");
            }
        }

        while (lowerLanguage == null) {
            System.out.println("please provide preferred language (ISO 639-2) to lower subtitles");

            lowerLanguage = LanguageAlpha3Code.getByCodeIgnoreCase(scanner.nextLine());
            if (lowerLanguage == null) {
                System.out.println("incorrect language format");
            } else if (lowerLanguage == upperLanguage) {
                System.out.println("preferred languages have to be different");
                lowerLanguage = null;
            }
        }

        preferences.put("ffprobe_path", ffprobePath);
        preferences.put("ffmpeg_path", ffmpegPath);
        preferences.put("upper_language", upperLanguage.toString());
        preferences.put("lower_language", lowerLanguage.toString());

        return new Config(ffmpegPath, ffprobePath, upperLanguage, lowerLanguage);
    }

    private static Config getValidatedConfigFromPreferences() {
        Preferences preferences = Preferences.userRoot().node(PREFERENCES_ROOT_NODE);

        String ffprobePath = preferences.get("ffprobe_path", "");
        if (StringUtils.isBlank(ffprobePath)) {
            ffprobePath = null;
        } else {
            try {
                Ffprobe.validate(ffprobePath);
            } catch (FfmpegException e) {
                log.warn("incorrect ffprobe path in the preferences: " + ffprobePath);
                ffprobePath = null;
            }
        }

        String ffmpegPath = preferences.get("ffmpeg_path", "");
        if (StringUtils.isBlank(ffmpegPath)) {
            ffmpegPath = null;
        } else {
            try {
                Ffmpeg.validate(ffmpegPath);
            } catch (FfmpegException e) {
                log.warn("incorrect ffmpeg path in the preferences: " + ffmpegPath);
                ffmpegPath = null;
            }
        }

        LanguageAlpha3Code upperLanguage = null;
        String upperLanguageRaw = preferences.get("upper_language", "");
        if (!StringUtils.isBlank(upperLanguageRaw)) {
            upperLanguage = LanguageAlpha3Code.getByCodeIgnoreCase(upperLanguageRaw);
            if (upperLanguage == null) {
                log.warn("incorrect upper language in the preferences: " + upperLanguageRaw);
            }
        }

        LanguageAlpha3Code lowerLanguage = null;
        String lowerLanguageRaw = preferences.get("lower_language", "");
        if (!StringUtils.isBlank(lowerLanguageRaw)) {
            lowerLanguage = LanguageAlpha3Code.getByCodeIgnoreCase(lowerLanguageRaw);
            if (lowerLanguage == null) {
                log.warn("incorrect lower language in the preferences: " + lowerLanguageRaw);
            } else if (lowerLanguage == upperLanguage) {
                log.warn("languages are the same in the preferences: " + lowerLanguage);
            }
        }

        return new Config(ffmpegPath, ffprobePath, upperLanguage, lowerLanguage);
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

            if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
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

            if (!ALLOWED_VIDEO_MIME_TYPES.contains(mimeType)) {
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
        } catch (FfmpegException e) {
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