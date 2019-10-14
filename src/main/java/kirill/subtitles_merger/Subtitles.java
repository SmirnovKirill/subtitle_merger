package kirill.subtitles_merger;

import lombok.Getter;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.List;

@Getter
class Subtitles {
    private List<SubtitlesElement> elements = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < elements.size(); i++) {
            SubtitlesElement subtitlesElement = elements.get(i);

            result.append(subtitlesElement.getNumber());
            result.append("\n");

            result.append(DateTimeFormat.forPattern("HH:mm:ss,SSS").print(subtitlesElement.getFrom()));
            result.append(" --> ");
            result.append(DateTimeFormat.forPattern("HH:mm:ss,SSS").print(subtitlesElement.getTo()));
            result.append("\n");

            for (int j = 0; j < subtitlesElement.getLines().size(); j++) {
                SubtitlesElementLine line = subtitlesElement.getLines().get(j);

                result.append(line.getText());

                if (j != subtitlesElement.getLines().size() - 1 || i != elements.size() - 1) {
                    result.append("\n");
                }
            }

            if (i != elements.size() - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }
}
