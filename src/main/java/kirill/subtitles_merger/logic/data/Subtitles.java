package kirill.subtitles_merger.logic.data;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.format.DateTimeFormat;

import java.util.List;

@AllArgsConstructor
@Getter
public class Subtitles {
    private List<Subtitle> subtitles;

    private List<LanguageAlpha3Code> languages;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < subtitles.size(); i++) {
            Subtitle subtitle = subtitles.get(i);

            result.append(subtitle.getNumber());
            result.append("\n");

            result.append(DateTimeFormat.forPattern("HH:mm:ss,SSS").print(subtitle.getFrom()));
            result.append(" --> ");
            result.append(DateTimeFormat.forPattern("HH:mm:ss,SSS").print(subtitle.getTo()));
            result.append("\n");

            for (int j = 0; j < subtitle.getLines().size(); j++) {
                SubtitleLine line = subtitle.getLines().get(j);

                result.append(line.getText());

                if (j != subtitle.getLines().size() - 1 || i != subtitles.size() - 1) {
                    result.append("\n");
                }
            }

            if (i != subtitles.size() - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }
}
