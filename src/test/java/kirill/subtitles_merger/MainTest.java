package kirill.subtitles_merger;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

class MainTest {
    @Test
    private void testParseFromFileToSubtitles() throws IOException {
        Subtitles subtitles = Main.parseSubtitles(MainTest.class.getResourceAsStream("/ru.srt"), "ru");
        assertThat(subtitles.getElements()).hasSize(10);

        assertThat(subtitles.getElements().get(0).getLines()).hasSize(1);

        SubtitlesElement elementWithTwoLines = subtitles.getElements().stream()
                .filter(currentElement -> currentElement.getNumber() == 7)
                .findFirst().orElseThrow(IllegalStateException::new);
        assertThat(elementWithTwoLines.getLines()).hasSize(2);
    }

    @Test
    private void testMerge() throws IOException {
        Subtitles upperSubtitles = Main.parseSubtitles(MainTest.class.getResourceAsStream("/ru.srt"), "upper");
        Subtitles lowerSubtitles = Main.parseSubtitles(MainTest.class.getResourceAsStream("/rn.srt"), "lower");

        Subtitles merged = Main.mergeSubtitles(upperSubtitles, lowerSubtitles);
    }
}
