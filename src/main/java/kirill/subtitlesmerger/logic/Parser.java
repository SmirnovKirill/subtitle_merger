package kirill.subtitlesmerger.logic;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.data.Subtitles;
import kirill.subtitlesmerger.logic.data.Subtitle;
import kirill.subtitlesmerger.logic.data.SubtitleLine;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.List;

@CommonsLog
public class Parser {
    public static Subtitles parseSubtitles(
            String subtitlesUnprocessed,
            String subtitlesName,
            LanguageAlpha3Code language
    ) {
        List<Subtitle> result = new ArrayList<>();

        Subtitle currentSubtitle = null;
        ParsingStage parsingStage = ParsingStage.HAVE_NOT_STARTED;

        for (String currentLine : subtitlesUnprocessed.split("\\r?\\n")) {
            /*
             * This special character can be found at the beginning of the very first line so we have to remove it
             * in order to parse int correctly.
             */
            currentLine = currentLine.replace("\uFEFF", "").trim();
            if (parsingStage == ParsingStage.HAVE_NOT_STARTED) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                currentSubtitle = new Subtitle();
                currentSubtitle.setNumber(Integer.parseInt(currentLine));
                parsingStage = ParsingStage.PARSED_NUMBER;
            } else if (parsingStage == ParsingStage.PARSED_NUMBER) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                String fromText = currentLine.substring(0, currentLine.indexOf("-")).trim();
                String toText = currentLine.substring(currentLine.indexOf(">") + 1).trim();

                currentSubtitle.setFrom(DateTimeFormat.forPattern("HH:mm:ss,SSS").parseLocalTime(fromText));
                currentSubtitle.setTo(DateTimeFormat.forPattern("HH:mm:ss,SSS").parseLocalTime(toText));

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
            throw new IllegalStateException();
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

    private enum ParsingStage {
        HAVE_NOT_STARTED,
        PARSED_NUMBER,
        PARSED_DURATION,
        PARSED_FIRST_LINE,
    }
}
