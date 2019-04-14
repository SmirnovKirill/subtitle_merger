package kirill.subtitles_merger;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class MainTest {
    @Test
    private void testParseFromFileToSubtitles() throws IOException {
        Subtitles subtitles = Main.parseSubtitles(MainTest.class.getResourceAsStream("/ru.srt"));
        assertThat(subtitles.getElements()).hasSize(10);

        assertThat(subtitles.getElements().get(0).getLines()).hasSize(1);

        SubtitlesElement elementWithTwoLines = subtitles.getElements().stream()
                .filter(currentElement -> currentElement.getNumber() == 7)
                .findFirst().orElseThrow(IllegalStateException::new);
        assertThat(elementWithTwoLines.getLines()).hasSize(2);
    }
}
