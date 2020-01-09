package kirill.subtitlemerger.logic.core;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.entities.Subtitle;
import kirill.subtitlemerger.logic.core.entities.SubtitleLine;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.List;

@CommonsLog
public class Parser {
    public static Subtitles fromSubRipText(
            String subRipText,
            String subtitlesName,
            LanguageAlpha3Code language
    ) throws IncorrectFormatException {
        List<Subtitle> result = new ArrayList<>();

        Subtitle currentSubtitle = null;
        ParsingStage parsingStage = ParsingStage.HAVE_NOT_STARTED;

        for (String currentLine : subRipText.split("\\r?\\n")) {
            /*
             * This special character can be found at the beginning of the very first line so we have to remove it
             * in order to parse int correctly.
             */
            currentLine = currentLine.replace("\uFEFF", "").trim();
            if (parsingStage == ParsingStage.HAVE_NOT_STARTED) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                int subtitleNumber;
                try {
                    subtitleNumber = Integer.parseInt(currentLine);
                } catch (NumberFormatException e) {
                    throw new IncorrectFormatException();
                }

                currentSubtitle = new Subtitle();
                currentSubtitle.setNumber(subtitleNumber);
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

                currentSubtitle.getLines().add(new SubtitleLine(currentLine, subtitlesName));
                parsingStage = ParsingStage.PARSED_FIRST_LINE;
            } else {
                if (StringUtils.isBlank(currentLine)) {
                    result.add(currentSubtitle);

                    currentSubtitle = null;
                    parsingStage = ParsingStage.HAVE_NOT_STARTED;
                } else {
                    currentSubtitle.getLines().add(new SubtitleLine(currentLine, subtitlesName));
                }
            }
        }

        if (parsingStage != ParsingStage.HAVE_NOT_STARTED && parsingStage != ParsingStage.PARSED_FIRST_LINE) {
            throw new IncorrectFormatException();
        }

        if (parsingStage == ParsingStage.PARSED_FIRST_LINE) {
            result.add(currentSubtitle);
        }

        List<LanguageAlpha3Code> languages = new ArrayList<>();
        if (language != null) {
            languages.add(language);
        }

        return new Subtitles(result, languages);
    }

    private static LocalTime getFromTime(String currentLine) throws IncorrectFormatException {
        int delimiterIndex = currentLine.indexOf("-");
        if (delimiterIndex == -1) {
            throw new IncorrectFormatException();
        }

        String fromText = currentLine.substring(0, delimiterIndex).trim();
        try {
            return DateTimeFormat.forPattern("HH:mm:ss,SSS").parseLocalTime(fromText);
        } catch (IllegalArgumentException e) {
            throw new IncorrectFormatException();
        }
    }

    private static LocalTime getToTime(String currentLine) throws IncorrectFormatException {
        int delimiterIndex = currentLine.indexOf(">");
        if (delimiterIndex == -1) {
            throw new IncorrectFormatException();
        }

        if ((delimiterIndex + 1) >= currentLine.length()) {
            throw new IncorrectFormatException();
        }

        String toText = currentLine.substring(delimiterIndex + 1).trim();
        try {
            return DateTimeFormat.forPattern("HH:mm:ss,SSS").parseLocalTime(toText);
        } catch (IllegalArgumentException e) {
            throw new IncorrectFormatException();
        }
    }

    private enum ParsingStage {
        HAVE_NOT_STARTED,
        PARSED_NUMBER,
        PARSED_DURATION,
        PARSED_FIRST_LINE,
    }

    public static class IncorrectFormatException extends Exception {
    }
}
