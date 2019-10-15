package kirill.subtitles_merger;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitles_merger.ffprobe.Ffprobe;
import kirill.subtitles_merger.ffprobe.FfprobeSubtitlesInfo;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.prefs.Preferences;

@CommonsLog
public class Main {
    private static final String PREFERENCES_ROOT_NODE = "subtitlesmetger";

    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(
            "mp4",
            "mkv"
    );

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        Config config = getConfig(scanner);
        File directoryWithVideos = getDirectoryWithVideos(scanner);

        List<File> videoFiles = getVideoFiles(directoryWithVideos);
        if (CollectionUtils.isEmpty(videoFiles)) {
            log.info("no videos in the provided directory");
            return;
        }

        Ffprobe fFprobe = new Ffprobe(config.getFfprobePath());
        for (File file : videoFiles) {
            FfprobeSubtitlesInfo subtitlesInfo = fFprobe.getSubtitlesInfo(file);
            if (!CollectionUtils.isEmpty(subtitlesInfo.getSubtitleStreams())) {
                log.info("file " + file.getAbsolutePath() + " has " + subtitlesInfo.getSubtitleStreams().size() + " subs");
            }
        }
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

    private static List<File> getVideoFiles(File folder) {
        List<File> result = new ArrayList<>();

        File[] allFiles = folder.listFiles();
        Objects.requireNonNull(allFiles);

        for (File file : allFiles) {
            if (file.isFile()) {
                String extension = FilenameUtils.getExtension(file.getName());
                if (ALLOWED_VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
                    result.add(file);
                }
            }
        }

        return result;
    }
}
