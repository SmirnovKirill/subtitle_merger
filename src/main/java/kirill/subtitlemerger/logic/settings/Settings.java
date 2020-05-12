package kirill.subtitlemerger.logic.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.LogicConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import static kirill.subtitlemerger.logic.settings.SettingType.*;

@CommonsLog
public class Settings {
    private static final String PREFERENCES_ROOT_NODE = "subtitle-merger";

    private Preferences preferences;

    private Map<SettingType, Object> settings;

    public Settings() {
        preferences = Preferences.userRoot().node(PREFERENCES_ROOT_NODE);
        settings = getSavedSettings(preferences);
        setDefaultSettings(settings);
    }

    private static Map<SettingType, Object> getSavedSettings(Preferences preferences) {
        Map<SettingType, Object> result = new HashMap<>();

        for (SettingType settingType : SettingType.values()) {
            String rawValue = preferences.get(settingType.getCode(), "");
            if (StringUtils.isEmpty(rawValue)) {
                result.put(settingType, null);
                continue;
            }

            try {
                Object convertedObject = stringToObject(rawValue, settingType);
                validateObject(convertedObject, settingType);
                result.put(settingType, convertedObject);
            } catch (SettingException e) {
                log.warn("incorrect " + settingType + " value in saved settings: " + e.getMessage());
                result.put(settingType, null);
            }
        }

        return result;
    }

    private static Object stringToObject(String string, SettingType settingType) {
        if (StringUtils.isEmpty(string)) {
            return null;
        }

        switch (settingType) {
            case UPPER_DIRECTORY:
            case LOWER_DIRECTORY:
            case MERGED_DIRECTORY:
            case VIDEO_DIRECTORY:
            case VIDEO_SUBTITLE_DIRECTORY:
                return new File(string);
            case UPPER_LANGUAGE:
            case LOWER_LANGUAGE:
                LanguageAlpha3Code languageCode = LanguageAlpha3Code.getByCodeIgnoreCase(string);
                if (languageCode == null) {
                    throw new SettingException("incorrect language code: " + string);
                }
                return languageCode;
            case MERGE_MODE:
                MergeMode mergeMode = EnumUtils.getEnum(MergeMode.class, string);
                if (mergeMode == null) {
                    throw new SettingException("incorrect merge code: " + string);
                }
                return mergeMode;
            case MAKE_MERGED_STREAMS_DEFAULT:
            case PLAIN_TEXT_SUBTITLES:
                if ("true".equals(string)) {
                    return true;
                } else if ("false".equals(string)) {
                    return false;
                } else {
                    throw new SettingException("incorrect boolean value: " + string);
                }
            case SORT_BY:
                SortBy sortBy = EnumUtils.getEnum(SortBy.class, string);
                if (sortBy == null) {
                    throw new SettingException("incorrect sort by: " + string);
                }
                return sortBy;
            case SORT_DIRECTION:
                SortDirection sortDirection = EnumUtils.getEnum(SortDirection.class, string);
                if (sortDirection == null) {
                    throw new SettingException("incorrect sort direction: " + string);
                }
                return sortDirection;
            default:
                log.error("unexpected setting type: " + settingType + ", most likely a bug");
                throw new IllegalStateException();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static void validateObject(Object object, SettingType settingType) throws SettingException {
        if (!settingType.getObjectClass().isInstance(object)) {
            log.error("incorrect class for object of " + settingType + ", most likely a bug");
            throw new IllegalStateException();
        }

        switch (settingType) {
            case UPPER_DIRECTORY:
            case LOWER_DIRECTORY:
            case MERGED_DIRECTORY:
            case VIDEO_DIRECTORY:
            case VIDEO_SUBTITLE_DIRECTORY:
                validateDirectory((File) object);
                return;
            case UPPER_LANGUAGE:
            case LOWER_LANGUAGE:
                validateLanguage((LanguageAlpha3Code) object);
                return;
            case MERGE_MODE:
            case MAKE_MERGED_STREAMS_DEFAULT:
            case PLAIN_TEXT_SUBTITLES:
            case SORT_BY:
            case SORT_DIRECTION:
                /*
                 * Don't do anything here because if the object was converted then it's valid.
                 */
                return;
            default:
                log.error("unexpected setting type: " + settingType + ", most likely a bug");
                throw new IllegalStateException();
        }
    }

    private static void validateDirectory(File directory) {
        if (!directory.isDirectory()) {
            throw new SettingException(directory.getAbsolutePath() + " does not exist or is not a directory");
        }
    }

    private static void validateLanguage(LanguageAlpha3Code language) {
        if (!LogicConstants.ALLOWED_LANGUAGES.contains(language)) {
            throw new SettingException("language " + language.getName() + " is not allowed");
        }
    }

    private static void setDefaultSettings(Map<SettingType, Object> savedSettings) {
        savedSettings.putIfAbsent(SORT_BY, SortBy.MODIFICATION_TIME);
        savedSettings.putIfAbsent(SORT_DIRECTION, SortDirection.ASCENDING);
        savedSettings.putIfAbsent(MAKE_MERGED_STREAMS_DEFAULT, false);
        savedSettings.putIfAbsent(PLAIN_TEXT_SUBTITLES, false);
    }

    public File getUpperDirectory() {
        return (File) settings.get(UPPER_DIRECTORY);
    }

    public File getLowerDirectory() {
        return (File) settings.get(LOWER_DIRECTORY);
    }

    public File getMergedDirectory() {
        return (File) settings.get(MERGED_DIRECTORY);
    }

    public LanguageAlpha3Code getUpperLanguage() {
        return (LanguageAlpha3Code) settings.get(UPPER_LANGUAGE);
    }

    public LanguageAlpha3Code getLowerLanguage() {
        return (LanguageAlpha3Code) settings.get(LOWER_LANGUAGE);
    }

    public MergeMode getMergeMode() {
        return (MergeMode) settings.get(MERGE_MODE);
    }

    public boolean isMakeMergedStreamsDefault() {
        return Boolean.TRUE.equals(settings.get(MAKE_MERGED_STREAMS_DEFAULT));
    }

    public boolean isPlainTextSubtitles() {
        return Boolean.TRUE.equals(settings.get(PLAIN_TEXT_SUBTITLES));
    }

    public File getVideoDirectory() {
        return (File) settings.get(VIDEO_DIRECTORY);
    }

    public File getVideoSubtitleDirectory() {
        return (File) settings.get(VIDEO_SUBTITLE_DIRECTORY);
    }

    public Sort getSort() {
        return new Sort(getSortBy(), getSortDirection());
    }

    @SuppressWarnings("WeakerAccess")
    public SortBy getSortBy() {
        return (SortBy) settings.get(SORT_BY);
    }

    @SuppressWarnings("WeakerAccess")
    public SortDirection getSortDirection() {
        return (SortDirection) settings.get(SORT_DIRECTION);
    }

    /**
     * Saves the given setting value without throwing any exceptions. If the value is incorrect then there will be a log
     * warning record.
     * It was designed mostly for saving directories because it's never enough just to check the directory before
     * saving, it can always be removed during the saving process and thus become invalid.
     */
    public void saveQuietly(Object object, SettingType settingType) {
        try {
            save(object, settingType);
        } catch (SettingException e) {
            log.warn("failed to save " + settingType + ": " + e.getMessage());
        }
    }

    /**
     * Saves the given setting value if it is valid and throws an exception otherwise.
     * @throws SettingException if the value is incorrect
     */
    public void save(Object object, SettingType settingType) {
        validateObject(object, settingType);
        preferences.put(settingType.getCode(), objectToString(object, settingType));
        settings.put(settingType, object);
    }

    private static String objectToString(Object object, SettingType settingType) {
        if (object == null) {
            return null;
        }

        switch (settingType) {
            case UPPER_DIRECTORY:
            case LOWER_DIRECTORY:
            case MERGED_DIRECTORY:
            case VIDEO_DIRECTORY:
            case VIDEO_SUBTITLE_DIRECTORY:
                return ((File) object).getAbsolutePath();
            case UPPER_LANGUAGE:
            case LOWER_LANGUAGE:
            case MERGE_MODE:
            case MAKE_MERGED_STREAMS_DEFAULT:
            case PLAIN_TEXT_SUBTITLES:
            case SORT_BY:
            case SORT_DIRECTION:
                return object.toString();
            default:
                log.error("unexpected setting type: " + settingType + ", most likely a bug");
                throw new IllegalStateException();
        }
    }
}