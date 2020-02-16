package kirill.subtitlemerger.gui;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GuiConstants {
    public static final boolean DEBUG = false;

    public static final int INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES = 10;

    public static final String BUTTON_ERROR_CLASS = "button-error";

    public static final String TEXT_FIELD_ERROR_CLASS = "text-field-error";

    public static final String LABEL_SUCCESS_CLASS = "label-success";

    public static final String LABEL_WARN_CLASS = "label-warn";

    public static final String LABEL_ERROR_CLASS = "label-error";

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
}