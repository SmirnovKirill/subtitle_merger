package kirill.subtitlemerger.logic;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogicConstants {
    public static final List<String> ALLOWED_VIDEO_EXTENSIONS = Collections.singletonList("mkv");

    public static final List<String> ALLOWED_VIDEO_MIME_TYPES = Collections.singletonList("video/x-matroska");

    public static final List<LanguageAlpha3Code> ALLOWED_LANGUAGE_CODES = getAllLanguageCodes();

    public static final DateTimeFormatter SUBRIP_TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss,SSS");

    public static final Pattern LINE_SEPARATOR_PATTERN = Pattern.compile("\\r?\\n");

    private static List<LanguageAlpha3Code> getAllLanguageCodes() {
        return Arrays.stream(LanguageAlpha3Code.values())
                .filter(code -> code != LanguageAlpha3Code.undefined)
                .filter(code -> code.getUsage() != LanguageAlpha3Code.Usage.TERMINOLOGY)
                /*
                 * The title for this code is way too long and doesn't fit to the combobox so I think it's easier
                 * just to remove it, nobody will use it anyway.
                 */
                .filter(code -> code != LanguageAlpha3Code.inc)
                .sorted(Comparator.comparing(LanguageAlpha3Code::getName))
                .collect(Collectors.toList());
    }
}
