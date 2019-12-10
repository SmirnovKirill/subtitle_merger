package kirill.subtitlesmerger.logic;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.merge_in_files.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.merge_in_files.ffmpeg.FfmpegException;
import kirill.subtitlesmerger.logic.merge_in_files.ffmpeg.Ffprobe;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Optional;
import java.util.prefs.Preferences;

@CommonsLog
@Getter
public
class Config {
    private Preferences preferences;

    private File upperSubtitlesLastDirectory;

    private File lowerSubtitlesLastDirectory;

    private File mergedSubtitlesLastDirectory;

    private File ffprobeFile;

    private File ffmpegFile;

    private LanguageAlpha3Code upperLanguage;

    private LanguageAlpha3Code lowerLanguage;

    private File lastDirectoryWithVideos;

    public Config() {
        preferences = Preferences.userRoot().node(Constants.PREFERENCES_ROOT_NODE);

        try {
            upperSubtitlesLastDirectory = getValidatedDirectory(
                    preferences.get("upper_subtitles_last_directory", "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect upper subtitles last directory in saved preference: " + e.getMessage());
        }

        try {
            lowerSubtitlesLastDirectory = getValidatedDirectory(
                    preferences.get("lower_subtitles_last_directory", "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect lower subtitles last directory in saved preference: " + e.getMessage());
        }

        try {
            mergedSubtitlesLastDirectory = getValidatedDirectory(
                    preferences.get("merged_subtitles_last_directory", "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect merged subtitles last directory in saved preference: " + e.getMessage());
        }

        try {
            ffprobeFile = getValidatedFfprobeFile(
                    preferences.get("ffprobe_path", "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect ffprobe path in saved preference: " + e.getMessage());
        }

        try {
            ffmpegFile = getValidatedFfmpegFile(
                    preferences.get("ffmpeg_path", "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect ffmpeg path in saved preference: " + e.getMessage());
        }

        try {
            upperLanguage = getValidatedLanguage(
                    preferences.get("upper_language", "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect upper language in saved preference: " + e.getMessage());
        }

        try {
            lowerLanguage = getValidatedLanguage(
                    preferences.get("lower_language", "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect lower language in saved preference: " + e.getMessage());
        }

        try {
            lastDirectoryWithVideos = getValidatedDirectory(
                    preferences.get("last_directory_with_videos", "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect last directory with videos in saved preference: " + e.getMessage());
        }
    }

    private static Optional<File> getValidatedDirectory(String rawValue) throws ConfigException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        File result = new File(rawValue);
        if (!result.exists() || !result.isDirectory()) {
            throw new ConfigException("file " + rawValue + " does not exist or is not a directory");
        }

        return Optional.of(result);
    }

    private static Optional<File> getValidatedFfprobeFile(String rawValue) throws ConfigException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        File result = new File(rawValue);
        try {
            Ffprobe.validate(result);
        } catch (FfmpegException e) {
            throw new ConfigException("file " + rawValue + " is not a valid path for ffprobe");
        }

        return Optional.of(result);
    }

    private static Optional<File> getValidatedFfmpegFile(String rawValue) throws ConfigException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        File result = new File(rawValue);
        try {
            Ffmpeg.validate(result);
        } catch (FfmpegException e) {
            throw new ConfigException("file " + rawValue + " is not a valid path for ffmpeg");
        }

        return Optional.of(result);
    }

    private static Optional<LanguageAlpha3Code> getValidatedLanguage(String rawValue) throws ConfigException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        LanguageAlpha3Code result = LanguageAlpha3Code.getByCodeIgnoreCase(rawValue);
        if (result == null) {
            throw new ConfigException("language code " + rawValue + " is not valid");
        }

        if (result.getUsage() == LanguageAlpha3Code.Usage.TERMINOLOGY) {
            throw new ConfigException("language code " + rawValue + " is a terminology code");
        }

        return Optional.of(result);
    }

    public void saveUpperSubtitlesLastDirectory(String rawValue) throws ConfigException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        this.upperSubtitlesLastDirectory = directory;
        preferences.put("upper_subtitles_last_directory", directory.getAbsolutePath());
    }

    public void saveLowerSubtitlesLastDirectory(String rawValue) throws ConfigException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        this.lowerSubtitlesLastDirectory = directory;
        preferences.put("lower_subtitles_last_directory", directory.getAbsolutePath());
    }

    public void saveMergedSubtitlesLastDirectory(String rawValue) throws ConfigException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        this.mergedSubtitlesLastDirectory = directory;
        preferences.put("merged_subtitles_last_directory", directory.getAbsolutePath());
    }

    public void saveFfprobeFile(String rawValue) throws ConfigException {
        File ffprobeFile = getValidatedFfprobeFile(rawValue).orElse(null);
        if (ffprobeFile == null) {
            throw new EmptyValueException();
        }

        this.ffprobeFile = ffprobeFile;
        preferences.put("ffprobe_path", ffprobeFile.getAbsolutePath());
    }

    public void saveFfmpegFile(String rawValue) throws ConfigException {
        File ffmpegFile = getValidatedFfmpegFile(rawValue).orElse(null);
        if (ffmpegFile == null) {
            throw new EmptyValueException();
        }

        this.ffmpegFile = ffmpegFile;
        preferences.put("ffmpeg_path", ffmpegFile.getAbsolutePath());
    }

    public void saveUpperLanguage(String rawValue) throws ConfigException {
        LanguageAlpha3Code language = getValidatedLanguage(rawValue).orElse(null);
        if (language == null) {
            throw new EmptyValueException();
        }

        this.upperLanguage = language;
        preferences.put("upper_language", upperLanguage.toString());
    }

    public void saveLowerLanguage(String rawValue) throws ConfigException {
        LanguageAlpha3Code language = getValidatedLanguage(rawValue).orElse(null);
        if (language == null) {
            throw new EmptyValueException();
        }

        this.lowerLanguage = language;
        preferences.put("lower_language", lowerLanguage.toString());
    }

    public void saveLastDirectoryWithVideos(String rawValue) throws ConfigException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        this.lastDirectoryWithVideos = directory;
        preferences.put("last_directory_with_videos", directory.getAbsolutePath());
    }

    public static class ConfigException extends Exception {
        ConfigException(String message) {
            super(message);
        }
    }

    public static class EmptyValueException extends ConfigException {
        EmptyValueException() {
            super("empty value");
        }
    }
}
