package kirill.subtitlemerger.logic;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.SubtitleMerger;
import kirill.subtitlemerger.logic.core.SubtitleParser;
import kirill.subtitlemerger.logic.core.SubtitleWriter;
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
        Subtitles subtitles = SubtitleParser.fromSubRipText(
                IOUtils.toString(
                        LogicTest.class.getResourceAsStream("/MainTest/testParseFromFileToSubtitles/sub.srt"),
                        StandardCharsets.UTF_8
                ),
                LanguageAlpha3Code.rus
        );

        assertThat(subtitles.getSubtitles()).hasSize(10);

        assertThat(subtitles.getSubtitles().get(0).getLines()).hasSize(2);
        assertThat(subtitles.getSubtitles().get(1).getLines()).hasSize(1);
    }

    @Test
    public void testMerge() throws IOException, SubtitleFormatException, InterruptedException {
        Subtitles upperSubtitles = SubtitleParser.fromSubRipText(
                IOUtils.toString(
                        LogicTest.class.getResourceAsStream("/MainTest/testMerged/upper.srt"),
                        StandardCharsets.UTF_8
                ),
                LanguageAlpha3Code.rus
        );
        Subtitles lowerSubtitles = SubtitleParser.fromSubRipText(
                IOUtils.toString(
                        LogicTest.class.getResourceAsStream("/MainTest/testMerged/lower.srt"),
                        StandardCharsets.UTF_8
                ),
                LanguageAlpha3Code.eng
        );

        Subtitles merged = SubtitleMerger.mergeSubtitles(upperSubtitles, lowerSubtitles);
        String expected = IOUtils.toString(
                LogicTest.class.getResourceAsStream("/MainTest/testMerged/result.srt"),
                StandardCharsets.UTF_8
        );

        assertThat(SubtitleWriter.toSubRipText(merged)).isEqualTo(expected);
    }
}
