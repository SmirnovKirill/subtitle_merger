package kirill.subtitlemerger.logic.core;

import kirill.subtitlemerger.logic.core.entities.Subtitle;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Writer {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss,SSS");

    public static String toSubRipText(Subtitles subtitles) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < subtitles.getSubtitles().size(); i++) {
            Subtitle subtitle = subtitles.getSubtitles().get(i);

            result.append(subtitle.getNumber());
            result.append(System.lineSeparator());

            result.append(TIME_FORMATTER.print(subtitle.getFrom()));
            result.append(" --> ");
            result.append(TIME_FORMATTER.print(subtitle.getTo()));
            result.append(System.lineSeparator());

            for (int j = 0; j < subtitle.getLines().size(); j++) {
                result.append(subtitle.getLines().get(j));

                if (j != subtitle.getLines().size() - 1 || i != subtitles.getSubtitles().size() - 1) {
                    result.append(System.lineSeparator());
                }
            }

            if (i != subtitles.getSubtitles().size() - 1) {
                result.append(System.lineSeparator());
            }
        }

        return result.toString();
    }
}
