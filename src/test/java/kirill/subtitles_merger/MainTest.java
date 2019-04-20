package kirill.subtitles_merger;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class MainTest {
    @Test
    public void testParseFromFileToSubtitles() throws IOException {
        Subtitles subtitles = Main.parseSubtitles(MainTest.class.getResourceAsStream("/MainTest/testParseFromFileToSubtitles/sub.srt"), "ru");
        assertThat(subtitles.getElements()).hasSize(10);

        assertThat(subtitles.getElements().get(0).getLines()).hasSize(2);
        assertThat(subtitles.getElements().get(1).getLines()).hasSize(1);
    }

    @Test
    public void testMerge() throws IOException {
        Subtitles upperSubtitles = Main.parseSubtitles(MainTest.class.getResourceAsStream("/MainTest/testMerged/upper.srt"), "upper");
        Subtitles lowerSubtitles = Main.parseSubtitles(MainTest.class.getResourceAsStream("/MainTest/testMerged/lower.srt"), "lower");

        Subtitles merged = Main.mergeSubtitles(upperSubtitles, lowerSubtitles);
        String expected = IOUtils.toString(MainTest.class.getResourceAsStream("/MainTest/testMerged/result.srt"));
        assertThat(merged.toString()).isEqualTo(expected);
    }
}
