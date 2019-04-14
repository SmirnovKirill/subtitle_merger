package kirill.subtitles_merger;

import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class MainTest {
    @Test
    public void testParseFromFileToSubtitles() throws IOException {
        Subtitles subtitles = Main.parseSubtitles(MainTest.class.getResourceAsStream("/ru.srt"), "ru");
        assertThat(subtitles.getElements()).hasSize(10);

        assertThat(subtitles.getElements().get(0).getLines()).hasSize(2);
        assertThat(subtitles.getElements().get(1).getLines()).hasSize(1);
    }

    @Test
    public void testMerge() throws IOException {
        Subtitles upperSubtitles = Main.parseSubtitles(MainTest.class.getResourceAsStream("/ru.srt"), "upper");
        Subtitles lowerSubtitles = Main.parseSubtitles(MainTest.class.getResourceAsStream("/en.srt"), "lower");

        Subtitles merged = Main.mergeSubtitles(upperSubtitles, lowerSubtitles);
        StringBuilder result = new StringBuilder();

        for (SubtitlesElement subtitlesElement : merged.getElements()) {
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

        System.out.println(result.toString());
    }
}
