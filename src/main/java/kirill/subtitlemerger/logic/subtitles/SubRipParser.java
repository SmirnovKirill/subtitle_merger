package kirill.subtitlemerger.logic.subtitles;

import kirill.subtitlemerger.logic.subtitles.entities.Subtitle;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

@CommonsLog
public class SubRipParser {
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile(
            "^(\\d{2}:\\d{2}:\\d{2},\\d{3}) --> (\\d{2}:\\d{2}:\\d{2},\\d{3})$"
    );

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss,SSS");

    /*
     * The main idea of this method is to find lines with time ranges, they are the most stable and reliable parts of
     * the text.
     */
    public static Subtitles from(String text) throws SubtitleFormatException {
        List<Subtitle> result = new ArrayList<>();

        /* Remove BOM if it's present. */
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }

        text = text.trim();
        if (StringUtils.isBlank(text)) {
            return new Subtitles(result);
        }

        List<String> lines = text.lines().collect(toList());
        /* We need at least two lines - one should contain a number and the other a time range. */
        if (lines.size() < 2) {
            throw new SubtitleFormatException();
        }

        assertSubtitleNumber(lines.get(0));
        Range<LocalTime> timeRange = getTimeRange(lines.get(1));
        List<String> subtitleLines = new ArrayList<>();
        for (int i = 2; i < lines.size(); i++) {
            if (isLineWithTimeRange(lines.get(i))) {
                assertBlank(lines.get(i - 2));
                assertSubtitleNumber(lines.get(i - 1));

                /* The last two lines are a blank line and a line with a number. */
                subtitleLines = subtitleLines.subList(0, subtitleLines.size() - 2);
                result.add(new Subtitle(timeRange.getMinimum(), timeRange.getMaximum(), subtitleLines));

                timeRange = getTimeRange(lines.get(i));
                subtitleLines = new ArrayList<>();
            } else {
                subtitleLines.add(lines.get(i));
            }
        }

        result.add(new Subtitle(timeRange.getMinimum(), timeRange.getMaximum(), subtitleLines));

        return new Subtitles(result);
    }

    private static void assertSubtitleNumber(String line) throws SubtitleFormatException {
        try {
            if (Integer.parseInt(line.trim()) < 0) {
                throw new SubtitleFormatException();
            }
        } catch (NumberFormatException e) {
            throw new SubtitleFormatException();
        }
    }

    private static Range<LocalTime> getTimeRange(String line) throws SubtitleFormatException {
        Matcher matcher = TIME_RANGE_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            throw new SubtitleFormatException();
        }

        LocalTime from;
        try {
            from = TIME_FORMATTER.parseLocalTime(matcher.group(1));
        } catch (IllegalArgumentException e) {
            throw new SubtitleFormatException();
        }

        LocalTime to;
        try {
            to = TIME_FORMATTER.parseLocalTime(matcher.group(2));
        } catch (IllegalArgumentException e) {
            throw new SubtitleFormatException();
        }

        return Range.between(from, to, LocalTime::compareTo);
    }

    private static boolean isLineWithTimeRange(String line) {
        /*
         * This simple check is here for the sake of performance, it's pretty precise and at the same time much faster
         * comparing to regular expressions.
         */
        if (!line.contains("-->")) {
            return false;
        }

        return TIME_RANGE_PATTERN.matcher(line.trim()).matches();
    }

    private static void assertBlank(String line) throws SubtitleFormatException {
        if (!StringUtils.isBlank(line)) {
            throw new SubtitleFormatException();
        }
    }
}
