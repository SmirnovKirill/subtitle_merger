package kirill.subtitles_merger;

import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.List;

public class Subtitles {
    private List<SubtitlesElement> elements = new ArrayList<>();

    public List<SubtitlesElement> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (SubtitlesElement subtitlesElement : elements) {
            result.append(subtitlesElement.getNumber());
            result.append("\n");

            result.append(DateTimeFormat.forPattern("HH:mm:ss,SSS").print(subtitlesElement.getFrom()));
            result.append(" --> ");
            result.append(DateTimeFormat.forPattern("HH:mm:ss,SSS").print(subtitlesElement.getTo()));
            result.append("\n");

            for (SubtitlesElementLine line : subtitlesElement.getLines()) {
                result.append(line.getText());
                result.append("\n");
            }

            result.append("\n");
        }

        return result.toString();
    }
}
