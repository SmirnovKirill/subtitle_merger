package kirill.subtitlemerger.logic.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.prefs.Preferences;

import static kirill.subtitlemerger.logic.settings.SettingType.*;

@CommonsLog
@Getter
public class Settings {
    private static final String PREFERENCES_ROOT_NODE = "subtitle-merger";

    private Preferences preferences;

    private File upperSubtitlesDirectory;

    private File lowerSubtitlesDirectory;

    private File mergedSubtitlesDirectory;

    private File ffprobeFile;

    private File ffmpegFile;

    private LanguageAlpha3Code upperLanguage;

    private LanguageAlpha3Code lowerLanguage;

    private MergeMode mergeMode;

    private boolean makeMergedStreamsDefault;

    private File videosDirectory;

    private SortBy sortBy;

    private SortDirection sortDirection;

    private File externalSubtitlesDirectory;

    public Settings() {
        preferences = Preferences.userRoot().node(PREFERENCES_ROOT_NODE);
        initSavedSettings(preferences);

        setDefaultSettingsIfNecessary();
    }

    private void initSavedSettings(Preferences preferences) {
        upperSubtitlesDirectory = getSetting(
                UPPER_SUBTITLES_DIRECTORY,
                Settings::getValidatedDirectory,
                preferences
        ).orElse(null);

        lowerSubtitlesDirectory = getSetting(
                LOWER_SUBTITLES_DIRECTORY,
                Settings::getValidatedDirectory,
                preferences
        ).orElse(null);

        mergedSubtitlesDirectory = getSetting(
                MERGED_SUBTITLES_DIRECTORY,
                Settings::getValidatedDirectory,
                preferences
        ).orElse(null);

        ffprobeFile = getSetting(FFPROBE_PATH, Settings::getValidatedFfprobeFile, preferences).orElse(null);
        ffmpegFile = getSetting(FFMPEG_PATH, Settings::getValidatedFfmpegFile, preferences).orElse(null);
        upperLanguage = getSetting(UPPER_LANGUAGE, Settings::getValidatedLanguage, preferences).orElse(null);
        lowerLanguage = getSetting(LOWER_LANGUAGE, Settings::getValidatedLanguage, preferences).orElse(null);
        mergeMode = getSetting(MERGE_MODE, Settings::getValidatedMergeMode, preferences).orElse(null);

        makeMergedStreamsDefault = getSetting(
                MAKE_MERGED_STREAMS_DEFAULT,
                Settings::getValidatedBoolean,
                preferences
        ).orElse(false);

        videosDirectory = getSetting(VIDEOS_DIRECTORY, Settings::getValidatedDirectory, preferences).orElse(null);
        sortBy = getSetting(SORT_BY, Settings::getValidatedSortBy, preferences).orElse(null);
        sortDirection = getSetting(SORT_DIRECTION, Settings::getValidatedSortDirection, preferences).orElse(null);

        externalSubtitlesDirectory = getSetting(
                EXTERNAL_SUBTITLES_DIRECTORY,
                Settings::getValidatedDirectory,
                preferences
        ).orElse(null);
    }

    /**
     * @return value of the given type from the preferences if the value is not empty and is valid.
     */
    private static <T> Optional<T> getSetting(
            SettingType settingType,
            SettingValidator<T> validator,
            Preferences preferences
    ) {
        String rawValue = preferences.get(settingType.getCode(), "");
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        try {
            return Optional.of(validator.getValidatedValue(rawValue));
        } catch (SettingException e) {
            log.warn("incorrect value for " + settingType.getCode() + " in saved preferences: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static File getValidatedDirectory(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        File result = new File(rawValue);
        if (!result.exists() || !result.isDirectory()) {
            throw new SettingException("file " + rawValue + " does not exist or is not a directory");
        }

        return result;
    }

    private static File getValidatedFfprobeFile(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        File result = new File(rawValue);
        try {
            Ffprobe.validate(result);
        } catch (FfmpegException e) {
            log.warn("incorrect path to ffprobe: " + e.getCode() + ", console output " + e.getConsoleOutput());
            throw new SettingException("file " + rawValue + " is not a valid path to ffprobe");
        } catch (InterruptedException e) {
            throw new SettingException("something's not right, process can't be interrupted");
        }

        return result;
    }

    private static File getValidatedFfmpegFile(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        File result = new File(rawValue);
        try {
            Ffmpeg.validate(result);
        } catch (FfmpegException e) {
            log.warn("incorrect path to ffmpeg: " + e.getCode() + ", console output " + e.getConsoleOutput());
            throw new SettingException("file " + rawValue + " is not a valid path to ffmpeg");
        } catch (InterruptedException e) {
            throw new SettingException("something's not right, process can't be interrupted");
        }

        return result;
    }

    private static LanguageAlpha3Code getValidatedLanguage(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        LanguageAlpha3Code result = LanguageAlpha3Code.getByCodeIgnoreCase(rawValue);
        if (result == null) {
            throw new SettingException("language code " + rawValue + " is not valid");
        }

        if (!LogicConstants.ALLOWED_LANGUAGE_CODES.contains(result)) {
            throw new SettingException("language code " + rawValue + " is not allowed");
        }

        return result;
    }

    private static MergeMode getValidatedMergeMode(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        MergeMode result = EnumUtils.getEnum(MergeMode.class, rawValue);
        if (result == null) {
            throw new SettingException("value " + rawValue + " is not a valid merge mode");
        }

        return result;
    }

    private static Boolean getValidatedBoolean(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        if ("true".equals(rawValue)) {
            return true;
        } else if ("false".equals(rawValue)) {
            return false;
        } else {
            throw new SettingException("value " + rawValue + " is not a valid boolean type");
        }
    }

    private static SortBy getValidatedSortBy(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        SortBy result = EnumUtils.getEnum(SortBy.class, rawValue);
        if (result == null) {
            throw new SettingException("value " + rawValue + " is not a valid sort option");
        }

        return result;
    }

    private static SortDirection getValidatedSortDirection(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        SortDirection result = EnumUtils.getEnum(SortDirection.class, rawValue);
        if (result == null) {
            throw new SettingException("value " + rawValue + " is not a valid sort direction");
        }

        return result;
    }

    private void setDefaultSettingsIfNecessary() {
        try {
            if (sortBy == null) {
                saveSortBy(SortBy.MODIFICATION_TIME.toString());
            }

            if (sortDirection == null) {
                saveSortDirection(SortDirection.ASCENDING.toString());
            }
        } catch (SettingException e) {
            log.error("failed to save sort parameters, should not happen: " + e.getMessage());
        }

        if (ffprobeFile == null) {
            File packedFfprobeFile = getPackedFfprobeFile().orElse(null);
            if (packedFfprobeFile != null) {
                try {
                    saveFfprobeFile(packedFfprobeFile.getAbsolutePath());
                } catch (SettingException e) {
                    log.error("failed to validate and save packed ffprobe: " + e.getMessage());
                }
            }
        }

        if (ffmpegFile == null) {
            File packedFfmpegFile = getPackedFfmpegFile().orElse(null);
            if (packedFfmpegFile != null) {
                try {
                    saveFfmpegFile(packedFfmpegFile.getAbsolutePath());
                } catch (SettingException e) {
                    log.error("failed to validate and save packed ffmpeg: " + e.getMessage());
                }
            }
        }
    }

    private static Optional<File> getPackedFfprobeFile() {
        File directoryWithJar;
        try {
            directoryWithJar = getDirectoryWithJar();
        } catch (IllegalStateException e) {
            return Optional.empty();
        }

        File result;
        if (SystemUtils.IS_OS_LINUX) {
            result = new File(directoryWithJar, "ffmpeg/ffprobe");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            result = new File(directoryWithJar, "ffmpeg/bin/ffprobe.exe");
        } else {
            return Optional.empty();
        }

        if (!result.exists()) {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    /**
     * @return the directory containing jar file that is running.
     * @throws IllegalStateException if directory can't be located for some reason
     */
    private static File getDirectoryWithJar() {
        File jar;
        try {
            jar = new File(Settings.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            log.error("failed to get jar location: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }

        File result = jar.getParentFile();
        if (result == null) {
            log.error("folder with jar is null, that shouldn't happen");
            throw new NullPointerException();
        }

        return result;
    }

    private static Optional<File> getPackedFfmpegFile() {
        File directoryWithJar;
        try {
            directoryWithJar = getDirectoryWithJar();
        } catch (IllegalStateException e) {
            return Optional.empty();
        }

        File result;
        if (SystemUtils.IS_OS_LINUX) {
            result = new File(directoryWithJar, "ffmpeg/ffmpeg");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            result = new File(directoryWithJar, "ffmpeg/bin/ffmpeg.exe");
        } else {
            return Optional.empty();
        }

        if (!result.exists()) {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    public void saveUpperSubtitlesDirectory(String rawValue) throws SettingException {
        upperSubtitlesDirectory = getValidatedDirectory(rawValue);
        preferences.put(UPPER_SUBTITLES_DIRECTORY.getCode(), upperSubtitlesDirectory.getAbsolutePath());
    }

    public void saveLowerSubtitlesDirectory(String rawValue) throws SettingException {
        lowerSubtitlesDirectory = getValidatedDirectory(rawValue);
        preferences.put(LOWER_SUBTITLES_DIRECTORY.getCode(), lowerSubtitlesDirectory.getAbsolutePath());
    }

    public void saveMergedSubtitlesDirectory(String rawValue) throws SettingException {
        mergedSubtitlesDirectory = getValidatedDirectory(rawValue);
        preferences.put(MERGED_SUBTITLES_DIRECTORY.getCode(), mergedSubtitlesDirectory.getAbsolutePath());
    }

    public void saveFfprobeFile(String rawValue) throws SettingException {
        ffprobeFile = getValidatedFfprobeFile(rawValue);
        preferences.put(FFPROBE_PATH.getCode(), ffprobeFile.getAbsolutePath());
    }

    public void clearFfprobeFile() {
        ffprobeFile = null;
        preferences.remove(FFPROBE_PATH.getCode());
    }

    public void saveFfmpegFile(String rawValue) throws SettingException {
        ffmpegFile = getValidatedFfmpegFile(rawValue);
        preferences.put(FFMPEG_PATH.getCode(), ffmpegFile.getAbsolutePath());
    }

    public void clearFfmpegFile() {
        ffmpegFile = null;
        preferences.remove(FFMPEG_PATH.getCode());
    }

    public void saveUpperLanguage(String rawValue) throws SettingException {
        upperLanguage = getValidatedLanguage(rawValue);
        preferences.put(UPPER_LANGUAGE.getCode(), upperLanguage.toString());
    }

    public void saveLowerLanguage(String rawValue) throws SettingException {
        lowerLanguage = getValidatedLanguage(rawValue);
        preferences.put(LOWER_LANGUAGE.getCode(), lowerLanguage.toString());
    }

    public void saveMergeMode(String rawValue) throws SettingException {
        mergeMode = getValidatedMergeMode(rawValue);
        preferences.put(MERGE_MODE.getCode(), mergeMode.toString());
    }

    public void saveMarkMergedStreamAsDefault(String rawValue) throws SettingException {
        makeMergedStreamsDefault = getValidatedBoolean(rawValue);
        preferences.put(MAKE_MERGED_STREAMS_DEFAULT.getCode(), Boolean.toString(makeMergedStreamsDefault));
    }

    public void saveDirectoryWithVideos(String rawValue) throws SettingException {
        videosDirectory = getValidatedDirectory(rawValue);
        preferences.put(VIDEOS_DIRECTORY.getCode(), videosDirectory.getAbsolutePath());
    }

    public void saveSortBy(String rawValue) throws SettingException {
        sortBy = getValidatedSortBy(rawValue);
        preferences.put(SORT_BY.getCode(), sortBy.toString());
    }

    public void saveSortDirection(String rawValue) throws SettingException {
        sortDirection = getValidatedSortDirection(rawValue);
        preferences.put(SORT_DIRECTION.getCode(), sortDirection.toString());
    }

    public void saveDirectoryWithExternalSubtitles(String rawValue) throws SettingException {
        externalSubtitlesDirectory = getValidatedDirectory(rawValue);
        preferences.put(EXTERNAL_SUBTITLES_DIRECTORY.getCode(), externalSubtitlesDirectory.getAbsolutePath());
    }
    
    private static class EmptyValueException extends SettingException {
        EmptyValueException() {
            super("empty value");
        }
    }

    @FunctionalInterface
    private interface SettingValidator<T> {
        T getValidatedValue(String rawValue) throws SettingException;
    }
}
