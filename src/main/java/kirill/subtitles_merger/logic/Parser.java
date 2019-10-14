package kirill.subtitles_merger.logic;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;

import java.io.*;
import java.nio.charset.StandardCharsets;

@CommonsLog
public class Parser {
    public static Subtitles parseSubtitles(String path, String subtitlesName) throws IOException {
        if (!new File(path).exists()) {
            log.error("file " + path + " doesn't exist");
            throw new IllegalArgumentException();
        }

        return parseSubtitles(new FileInputStream(path), subtitlesName);
    }

    public static Subtitles parseSubtitles(InputStream inputStream, String subtitlesName) throws IOException {
        Subtitles result = new Subtitles();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        SubtitlesElement currentElement = null;
        ParsingStage parsingStage = ParsingStage.HAVE_NOT_STARTED;

        String currentLine;
        while ((currentLine = bufferedReader.readLine()) != null) {
            /* Этот спец, символ может быть в начале самой первой строки, надо убрать а то не распарсится инт. */
            currentLine = currentLine.replace("\uFEFF", "").trim();
            if (parsingStage == ParsingStage.HAVE_NOT_STARTED) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                currentElement = new SubtitlesElement();
                currentElement.setNumber(Integer.parseInt(currentLine));
                parsingStage = ParsingStage.PARSED_NUMBER;
            } else if (parsingStage == ParsingStage.PARSED_NUMBER) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                String fromText = currentLine.substring(0, currentLine.indexOf("-")).trim();
                String toText = currentLine.substring(currentLine.indexOf(">") + 1).trim();

                currentElement.setFrom(DateTimeFormat.forPattern("HH:mm:ss,SSS").parseLocalTime(fromText));
                currentElement.setTo(DateTimeFormat.forPattern("HH:mm:ss,SSS").parseLocalTime(toText));

                parsingStage = ParsingStage.PARSED_DURATION;
            } else if (parsingStage == ParsingStage.PARSED_DURATION) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                currentElement.getLines().add(new SubtitlesElementLine(currentLine, subtitlesName));
                parsingStage = ParsingStage.PARSED_FIRST_LINE;
            } else {
                if (StringUtils.isBlank(currentLine)) {
                    result.getElements().add(currentElement);

                    currentElement = null;
                    parsingStage = ParsingStage.HAVE_NOT_STARTED;
                } else {
                    currentElement.getLines().add(new SubtitlesElementLine(currentLine, subtitlesName));
                }
            }
        }

        if (parsingStage != ParsingStage.HAVE_NOT_STARTED && parsingStage != ParsingStage.PARSED_FIRST_LINE) {
            throw new IllegalStateException();
        }

        if (parsingStage == ParsingStage.PARSED_FIRST_LINE) {
            result.getElements().add(currentElement);
        }

        return result;
    }

    private enum ParsingStage {
        HAVE_NOT_STARTED,
        PARSED_NUMBER,
        PARSED_DURATION,
        PARSED_FIRST_LINE,
    }
}
