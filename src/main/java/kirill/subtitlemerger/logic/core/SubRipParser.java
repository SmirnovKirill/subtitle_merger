package kirill.subtitlemerger.logic.core;

import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.core.entities.Subtitle;
import kirill.subtitlemerger.logic.core.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

@CommonsLog
public class SubRipParser {
    public static Subtitles from(String text) throws SubtitleFormatException {
        List<Subtitle> result = new ArrayList<>();

        /* Remove BOM if it's present. */
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }

        CurrentSubtitle currentSubtitle = null;
        ParsingStage parsingStage = ParsingStage.HAVE_NOT_STARTED;

        for (String currentLine : LogicConstants.LINE_SEPARATOR_PATTERN.split(text)) {
            currentLine = currentLine.trim();

            if (parsingStage == ParsingStage.HAVE_NOT_STARTED) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                /* We can parse the number here but we don't actually need it. */
                currentSubtitle = new CurrentSubtitle();
                parsingStage = ParsingStage.PARSED_NUMBER;
            } else if (parsingStage == ParsingStage.PARSED_NUMBER) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                currentSubtitle.setFrom(getFromTime(currentLine));
                currentSubtitle.setTo(getToTime(currentLine));

                parsingStage = ParsingStage.PARSED_DURATION;
            } else if (parsingStage == ParsingStage.PARSED_DURATION) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                currentSubtitle.setLines(new ArrayList<>());
                currentSubtitle.getLines().add(currentLine);
                parsingStage = ParsingStage.LINES_STARTED;
            } else {
                if (StringUtils.isBlank(currentLine)) {
                    result.add(subtitleFrom(currentSubtitle));

                    currentSubtitle = null;
                    parsingStage = ParsingStage.HAVE_NOT_STARTED;
                } else {
                    currentSubtitle.getLines().add(currentLine);
                }
            }
        }

        if (parsingStage != ParsingStage.HAVE_NOT_STARTED && parsingStage != ParsingStage.LINES_STARTED) {
            throw new SubtitleFormatException();
        }

        if (parsingStage == ParsingStage.LINES_STARTED) {
            result.add(subtitleFrom(currentSubtitle));
        }

        return new Subtitles(result);
    }

    private static LocalTime getFromTime(String line) throws SubtitleFormatException {
        int delimiterIndex = line.indexOf("-");
        if (delimiterIndex == -1) {
            throw new SubtitleFormatException();
        }

        String fromText = line.substring(0, delimiterIndex).trim();
        try {
            return LogicConstants.SUBRIP_TIME_FORMATTER.parseLocalTime(fromText);
        } catch (IllegalArgumentException e) {
            throw new SubtitleFormatException();
        }
    }

    private static LocalTime getToTime(String line) throws SubtitleFormatException {
        int delimiterIndex = line.indexOf(">");
        if (delimiterIndex == -1) {
            throw new SubtitleFormatException();
        }

        if ((delimiterIndex + 1) >= line.length()) {
            throw new SubtitleFormatException();
        }

        String toText = line.substring(delimiterIndex + 1).trim();
        try {
            return LogicConstants.SUBRIP_TIME_FORMATTER.parseLocalTime(toText);
        } catch (IllegalArgumentException e) {
            throw new SubtitleFormatException();
        }
    }

    private static Subtitle subtitleFrom(CurrentSubtitle currentSubtitle) {
        return new Subtitle(currentSubtitle.getFrom(), currentSubtitle.getTo(), currentSubtitle.getLines());
    }

    private enum ParsingStage {
        HAVE_NOT_STARTED,
        PARSED_NUMBER,
        PARSED_DURATION,
        LINES_STARTED,
    }

    @Getter
    @Setter
    private static class CurrentSubtitle {
        private LocalTime from;

        private LocalTime to;

        private List<String> lines;
    }
}
