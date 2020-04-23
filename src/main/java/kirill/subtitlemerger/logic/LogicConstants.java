package kirill.subtitlemerger.logic;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogicConstants {
    public static final List<String> ALLOWED_VIDEO_EXTENSIONS = Collections.singletonList("mkv");

    public static final List<LanguageAlpha3Code> ALLOWED_LANGUAGES = getAllowedLanguages();

    public static final DateTimeFormatter SUBRIP_TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss,SSS");

    public static final Pattern LINE_SEPARATOR_PATTERN = Pattern.compile("\\r?\\n");

    public static final int INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES = 10;

    public static final List<Charset> SUPPORTED_ENCODINGS = Arrays.asList(
            StandardCharsets.UTF_8,
            Charset.forName("IBM00858"),
            Charset.forName("IBM437"),
            Charset.forName("IBM775"),
            Charset.forName("IBM850"),
            Charset.forName("IBM852"),
            Charset.forName("IBM855"),
            Charset.forName("IBM857"),
            Charset.forName("IBM862"),
            Charset.forName("IBM866"),
            StandardCharsets.ISO_8859_1,
            Charset.forName("ISO-8859-2"),
            Charset.forName("ISO-8859-4"),
            Charset.forName("ISO-8859-5"),
            Charset.forName("ISO-8859-7"),
            Charset.forName("ISO-8859-9"),
            Charset.forName("ISO-8859-13"),
            Charset.forName("ISO-8859-15"),
            Charset.forName("KOI8-R"),
            Charset.forName("KOI8-U"),
            StandardCharsets.US_ASCII,
            StandardCharsets.UTF_16,
            StandardCharsets.UTF_16BE,
            StandardCharsets.UTF_16LE,
            Charset.forName("UTF-32"),
            Charset.forName("UTF-32BE"),
            Charset.forName("UTF-32LE"),
            Charset.forName("windows-1250"),
            Charset.forName("windows-1251"),
            Charset.forName("windows-1252"),
            Charset.forName("windows-1253"),
            Charset.forName("windows-1254"),
            Charset.forName("windows-1257")
    );

    public static final List<String> ALLOWED_VIDEO_FORMATS = Collections.singletonList("matroska,webm");

    public static final String SUB_RIP_FORMAT = "subrip";

    public static final String SUB_STATION_ALPHA_FORMAT = "ass";

    public static final List<String> ALLOWED_SUBTITLE_FORMATS = Arrays.asList(SUB_RIP_FORMAT, SUB_STATION_ALPHA_FORMAT);

    private static List<LanguageAlpha3Code> getAllowedLanguages() {
        return Arrays.stream(LanguageAlpha3Code.values())
                .filter(code -> code != LanguageAlpha3Code.undefined)
                /*
                 * https://www.ffmpeg.org/ffmpeg-formats.html#matroska says that bibliographic codes are used, so we
                 * will exclude terminological.
                 */
                .filter(code -> code.getUsage() != LanguageAlpha3Code.Usage.TERMINOLOGY)
                .collect(Collectors.toList());
    }
}
