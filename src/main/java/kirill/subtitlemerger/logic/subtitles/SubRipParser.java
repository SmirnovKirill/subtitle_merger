package kirill.subtitlemerger.logic.subtitles;

import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitle;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommonsLog
public class SubRipParser {
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile(
            "^(\\d{2}:\\d{2}:\\d{2},\\d{3}) --> (\\d{2}:\\d{2}:\\d{2},\\d{3})$"
    );

    /*
     * The main idea of this method is to find lines with time ranges, they are the most stable and reliable parts of
     * a text with subtitles.
     */
    public static Subtitles from(String text) throws SubtitleFormatException {
        List<Subtitle> result = new ArrayList<>();

        /* Remove BOM if it's present. */
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }

        if (StringUtils.isBlank(text)) {
            return new Subtitles(result);
        }

        /*
         * linesBeforeTimeRange contains at least one line with the subtitle number and an arbitrary number of blank
         * lines, linesAfterTimeRange contains the text of the subtitle and the number of the following subtitle if it's
         * present.
         */
        List<String> linesBeforeTimeRange = new ArrayList<>();
        List<String> linesAfterTimeRange = new ArrayList<>();
        Range<LocalTime> timeRange = null;

        for (String line : LogicConstants.LINE_SEPARATOR_PATTERN.split(text)) {
            if (isLineWithTimeRange(line)) {
                if (timeRange != null) {
                    /* We can get here only after parsing the second line with the time range.*/
                    validateNumber(linesBeforeTimeRange);
                    List<String> subtitleLines = getTrimmedLines(
                            linesAfterTimeRange.subList(0, linesAfterTimeRange.size() - 1)
                    );
                    result.add(new Subtitle(timeRange.getMinimum(), timeRange.getMaximum(), subtitleLines));

                    linesBeforeTimeRange.clear();
                    linesBeforeTimeRange.add(linesAfterTimeRange.get(linesAfterTimeRange.size() - 1));
                    linesAfterTimeRange = new ArrayList<>();
                }

                timeRange = getTimeRange(line);
            } else {
                if (timeRange == null) {
                    /*
                     * We can get here only before parsing the first line with the time range, after that lines before
                     * will be extracted from the lines after.
                     */
                    linesBeforeTimeRange.add(line);
                } else {
                    linesAfterTimeRange.add(line);
                }
            }
        }

        if (timeRange != null) {
            validateNumber(linesBeforeTimeRange);
            List<String> subtitleLines = getTrimmedLines(linesAfterTimeRange);
            result.add(new Subtitle(timeRange.getMinimum(), timeRange.getMaximum(), subtitleLines));
        } else if (!CollectionUtils.isEmpty(linesBeforeTimeRange)) {
            throw new SubtitleFormatException();
        }

        return new Subtitles(result);
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

    private static Range<LocalTime> getTimeRange(String line) throws SubtitleFormatException {
        Matcher matcher = TIME_RANGE_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            log.error("the line doesn't match the time range pattern, most likely a bug");
            throw new IllegalStateException();
        }

        LocalTime from;
        try {
            from = LogicConstants.SUBRIP_TIME_FORMATTER.parseLocalTime(matcher.group(1));
        } catch (IllegalArgumentException e) {
            throw new SubtitleFormatException();
        }

        LocalTime to;
        try {
            to = LogicConstants.SUBRIP_TIME_FORMATTER.parseLocalTime(matcher.group(2));
        } catch (IllegalArgumentException e) {
            throw new SubtitleFormatException();
        }

        return Range.between(from, to, LocalTime::compareTo);
    }

    /**
     * Checks that the last line contains a single integer and lines before are all empty if present.
     */
    private static void validateNumber(List<String> lines) throws SubtitleFormatException {
        if (lines.size() == 0) {
            throw new SubtitleFormatException();
        }

        String lastLine = lines.get(lines.size() - 1);
        try {
            Integer.parseInt(lastLine.trim());
        } catch (NumberFormatException e) {
            throw new SubtitleFormatException();
        }

        for (int i = 0; i < lines.size() - 1; i++) {
            if (!StringUtils.isBlank(lines.get(i))) {
                throw new SubtitleFormatException();
            }
        }
    }

    /**
     * @return lines with the leading and trailing blank lines removed.
     */
    private static List<String> getTrimmedLines(List<String> lines) {
        Integer firstNotBlankIndex = null;
        for (int i = 0; i < lines.size(); i++) {
            if (!StringUtils.isBlank(lines.get(i))) {
                firstNotBlankIndex = i;
                break;
            }
        }

        Integer lastNotBlankIndex = null;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (!StringUtils.isBlank(lines.get(i))) {
                lastNotBlankIndex = i;
                break;
            }
        }

        /* Either both of them are null or none of them. */
        if (firstNotBlankIndex == null || lastNotBlankIndex == null) {
            return new ArrayList<>();
        }

        return lines.subList(firstNotBlankIndex, lastNotBlankIndex + 1);
    }
}
