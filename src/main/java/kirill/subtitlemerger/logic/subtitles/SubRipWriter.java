package kirill.subtitlemerger.logic.subtitles;

import kirill.subtitlemerger.logic.subtitles.entities.Subtitle;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubRipWriter {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss,SSS");

    public static String toText(Subtitles subtitles, boolean plainText) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < subtitles.getSubtitles().size(); i++) {
            Subtitle subtitle = subtitles.getSubtitles().get(i);

            result.append(i + 1);
            result.append(System.lineSeparator());

            result.append(TIME_FORMATTER.print(subtitle.getFrom()));
            result.append(" --> ");
            result.append(TIME_FORMATTER.print(subtitle.getTo()));
            result.append(System.lineSeparator());

            List<String> processedLines = getProcessedSubtitleLines(subtitle.getLines(), plainText);
            for (int j = 0; j < processedLines.size(); j++) {
                result.append(processedLines.get(j));

                if (j != processedLines.size() - 1 || i != subtitles.getSubtitles().size() - 1) {
                    result.append(System.lineSeparator());
                }
            }

            if (i != subtitles.getSubtitles().size() - 1) {
                result.append(System.lineSeparator());
            }
        }

        return result.toString();
    }

    /**
     * Returns processed lines meaning:
     * 1) all tags are removed if the plainText flag is passed and lines that consist of tags only are not included;
     * 2) if there are no lines to return then an array with one empty line is returned to follow the format.
     *
     * We have to process all lines for a subtitle before writing them because some lines may contain only tags and thus
     * should be removed. And it's important to get the final version of the list in advance so that line breaks are
     * added correctly later.
     */
    private static List<String> getProcessedSubtitleLines(List<String> unprocessedLines, boolean plainText) {
        List<String> result;

        if (!plainText) {
            result = unprocessedLines;
        } else {
            result = new ArrayList<>();
            for (String unprocessedLine : unprocessedLines) {
                /*
                 * If a line was initially blank then it should be included, but if it was originally not blank but
                 * became blank after removing tags it should not.
                 */
                if (StringUtils.isBlank(unprocessedLine)) {
                    result.add(unprocessedLine);
                    continue;
                }

                String processedLine = getPlainText(unprocessedLine);
                if (!StringUtils.isBlank(processedLine)) {
                    result.add(processedLine);
                }
            }
        }

        if (CollectionUtils.isEmpty(result)) {
            result = Collections.singletonList("");
        }

        return result;
    }

    /**
     * This method removes HTML and SubStation Alpha tags using regular expressions. I realize that it's not right to
     * work with HTML using regular expressions and I could have used jsoup to clear tags out but here are the arguments
     * not to do it:
     * 1) A subtitle text is not an html text, it's a regular text with html tags as far as I know. So for example the
     * string "<test" is correct in terms of subtitles but incorrect in terms of HTML because brackets that are not tags
     * have to be encoded. So if I use jsoup it will simply remove the text "<test" completely instead of leaving it as
     * is.
     * 2) All not removed basic html entities will be html-encoded so I have to decode them back.
     * 3) Even if I use jsoup there are no well-known libraries for removing SubStation Alpha tags.
     * 4) Regular expressions are faster than jsoup.
     * 5) If it's possible not to use another library it's better to do so to reduce the jar's size.
     * So I think in general for our purposes regular expressions will suffice.
     */
    private static String getPlainText(String text) {
        String result = text;
        if (text.contains("<") || text.contains(">")) {
            result = result.replaceAll("<.+?>", "");
        }
        if (text.contains("{") || text.contains("}")) {
            result = result.replaceAll("\\{.+?}", "");
        }

        return result;
    }
}
