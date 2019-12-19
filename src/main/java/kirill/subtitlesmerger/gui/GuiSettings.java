package kirill.subtitlesmerger.gui;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import kirill.subtitlesmerger.logic.LogicConstants;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.FfmpegException;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

@CommonsLog
@Getter
public
class GuiSettings {
    private static final String PREFERENCES_ROOT_NODE = "subtitlesmerger";

    private Preferences preferences;

    private File upperSubtitlesLastDirectory;

    private File lowerSubtitlesLastDirectory;

    private File mergedSubtitlesLastDirectory;

    private File ffprobeFile;

    private File ffmpegFile;

    private LanguageAlpha3Code upperLanguage;

    private LanguageAlpha3Code lowerLanguage;

    private File lastDirectoryWithVideos;

    private ObservableSet<SettingType> missingSettings;

    GuiSettings() {
        preferences = Preferences.userRoot().node(PREFERENCES_ROOT_NODE);

        try {
            upperSubtitlesLastDirectory = getValidatedDirectory(
                    preferences.get(SettingType.UPPER_SUBTITLES_LAST_DIRECTORY.getSettingCode(), "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect upper subtitles last directory in saved preferences: " + e.getMessage());
        }

        try {
            lowerSubtitlesLastDirectory = getValidatedDirectory(
                    preferences.get(SettingType.LOWER_SUBTITLES_LAST_DIRECTORY.getSettingCode(), "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect lower subtitles last directory in saved preferences: " + e.getMessage());
        }

        try {
            mergedSubtitlesLastDirectory = getValidatedDirectory(
                    preferences.get(SettingType.MERGED_SUBTITLES_LAST_DIRECTORY.getSettingCode(), "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect merged subtitles last directory in saved preferences: " + e.getMessage());
        }

        try {
            ffprobeFile = getValidatedFfprobeFile(
                    preferences.get(SettingType.FFPROBE_PATH.getSettingCode(), "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect ffprobe path in saved preferences: " + e.getMessage());
        }

        try {
            ffmpegFile = getValidatedFfmpegFile(
                    preferences.get(SettingType.FFMPEG_PATH.getSettingCode(), "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect ffmpeg path in saved preferences: " + e.getMessage());
        }

        try {
            upperLanguage = getValidatedLanguage(
                    preferences.get(SettingType.UPPER_LANGUAGE.getSettingCode(), "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect upper language in saved preferences: " + e.getMessage());
        }

        try {
            lowerLanguage = getValidatedLanguage(
                    preferences.get(SettingType.LOWER_LANGUAGE.getSettingCode(), "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect lower language in saved preferences: " + e.getMessage());
        }

        try {
            lastDirectoryWithVideos = getValidatedDirectory(
                    preferences.get(SettingType.LAST_DIRECTORY_WITH_VIDEOS.getSettingCode(), "")
            ).orElse(null);
        } catch (ConfigException e) {
            log.warn("incorrect last directory with videos in saved preferences: " + e.getMessage());
        }

        missingSettings = generateMissingSettings();
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

        if (!LogicConstants.ALLOWED_LANGUAGE_CODES.contains(result)) {
            throw new ConfigException("language code " + rawValue + " is not allowed");
        }

        return Optional.of(result);
    }

    /*
     * Settings required for merging in videos.
     */
    private ObservableSet<SettingType> generateMissingSettings() {
        Set<SettingType> result = EnumSet.noneOf(SettingType.class);

        if (ffprobeFile == null) {
            result.add(SettingType.FFPROBE_PATH);
        }

        if (ffmpegFile == null) {
            result.add(SettingType.FFMPEG_PATH);
        }

        if (upperLanguage == null) {
            result.add(SettingType.UPPER_LANGUAGE);
        }

        if (lowerLanguage == null) {
            result.add(SettingType.LOWER_LANGUAGE);
        }

        return FXCollections.observableSet(result);
    }

    public void saveUpperSubtitlesLastDirectory(String rawValue) throws ConfigException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        this.upperSubtitlesLastDirectory = directory;
        preferences.put(SettingType.UPPER_SUBTITLES_LAST_DIRECTORY.getSettingCode(), directory.getAbsolutePath());
    }

    public void saveLowerSubtitlesLastDirectory(String rawValue) throws ConfigException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        this.lowerSubtitlesLastDirectory = directory;
        preferences.put(SettingType.LOWER_SUBTITLES_LAST_DIRECTORY.getSettingCode(), directory.getAbsolutePath());
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
        preferences.put(SettingType.FFPROBE_PATH.getSettingCode(), ffprobeFile.getAbsolutePath());
        missingSettings.remove(SettingType.FFPROBE_PATH);
    }

    public void saveFfmpegFile(String rawValue) throws ConfigException {
        File ffmpegFile = getValidatedFfmpegFile(rawValue).orElse(null);
        if (ffmpegFile == null) {
            throw new EmptyValueException();
        }

        this.ffmpegFile = ffmpegFile;
        preferences.put(SettingType.FFMPEG_PATH.getSettingCode(), ffmpegFile.getAbsolutePath());
        missingSettings.remove(SettingType.FFMPEG_PATH);
    }

    public void saveUpperLanguage(String rawValue) throws ConfigException {
        LanguageAlpha3Code language = getValidatedLanguage(rawValue).orElse(null);
        if (language == null) {
            throw new EmptyValueException();
        }

        this.upperLanguage = language;
        preferences.put(SettingType.UPPER_LANGUAGE.getSettingCode(), upperLanguage.toString());
        missingSettings.remove(SettingType.UPPER_LANGUAGE);
    }

    public void saveLowerLanguage(String rawValue) throws ConfigException {
        LanguageAlpha3Code language = getValidatedLanguage(rawValue).orElse(null);
        if (language == null) {
            throw new EmptyValueException();
        }

        this.lowerLanguage = language;
        preferences.put(SettingType.LOWER_LANGUAGE.getSettingCode(), lowerLanguage.toString());
        missingSettings.remove(SettingType.LOWER_LANGUAGE);
    }

    public void saveLastDirectoryWithVideos(String rawValue) throws ConfigException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        this.lastDirectoryWithVideos = directory;
        preferences.put(SettingType.LAST_DIRECTORY_WITH_VIDEOS.getSettingCode(), directory.getAbsolutePath());
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

    public enum SettingType {
        UPPER_SUBTITLES_LAST_DIRECTORY("upper_subtitles_last_directory"),
        LOWER_SUBTITLES_LAST_DIRECTORY("lower_subtitles_last_directory"),
        MERGED_SUBTITLES_LAST_DIRECTORY("merged_subtitles_last_directory"),
        FFPROBE_PATH("ffprobe_path"),
        FFMPEG_PATH("ffmpeg_path"),
        UPPER_LANGUAGE("upper_language"),
        LOWER_LANGUAGE("lower_language"),
        LAST_DIRECTORY_WITH_VIDEOS("last_directory_with_videos");

        @Getter
        private String settingCode;

        SettingType(String settingCode) {
            this.settingCode = settingCode;
        }
    }
}
