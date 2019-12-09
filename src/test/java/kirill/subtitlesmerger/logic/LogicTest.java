package kirill.subtitlesmerger.logic;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.core.Merger;
import kirill.subtitlesmerger.logic.core.Parser;
import kirill.subtitlesmerger.logic.core.entities.Subtitles;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;

public class LogicTest {
    @Test
    public void testParseFromFileToSubtitles() throws IOException, Parser.IncorrectFormatException {
        Subtitles subtitles = Parser.parseSubtitles(
                IOUtils.toString(
                        LogicTest.class.getResourceAsStream("/MainTest/testParseFromFileToSubtitles/sub.srt"),
                        StandardCharsets.UTF_8
                ),
                "ru",
                LanguageAlpha3Code.rus
        );

        assertThat(subtitles.getSubtitles()).hasSize(10);

        assertThat(subtitles.getSubtitles().get(0).getLines()).hasSize(2);
        assertThat(subtitles.getSubtitles().get(1).getLines()).hasSize(1);
    }

    @Test
    public void testMerge() throws IOException, Parser.IncorrectFormatException {
        Subtitles upperSubtitles = Parser.parseSubtitles(
                IOUtils.toString(
                        LogicTest.class.getResourceAsStream("/MainTest/testMerged/upper.srt"),
                        StandardCharsets.UTF_8
                ),
                "upper",
                LanguageAlpha3Code.rus
        );
        Subtitles lowerSubtitles = Parser.parseSubtitles(
                IOUtils.toString(
                        LogicTest.class.getResourceAsStream("/MainTest/testMerged/lower.srt"),
                        StandardCharsets.UTF_8
                ),
                "lower",
                LanguageAlpha3Code.eng
        );

        Subtitles merged = Merger.mergeSubtitles(upperSubtitles, lowerSubtitles);
        String expected = IOUtils.toString(LogicTest.class.getResourceAsStream("/MainTest/testMerged/result.srt"));
        assertThat(merged.toString()).isEqualTo(expected);
    }
}
