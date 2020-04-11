package kirill.subtitlemerger.logic;

import kirill.subtitlemerger.logic.core.SubRipParser;
import kirill.subtitlemerger.logic.core.SubRipWriter;
import kirill.subtitlemerger.logic.core.SubtitleMerger;
import kirill.subtitlemerger.logic.core.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;

public class LogicTest {
    @Test
    public void testParseFromFileToSubtitles() throws IOException, SubtitleFormatException {
        Subtitles subtitles = SubRipParser.from(
                IOUtils.toString(
                        LogicTest.class.getResourceAsStream("/MainTest/testParseFromFileToSubtitles/sub.srt"),
                        StandardCharsets.UTF_8
                )
        );

        assertThat(subtitles.getSubtitles()).hasSize(10);

        assertThat(subtitles.getSubtitles().get(0).getLines()).hasSize(2);
        assertThat(subtitles.getSubtitles().get(1).getLines()).hasSize(1);
    }

    @Test
    public void testMerge() throws IOException, SubtitleFormatException, InterruptedException {
        Subtitles upperSubtitles = SubRipParser.from(
                IOUtils.toString(
                        LogicTest.class.getResourceAsStream("/MainTest/testMerged/upper.srt"),
                        StandardCharsets.UTF_8
                )
        );
        Subtitles lowerSubtitles = SubRipParser.from(
                IOUtils.toString(
                        LogicTest.class.getResourceAsStream("/MainTest/testMerged/lower.srt"),
                        StandardCharsets.UTF_8
                )
        );

        Subtitles merged = SubtitleMerger.mergeSubtitles(upperSubtitles, lowerSubtitles);
        String expected = IOUtils.toString(
                LogicTest.class.getResourceAsStream("/MainTest/testMerged/result.srt"),
                StandardCharsets.UTF_8
        );

        assertThat(SubRipWriter.toText(merged)).isEqualTo(expected);
    }
}
