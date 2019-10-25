package kirill.subtitles_merger;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitles_merger.ffmpeg.Ffmpeg;
import kirill.subtitles_merger.ffmpeg.FfmpegException;
import kirill.subtitles_merger.ffmpeg.Ffprobe;
import kirill.subtitles_merger.ffmpeg.json.JsonFfprobeFileInfo;
import kirill.subtitles_merger.ffmpeg.json.JsonStream;
import kirill.subtitles_merger.logic.Parser;
import kirill.subtitles_merger.logic.Subtitles;
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

import static kirill.subtitles_merger.BriefFileUnavailabilityReason.*;

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
                ffmpeg.addSubtitleToFile(
                        merged,
                        fullFileInfo.getAllSubtitles().size(),
                        fullFileInfo.getBriefInfo().getFile()
                );
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
                    ffprobePath = null;
                    System.out.println("path to the ffprobe executable file is incorrect");
                }
            }
        }

        while (StringUtils.isBlank(ffmpegPath)) {
            System.out.println("please provide path to the ffmpeg executable file");
            ffmpegPath = scanner.nextLine();
            if (!StringUtils.isBlank(ffmpegPath)) {
                try {
                    Ffprobe.validate(ffmpegPath);
                } catch (FfmpegException e) {
                    ffmpegPath = null;
                    System.out.println("path to the ffmpeg executable file is incorrect");
                }
            }
        }

        while (upperLanguage == null) {
            System.out.println("please provide preferred language (ISO 639-2) to upper subtitles");
            String upperLanguageRaw = scanner.nextLine();
            if (LanguageAlpha3Code.getByCodeIgnoreCase(upperLanguageRaw) == null) {
                System.out.println("incorrect language format");
            } else {
                upperLanguage = LanguageAlpha3Code.getByCodeIgnoreCase(upperLanguageRaw);
            }
        }

        while (lowerLanguage == null) {
            System.out.println("please provide preferred language (ISO 639-2) to lower subtitles");
            String lowerLanguageRaw = scanner.nextLine();
            if (LanguageAlpha3Code.getByCodeIgnoreCase(lowerLanguageRaw) == null) {
                System.out.println("incorrect language format");
            } else {
                lowerLanguage = LanguageAlpha3Code.getByCodeIgnoreCase(lowerLanguageRaw);
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
                log.warn("incorrect ffprobe path in config: " + ffprobePath);
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
                log.warn("incorrect ffmpeg path in config: " + ffmpegPath);
                ffmpegPath = null;
            }
        }

        LanguageAlpha3Code upperLanguage = null;
        String upperLanguageRaw = preferences.get("upper_language", "");
        if (!StringUtils.isBlank(upperLanguageRaw)) {
            if (LanguageAlpha3Code.getByCodeIgnoreCase(upperLanguageRaw) == null) {
                log.warn("incorrect upper language in config: " + upperLanguageRaw);
            } else {
                upperLanguage = LanguageAlpha3Code.getByCodeIgnoreCase(upperLanguageRaw);
            }
        }

        LanguageAlpha3Code lowerLanguage = null;
        String lowerLanguageRaw = preferences.get("lowe_language", "");
        if (!StringUtils.isBlank(lowerLanguageRaw)) {
            if (LanguageAlpha3Code.getByCodeIgnoreCase(lowerLanguageRaw) == null) {
                log.warn("incorrect lower language in config: " + lowerLanguageRaw);
            } else {
                lowerLanguage = LanguageAlpha3Code.getByCodeIgnoreCase(lowerLanguageRaw);
            }
        }

        return new Config(ffmpegPath, ffprobePath, upperLanguage, lowerLanguage);
    }

    private static File getDirectoryWithVideos(Scanner scanner) {
        String path = null;
        boolean hasErrors = false;

        while (StringUtils.isBlank(path) || hasErrors) {
            if (!hasErrors) {
                System.out.println("please provide full path to the directory with video files");
            }

            path = scanner.nextLine();
            if (!new File(path).exists()) {
                hasErrors = true;
                System.out.println("directory does not exist");
            } else if (!new File(path).isDirectory()) {
                hasErrors = true;
                System.out.println("specified path contains a file not a directory");
            } else {
                hasErrors = false;
            }
        }

        return new File(path);
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

    private static List<BriefSingleSubtitlesInfo> getBriefSubtitlesInfo(JsonFfprobeFileInfo ffprobeInfo) {
        List<BriefSingleSubtitlesInfo> result = new ArrayList<>();

        for (JsonStream stream : ffprobeInfo.getStreams()) {
            if (!"subtitle".equals(stream.getCodecType())) {
                continue;
            }

            SubtitlesCodec codec = SubtitlesCodec.from(stream.getCodecName()).orElse(null);
            if (codec == null) {
                result.add(
                        new BriefSingleSubtitlesInfo(
                                stream.getIndex(),
                                null,
                                BriefSubtitlesUnavailabilityReason.NOT_ALLOWED_CODEC,
                                getLanguage(stream).orElse(null),
                                getTitle(stream).orElse(null)
                        )
                );
                continue;
            }

            result.add(
                    new BriefSingleSubtitlesInfo(
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
         * В описании формата https://www.ffmpeg.org/ffmpeg-formats.html#matroska сказано:
         * The language can be either the 3 letters bibliographic ISO-639-2 (ISO 639-2/B) form (like "fre" for French),
         * or a language code mixed with a country code for specialities in languages
         * (like "fre-ca" for Canadian French).
         * Поэтому можно засплиттить по дефису и использовать первую часть как код в ISO-639.
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
                    FullFileUnavailabilityReason.FAILED_BEFORE,
                    wrap(briefFileInfo.getAllSubtitles())
            );
        }

        try {
            List<FullSingleSubtitlesInfo> allSubtitles = new ArrayList<>();

            for (BriefSingleSubtitlesInfo briefSubtitlesInfo : briefFileInfo.getAllSubtitles()) {
                if (briefFileInfo.getUnavailabilityReason() != null) {
                    allSubtitles.add(
                            new FullSingleSubtitlesInfo(
                                    briefSubtitlesInfo,
                                    briefSubtitlesInfo.getUnavailabilityReason() != null
                                            ? FullSingleSubtitlesUnavailabilityReason.FAILED_BEFORE
                                            : null,
                                    null
                            )
                    );
                    continue;
                }

                String subtitlesText = ffmpeg.getSubtitlesText(briefSubtitlesInfo.getIndex(), briefFileInfo.getFile());
                allSubtitles.add(
                        new FullSingleSubtitlesInfo(
                                briefSubtitlesInfo,
                                null,
                                Parser.parseSubtitles(
                                        subtitlesText,
                                        "subs-" + briefSubtitlesInfo.getIndex(),
                                        briefSubtitlesInfo.getLanguage()
                                )
                        )
                );
            }

            return new FullFileInfo(briefFileInfo, null, allSubtitles);
        } catch (FfmpegException e) {
            return new FullFileInfo(
                    briefFileInfo,
                    FullFileUnavailabilityReason.FFMPEG_FAILED,
                    wrap(briefFileInfo.getAllSubtitles())
            );
        }
    }

    private static List<FullSingleSubtitlesInfo> wrap(List<BriefSingleSubtitlesInfo> allBiefSubtitlesInfo) {
        if (allBiefSubtitlesInfo == null) {
            return null;
        }

        List<FullSingleSubtitlesInfo> result = new ArrayList<>();

        for (BriefSingleSubtitlesInfo briefSubtitlesInfo : allBiefSubtitlesInfo) {
            result.add(
                    new FullSingleSubtitlesInfo(
                            briefSubtitlesInfo,
                            briefSubtitlesInfo.getUnavailabilityReason() != null
                                    ? FullSingleSubtitlesUnavailabilityReason.FAILED_BEFORE
                                    : null,
                            null
                    ));
        }

        return result;
    }
}
