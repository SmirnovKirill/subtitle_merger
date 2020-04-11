package kirill.subtitlemerger.logic.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
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
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

@CommonsLog
@Getter
public class Settings {
    private static final String PREFERENCES_ROOT_NODE = "subtitle_merger";

    private Preferences preferences;

    private File upperSubtitlesLastDirectory;

    private File lowerSubtitlesLastDirectory;

    private File mergedSubtitlesLastDirectory;

    private File ffprobeFile;

    private File ffmpegFile;

    private LanguageAlpha3Code upperLanguage;

    private LanguageAlpha3Code lowerLanguage;

    private MergeMode mergeMode;

    private boolean markMergedStreamAsDefault;

    private File lastDirectoryWithVideos;

    private SortBy sortBy;

    private SortDirection sortDirection;

    private File lastDirectoryWithExternalSubtitles;

    /**
     * Settings required for merging in videos.
     */
    private ObservableSet<SettingType> missingSettings;

    public Settings() {
        preferences = Preferences.userRoot().node(PREFERENCES_ROOT_NODE);

        try {
            upperSubtitlesLastDirectory = getValidatedDirectory(
                    preferences.get(SettingType.UPPER_SUBTITLES_LAST_DIRECTORY.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect upper subtitles last directory in saved preferences: " + e.getMessage());
        }

        try {
            lowerSubtitlesLastDirectory = getValidatedDirectory(
                    preferences.get(SettingType.LOWER_SUBTITLES_LAST_DIRECTORY.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect lower subtitles last directory in saved preferences: " + e.getMessage());
        }

        try {
            mergedSubtitlesLastDirectory = getValidatedDirectory(
                    preferences.get(SettingType.MERGED_SUBTITLES_LAST_DIRECTORY.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect merged subtitles last directory in saved preferences: " + e.getMessage());
        }

        try {
            ffprobeFile = getValidatedFfprobeFile(
                    preferences.get(SettingType.FFPROBE_PATH.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect ffprobe path in saved preferences: " + e.getMessage());
        }

        try {
            ffmpegFile = getValidatedFfmpegFile(
                    preferences.get(SettingType.FFMPEG_PATH.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect ffmpeg path in saved preferences: " + e.getMessage());
        }

        try {
            upperLanguage = getValidatedLanguage(
                    preferences.get(SettingType.UPPER_LANGUAGE.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect upper language in saved preferences: " + e.getMessage());
        }

        try {
            lowerLanguage = getValidatedLanguage(
                    preferences.get(SettingType.LOWER_LANGUAGE.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect lower language in saved preferences: " + e.getMessage());
        }

        try {
            mergeMode = getValidatedMergeMode(
                    preferences.get(SettingType.MERGE_MODE.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect merge mode in saved preferences: " + e.getMessage());
        }

        try {
            markMergedStreamAsDefault = getValidatedBoolean(
                    preferences.get(SettingType.MARK_MERGED_STREAM_AS_DEFAULT.getCode(), "")
            ).orElse(true);
        } catch (SettingException e) {
            log.warn("incorrect flag for marking stream as default in saved preferences: " + e.getMessage());
        }

        try {
            lastDirectoryWithVideos = getValidatedDirectory(
                    preferences.get(SettingType.LAST_DIRECTORY_WITH_VIDEOS.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect last directory with videos in saved preferences: " + e.getMessage());
        }

        try {
            sortBy = getValidatedSortBy(
                    preferences.get(SettingType.SORT_BY.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect sort by value in saved preferences: " + e.getMessage());
        }

        try {
            sortDirection = getValidatedSortDirection(
                    preferences.get(SettingType.SORT_DIRECTION.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect sort direction in saved preferences: " + e.getMessage());
        }

        try {
            lastDirectoryWithExternalSubtitles = getValidatedDirectory(
                    preferences.get(SettingType.LAST_DIRECTORY_WITH_EXTERNAL_SUBTITLES.getCode(), "")
            ).orElse(null);
        } catch (SettingException e) {
            log.warn("incorrect last directory with external subtitles in saved preferences: " + e.getMessage());
        }

        missingSettings = generateMissingSettings();

        setDefaultSettingsIfNecessary();
    }

    private static Optional<File> getValidatedDirectory(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        File result = new File(rawValue);
        if (!result.exists() || !result.isDirectory()) {
            throw new SettingException("file " + rawValue + " does not exist or is not a directory");
        }

        return Optional.of(result);
    }

    private static Optional<File> getValidatedFfprobeFile(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
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

        return Optional.of(result);
    }

    private static Optional<File> getValidatedFfmpegFile(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
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

        return Optional.of(result);
    }

    private static Optional<LanguageAlpha3Code> getValidatedLanguage(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        LanguageAlpha3Code result = LanguageAlpha3Code.getByCodeIgnoreCase(rawValue);
        if (result == null) {
            throw new SettingException("language code " + rawValue + " is not valid");
        }

        if (!LogicConstants.ALLOWED_LANGUAGE_CODES.contains(result)) {
            throw new SettingException("language code " + rawValue + " is not allowed");
        }

        return Optional.of(result);
    }

    private static Optional<MergeMode> getValidatedMergeMode(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        MergeMode result = EnumUtils.getEnum(MergeMode.class, rawValue);
        if (result == null) {
            throw new SettingException("value " + rawValue + " is not a valid merge mode");
        }

        return Optional.of(result);
    }

    private static Optional<Boolean> getValidatedBoolean(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        if ("true".equals(rawValue)) {
            return Optional.of(true);
        } else if ("false".equals(rawValue)) {
            return Optional.of(false);
        } else {
            throw new SettingException("value " + rawValue + " is not a valid boolean type");
        }
    }

    private static Optional<SortBy> getValidatedSortBy(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        SortBy result = EnumUtils.getEnum(SortBy.class, rawValue);
        if (result == null) {
            throw new SettingException("value " + rawValue + " is not a valid sort option");
        }

        return Optional.of(result);
    }

    private static Optional<SortDirection> getValidatedSortDirection(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            return Optional.empty();
        }

        SortDirection result = EnumUtils.getEnum(SortDirection.class, rawValue);
        if (result == null) {
            throw new SettingException("value " + rawValue + " is not a valid sort direction");
        }

        return Optional.of(result);
    }

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

        if (mergeMode == null) {
            result.add(SettingType.MERGE_MODE);
        }

        return FXCollections.observableSet(result);
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
                    ffprobeFile = getValidatedFfprobeFile(packedFfprobeFile.getAbsolutePath())
                            .orElseThrow(IllegalStateException::new);
                    saveFfprobeFile(ffprobeFile.getAbsolutePath());
                } catch (SettingException e) {
                    ffprobeFile = null;
                    log.error("failed to validate and save packed ffprobe: " + e.getMessage());
                }
            }
        }

        if (ffmpegFile == null) {
            File packedFfmpegFile = getPackedFfmpegFile().orElse(null);
            if (packedFfmpegFile != null) {
                try {
                    ffmpegFile = getValidatedFfmpegFile(packedFfmpegFile.getAbsolutePath())
                            .orElseThrow(IllegalStateException::new);
                    saveFfmpegFile(ffmpegFile.getAbsolutePath());
                } catch (SettingException e) {
                    ffmpegFile = null;
                    log.error("failed to validate and save packed ffmpeg: " + e.getMessage());
                }
            }
        }
    }

    private static Optional<File> getPackedFfprobeFile() {
        File folderWithJar = getFolderWithJar().orElse(null);
        if (folderWithJar == null) {
            return Optional.empty();
        }

        File result;
        if (SystemUtils.IS_OS_LINUX) {
            result = new File(folderWithJar, "ffmpeg/ffprobe");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            result = new File(folderWithJar, "ffmpeg/bin/ffprobe.exe");
        } else {
            return Optional.empty();
        }

        if (!result.exists()) {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    private static Optional<File> getFolderWithJar() {
        File result;
        try {
            result = new File(Settings.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParentFile();
        } catch (URISyntaxException e) {
            log.error("failed to get jar location: " + ExceptionUtils.getStackTrace(e));
            return Optional.empty();
        }

        if (result == null) {
            log.error("folder with jar is null, that shouldn't happen");
            return Optional.empty();
        }

        return Optional.of(result);
    }

    private static Optional<File> getPackedFfmpegFile() {
        File folderWithJar = getFolderWithJar().orElse(null);
        if (folderWithJar == null) {
            return Optional.empty();
        }

        File result;
        if (SystemUtils.IS_OS_LINUX) {
            result = new File(folderWithJar, "ffmpeg/ffmpeg");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            result = new File(folderWithJar, "ffmpeg/bin/ffmpeg.exe");
        } else {
            return Optional.empty();
        }

        if (!result.exists()) {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    public void saveUpperSubtitlesLastDirectory(String rawValue) throws SettingException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        upperSubtitlesLastDirectory = directory;
        preferences.put(SettingType.UPPER_SUBTITLES_LAST_DIRECTORY.getCode(), directory.getAbsolutePath());
    }

    public void saveLowerSubtitlesLastDirectory(String rawValue) throws SettingException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        lowerSubtitlesLastDirectory = directory;
        preferences.put(SettingType.LOWER_SUBTITLES_LAST_DIRECTORY.getCode(), directory.getAbsolutePath());
    }

    public void saveMergedSubtitlesLastDirectory(String rawValue) throws SettingException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        mergedSubtitlesLastDirectory = directory;
        preferences.put("merged_subtitles_last_directory", directory.getAbsolutePath());
    }

    public void saveFfprobeFile(String rawValue) throws SettingException {
        File ffprobeFile = getValidatedFfprobeFile(rawValue).orElse(null);
        if (ffprobeFile == null) {
            throw new EmptyValueException();
        }

        this.ffprobeFile = ffprobeFile;
        preferences.put(SettingType.FFPROBE_PATH.getCode(), ffprobeFile.getAbsolutePath());
        missingSettings.remove(SettingType.FFPROBE_PATH);
    }

    public void clearFfprobeFile() {
        ffprobeFile = null;
        preferences.remove(SettingType.FFPROBE_PATH.getCode());
        missingSettings.add(SettingType.FFPROBE_PATH);
    }

    public void saveFfmpegFile(String rawValue) throws SettingException {
        File ffmpegFile = getValidatedFfmpegFile(rawValue).orElse(null);
        if (ffmpegFile == null) {
            throw new EmptyValueException();
        }

        this.ffmpegFile = ffmpegFile;
        preferences.put(SettingType.FFMPEG_PATH.getCode(), ffmpegFile.getAbsolutePath());
        missingSettings.remove(SettingType.FFMPEG_PATH);
    }

    public void clearFfmpegFile() {
        ffmpegFile = null;
        preferences.remove(SettingType.FFMPEG_PATH.getCode());
        missingSettings.add(SettingType.FFMPEG_PATH);
    }

    public void saveUpperLanguage(String rawValue) throws SettingException {
        LanguageAlpha3Code language = getValidatedLanguage(rawValue).orElse(null);
        if (language == null) {
            throw new EmptyValueException();
        }

        upperLanguage = language;
        preferences.put(SettingType.UPPER_LANGUAGE.getCode(), upperLanguage.toString());
        missingSettings.remove(SettingType.UPPER_LANGUAGE);
    }

    public void saveLowerLanguage(String rawValue) throws SettingException {
        LanguageAlpha3Code language = getValidatedLanguage(rawValue).orElse(null);
        if (language == null) {
            throw new EmptyValueException();
        }

        lowerLanguage = language;
        preferences.put(SettingType.LOWER_LANGUAGE.getCode(), lowerLanguage.toString());
        missingSettings.remove(SettingType.LOWER_LANGUAGE);
    }

    public void saveMergeMode(String rawValue) throws SettingException {
        MergeMode mergeMode = getValidatedMergeMode(rawValue).orElse(null);
        if (mergeMode == null) {
            throw new EmptyValueException();
        }

        this.mergeMode = mergeMode;
        preferences.put(SettingType.MERGE_MODE.getCode(), mergeMode.toString());
        missingSettings.remove(SettingType.MERGE_MODE);
    }

    public void saveMarkMergedStreamAsDefault(String rawValue) throws SettingException {
        Boolean markMergedStreamAsDefault = getValidatedBoolean(rawValue).orElse(null);
        if (markMergedStreamAsDefault == null) {
            throw new EmptyValueException();
        }

        this.markMergedStreamAsDefault = markMergedStreamAsDefault;
        preferences.put(SettingType.MARK_MERGED_STREAM_AS_DEFAULT.getCode(), markMergedStreamAsDefault.toString());
    }

    public void saveLastDirectoryWithVideos(String rawValue) throws SettingException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        lastDirectoryWithVideos = directory;
        preferences.put(SettingType.LAST_DIRECTORY_WITH_VIDEOS.getCode(), directory.getAbsolutePath());
    }

    public void saveSortBy(String rawValue) throws SettingException {
        SortBy sortBy = getValidatedSortBy(rawValue).orElse(null);
        if (sortBy == null) {
            throw new EmptyValueException();
        }

        this.sortBy = sortBy;
        preferences.put(SettingType.SORT_BY.getCode(), sortBy.toString());
    }

    public void saveSortDirection(String rawValue) throws SettingException {
        SortDirection sortDirection = getValidatedSortDirection(rawValue).orElse(null);
        if (sortDirection == null) {
            throw new EmptyValueException();
        }

        this.sortDirection = sortDirection;
        preferences.put(SettingType.SORT_DIRECTION.getCode(), sortDirection.toString());
    }

    public void saveLastDirectoryWithExternalSubtitles(String rawValue) throws SettingException {
        File directory = getValidatedDirectory(rawValue).orElse(null);
        if (directory == null) {
            throw new EmptyValueException();
        }

        lastDirectoryWithExternalSubtitles = directory;
        preferences.put(SettingType.LAST_DIRECTORY_WITH_EXTERNAL_SUBTITLES.getCode(), directory.getAbsolutePath());
    }
    
    private static class EmptyValueException extends SettingException {
        EmptyValueException() {
            super("empty value");
        }
    }
}
