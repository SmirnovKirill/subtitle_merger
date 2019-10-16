package kirill.subtitles_merger;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitles_merger.ffmpeg.Ffmpeg;
import kirill.subtitles_merger.ffmpeg.Ffprobe;
import kirill.subtitles_merger.ffmpeg.json.JsonFfprobeFileInfo;
import kirill.subtitles_merger.ffmpeg.json.JsonStream;
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

import static kirill.subtitles_merger.FileUnavailabilityReason.*;

@CommonsLog
public class Main {
    private static final String PREFERENCES_ROOT_NODE = "subtitlesmerger";

    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Collections.singletonList("mkv");

    private static final List<String> ALLOWED_VIDEO_MIME_TYPES = Collections.singletonList("video/x-matroska");

    public static void main(String[] args) throws InterruptedException {
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
    }

    private static Config getConfig(Scanner scanner) {
        Preferences preferences = Preferences.userRoot().node(PREFERENCES_ROOT_NODE);

        boolean hasErrors;

        String ffmpegPath = preferences.get("ffmpeg_path", "");
        while (StringUtils.isBlank(ffmpegPath)) {
            System.out.println("please provide path to the ffmpeg executable file");
            ffmpegPath = scanner.nextLine();
            if (!StringUtils.isBlank(ffmpegPath)) {
                preferences.put("ffmpeg_path", ffmpegPath);
            }
            //todo валидация
        }

        String ffprobePath = preferences.get("ffprobe_path", "");
        while (StringUtils.isBlank(ffprobePath)) {
            System.out.println("please provide path to the ffprobe executable file");
            ffprobePath = scanner.nextLine();
            if (!StringUtils.isBlank(ffprobePath)) {
                preferences.put("ffprobe_path", ffprobePath);
            }
            //todo валидация
        }

        String upperLanguage = preferences.get("upper_language", "");
        hasErrors = false;
        while (StringUtils.isBlank(upperLanguage) || hasErrors) {
            if (!hasErrors) {
                System.out.println("please provide preferred language (ISO 639-2) to upper subtitles");
            }

            upperLanguage = scanner.nextLine();
            if (LanguageAlpha3Code.getByCodeIgnoreCase(upperLanguage) == null) {
                hasErrors = true;
                System.out.println("incorrect language format");
            } else {
                preferences.put("upper_language", upperLanguage);
                hasErrors = false;
            }
        }

        String lowerLanguage = preferences.get("lower_language", "");
        hasErrors = false;
        while (StringUtils.isBlank(lowerLanguage) || hasErrors) {
            if (!hasErrors) {
                System.out.println("please provide preferred language (ISO 639-2) to lower subtitles");
            }

            lowerLanguage = scanner.nextLine();
            if (LanguageAlpha3Code.getByCodeIgnoreCase(upperLanguage) == null) {
                hasErrors = true;
                System.out.println("incorrect language format");
            } else if (lowerLanguage.equalsIgnoreCase(upperLanguage)) {
                System.out.println("languages have to be different!");
                hasErrors = true;
            } else {
                preferences.put("lower_language", lowerLanguage);
                hasErrors = false;
            }
        }

        //todo валидация того что достали из preferences
        return new Config(
                ffmpegPath,
                ffprobePath,
                LanguageAlpha3Code.getByCodeIgnoreCase(upperLanguage),
                LanguageAlpha3Code.getByCodeIgnoreCase(lowerLanguage)
        );
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

    private static List<BriefFileInfo> getBriefFilesInfo(File[] files, Ffprobe ffprobe) throws InterruptedException {
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
                log.error("failed to get mime type of file " + file.getAbsolutePath() + ": " + ExceptionUtils.getStackTrace(e));
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
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                log.error("failed to get ffprobe info for file " + file.getAbsolutePath() + ": " + ExceptionUtils.getStackTrace(e));
                result.add(new BriefFileInfo(file, FAILED_TO_GET_FFPROBE_INFO, null, null));
                continue;
            }

            VideoFormat videoFormat = VideoFormat.from(ffprobeInfo.getFormat().getFormatName()).orElse(null);
            if (videoFormat == null) {
                result.add(new BriefFileInfo(file, NOT_ALLOWED_CONTAINER, null, null));
                continue;
            }

            result.add(new BriefFileInfo(file, null, videoFormat, getBriefSubtitlesInfo(ffprobeInfo)));
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
                                SubtitlesUnavailabilityReason.NOT_ALLOWED_CODEC,
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

        return Optional.ofNullable(LanguageAlpha3Code.getByCodeIgnoreCase(languageRaw));
    }

    private static Optional<String> getTitle(JsonStream stream) {
        if (MapUtils.isEmpty(stream.getTags())) {
            return Optional.empty();
        }

        return Optional.ofNullable(stream.getTags().get("title"));
    }
}
