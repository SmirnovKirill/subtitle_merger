package kirill.subtitlesmerger.logic;

import com.neovisionaries.i18n.LanguageAlpha3Code;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Constants {
    static final String PREFERENCES_ROOT_NODE = "subtitlesmerger";

    public static final List<String> ALLOWED_VIDEO_EXTENSIONS = Collections.singletonList("mkv");

    public static final List<String> ALLOWED_VIDEO_MIME_TYPES = Collections.singletonList("video/x-matroska");

    public static final boolean DEBUG = false;

    public static final int INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES = 10;

    public static final List<LanguageAlpha3Code> ALLOWED_LANGUAGE_CODES = getAllLanguageCodes();

    private static List<LanguageAlpha3Code> getAllLanguageCodes() {
        return Arrays.stream(LanguageAlpha3Code.values())
                .filter(code -> code != LanguageAlpha3Code.undefined)
                .filter(code -> code.getUsage() != LanguageAlpha3Code.Usage.TERMINOLOGY)
                /*
                 * The title for this code is way to long and doesn't fit to the combobox so I think it's simpler
                 * just to remove it, nobody will use it anyway.
                 */
                .filter(code -> code != LanguageAlpha3Code.inc)
                .sorted(Comparator.comparing(LanguageAlpha3Code::getName))
                .collect(Collectors.toList());
    }
}
