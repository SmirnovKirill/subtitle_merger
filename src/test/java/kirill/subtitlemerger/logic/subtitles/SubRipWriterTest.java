package kirill.subtitlemerger.logic.subtitles;

import kirill.subtitlemerger.logic.subtitles.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;

public class SubRipWriterTest {
    @Test
    public void testTags() throws IOException, SubtitleFormatException {
        String inputText = IOUtils.toString(
                getClass().getResourceAsStream("/logic/subtitles/sub_rip_writer/tags.srt"),
                StandardCharsets.UTF_8
        );
        Subtitles subtitles = SubRipParser.from(inputText);

        String expectedPlain = IOUtils.toString(
                getClass().getResourceAsStream("/logic/subtitles/sub_rip_writer/tags_plain.srt"),
                StandardCharsets.UTF_8
        );

        assertThat(SubRipWriter.toText(subtitles, true)).isEqualTo(expectedPlain);
        assertThat(SubRipWriter.toText(subtitles, false)).isEqualTo(inputText);
    }

    @Test
    public void testEmptyLines() throws IOException, SubtitleFormatException {
        Subtitles subtitles = SubRipParser.from(
                IOUtils.toString(
                        getClass().getResourceAsStream("/logic/subtitles/sub_rip_writer/empty_lines.srt"),
                        StandardCharsets.UTF_8
                )
        );

        String expectedNotPlain = IOUtils.toString(
                getClass().getResourceAsStream(
                        "/logic/subtitles/sub_rip_writer/empty_lines_output_not_plain.srt"
                ),
                StandardCharsets.UTF_8
        );
        assertThat(SubRipWriter.toText(subtitles, false)).isEqualTo(expectedNotPlain);

        String expectedPlain = IOUtils.toString(
                getClass().getResourceAsStream("/logic/subtitles/sub_rip_writer/empty_lines_output_plain.srt"),
                StandardCharsets.UTF_8
        );
        assertThat(SubRipWriter.toText(subtitles, true)).isEqualTo(expectedPlain);
    }
}
