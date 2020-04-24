package kirill.subtitlemerger.logic.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.LogicConstants;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Optional;
import java.util.prefs.Preferences;

import static kirill.subtitlemerger.logic.settings.SettingType.*;

@CommonsLog
@Getter
public class Settings {
    private static final String PREFERENCES_ROOT_NODE = "subtitle-merger";

    private Preferences preferences;

    private File upperDirectory;

    private File lowerDirectory;

    private File mergedDirectory;

    private LanguageAlpha3Code upperLanguage;

    private LanguageAlpha3Code lowerLanguage;

    private MergeMode mergeMode;

    private boolean makeMergedStreamsDefault;

    private boolean plainTextSubtitles;

    private File videosDirectory;

    private SortBy sortBy;

    private SortDirection sortDirection;

    private File externalDirectory;

    public Settings() {
        preferences = Preferences.userRoot().node(PREFERENCES_ROOT_NODE);
        initSavedSettings(preferences);

        setDefaultSettings();
    }

    private void initSavedSettings(Preferences preferences) {
        upperDirectory = getSetting(UPPER_DIRECTORY, Settings::getValidatedDirectory, preferences).orElse(null);
        lowerDirectory = getSetting(LOWER_DIRECTORY, Settings::getValidatedDirectory, preferences).orElse(null);
        mergedDirectory = getSetting(MERGED_DIRECTORY, Settings::getValidatedDirectory, preferences).orElse(null);

        upperLanguage = getSetting(UPPER_LANGUAGE, Settings::getValidatedLanguage, preferences).orElse(null);
        lowerLanguage = getSetting(LOWER_LANGUAGE, Settings::getValidatedLanguage, preferences).orElse(null);
        mergeMode = getSetting(MERGE_MODE, Settings::getValidatedMergeMode, preferences).orElse(null);
        makeMergedStreamsDefault = getSetting(
                MAKE_MERGED_STREAMS_DEFAULT,
                Settings::getValidatedBoolean,
                preferences
        ).orElse(false);
        plainTextSubtitles = getSetting(PLAIN_TEXT_SUBTITLES, Settings::getValidatedBoolean, preferences).orElse(false);

        videosDirectory = getSetting(VIDEOS_DIRECTORY, Settings::getValidatedDirectory, preferences).orElse(null);
        sortBy = getSetting(SORT_BY, Settings::getValidatedSortBy, preferences).orElse(null);
        sortDirection = getSetting(SORT_DIRECTION, Settings::getValidatedSortDirection, preferences).orElse(null);
        externalDirectory = getSetting(EXTERNAL_DIRECTORY, Settings::getValidatedDirectory, preferences).orElse(null);
    }

    /**
     * @return the value of the given type from the preferences if the value is not empty and is valid.
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
            throw new SettingException("the file " + rawValue + " does not exist or is not a directory");
        }

        return result;
    }

    private static LanguageAlpha3Code getValidatedLanguage(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        LanguageAlpha3Code result = LanguageAlpha3Code.getByCodeIgnoreCase(rawValue);
        if (result == null) {
            throw new SettingException("the language code " + rawValue + " is not valid");
        }

        if (!LogicConstants.ALLOWED_LANGUAGES.contains(result)) {
            throw new SettingException("the language code " + rawValue + " is not allowed");
        }

        return result;
    }

    private static MergeMode getValidatedMergeMode(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        MergeMode result = EnumUtils.getEnum(MergeMode.class, rawValue);
        if (result == null) {
            throw new SettingException("the value " + rawValue + " is not a valid merge mode");
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
            throw new SettingException("the value " + rawValue + " is not a valid boolean type");
        }
    }

    private static SortBy getValidatedSortBy(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        SortBy result = EnumUtils.getEnum(SortBy.class, rawValue);
        if (result == null) {
            throw new SettingException("the value " + rawValue + " is not a valid sort option");
        }

        return result;
    }

    private static SortDirection getValidatedSortDirection(String rawValue) throws SettingException {
        if (StringUtils.isBlank(rawValue)) {
            throw new EmptyValueException();
        }

        SortDirection result = EnumUtils.getEnum(SortDirection.class, rawValue);
        if (result == null) {
            throw new SettingException("the value " + rawValue + " is not a valid sort direction");
        }

        return result;
    }

    private void setDefaultSettings() {
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
    }

    public void saveUpperDirectory(String rawValue) throws SettingException {
        upperDirectory = getValidatedDirectory(rawValue);
        preferences.put(UPPER_DIRECTORY.getCode(), upperDirectory.getAbsolutePath());
    }

    public void saveLowerDirectory(String rawValue) throws SettingException {
        lowerDirectory = getValidatedDirectory(rawValue);
        preferences.put(LOWER_DIRECTORY.getCode(), lowerDirectory.getAbsolutePath());
    }

    public void saveMergedDirectory(String rawValue) throws SettingException {
        mergedDirectory = getValidatedDirectory(rawValue);
        preferences.put(MERGED_DIRECTORY.getCode(), mergedDirectory.getAbsolutePath());
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

    public void saveMakeMergedStreamsDefault(String rawValue) throws SettingException {
        makeMergedStreamsDefault = getValidatedBoolean(rawValue);
        preferences.put(MAKE_MERGED_STREAMS_DEFAULT.getCode(), Boolean.toString(makeMergedStreamsDefault));
    }

    public void savePlainTextSubtitles(String rawValue) throws SettingException {
        plainTextSubtitles = getValidatedBoolean(rawValue);
        preferences.put(PLAIN_TEXT_SUBTITLES.getCode(), Boolean.toString(plainTextSubtitles));
    }

    public void saveVideosDirectory(String rawValue) throws SettingException {
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

    public void saveExternalDirectory(String rawValue) throws SettingException {
        externalDirectory = getValidatedDirectory(rawValue);
        preferences.put(EXTERNAL_DIRECTORY.getCode(), externalDirectory.getAbsolutePath());
    }
    
    private static class EmptyValueException extends SettingException {
        EmptyValueException() {
            super("an empty value");
        }
    }

    @FunctionalInterface
    private interface SettingValidator<T> {
        T getValidatedValue(String rawValue) throws SettingException;
    }
}
