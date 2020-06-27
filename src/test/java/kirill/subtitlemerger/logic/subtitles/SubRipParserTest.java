package kirill.subtitlemerger.logic.subtitles;

import kirill.subtitlemerger.logic.subtitles.entities.Subtitle;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;

public class SubRipParserTest {
    @Test
    public void testBasic() throws IOException, SubtitleFormatException {
        Subtitles subtitles = SubRipParser.from(
                IOUtils.toString(
                        getClass().getResourceAsStream("/logic/subtitles/sub_rip_parser/basic.srt"),
                        StandardCharsets.UTF_8
                )
        );

        assertThat(subtitles.getSubtitles()).hasSize(10);

        assertThat(subtitles.getSubtitles().get(0).getLines()).hasSize(2);
        assertThat(subtitles.getSubtitles().get(1).getLines()).hasSize(1);
    }

    @Test
    public void testEmpty() throws SubtitleFormatException {
        Subtitles subtitles = SubRipParser.from("");

        assertThat(subtitles.getSubtitles()).hasSize(0);
    }

    /*
     * Subtitle #3 has an empty line, it should be parsed correctly.
     */
    @Test
    public void testEmptyLines() throws IOException, SubtitleFormatException {
        Subtitles subtitles = SubRipParser.from(
                IOUtils.toString(
                        getClass().getResourceAsStream("/logic/subtitles/sub_rip_parser/empty_lines.srt"),
                        StandardCharsets.UTF_8
                )
        );

        assertThat(subtitles.getSubtitles()).hasSize(4);

        assertThat(subtitles.getSubtitles().get(2).getLines()).hasSize(4);
    }

    /*
     * Border case - just one subtitle.
     */
    @Test
    public void testOneSubtitle() throws IOException, SubtitleFormatException {
        Subtitles subtitles = SubRipParser.from(
                IOUtils.toString(
                        getClass().getResourceAsStream("/logic/subtitles/sub_rip_parser/one_subtitle.srt"),
                        StandardCharsets.UTF_8
                )
        );

        assertThat(subtitles.getSubtitles()).hasSize(1);

        Subtitle subtitle = subtitles.getSubtitles().get(0);
        assertThat(subtitle.getLines()).hasSize(2);
        assertThat(subtitle.getLines().get(0)).isEqualTo("Я считаю,");
        assertThat(subtitle.getLines().get(1)).isEqualTo("что существует женщина-киллер,");
    }

    /*
     * Another border case - two subtitles.
     */
    @Test
    public void testTwoSubtitles() throws IOException, SubtitleFormatException {
        Subtitles subtitles = SubRipParser.from(
                IOUtils.toString(
                        getClass().getResourceAsStream("/logic/subtitles/sub_rip_parser/two_subtitles.srt"),
                        StandardCharsets.UTF_8
                )
        );

        assertThat(subtitles.getSubtitles()).hasSize(2);

        Subtitle subtitle = subtitles.getSubtitles().get(0);
        assertThat(subtitle.getLines()).hasSize(2);
        assertThat(subtitle.getLines().get(0)).isEqualTo("Я считаю,");
        assertThat(subtitle.getLines().get(1)).isEqualTo("что существует женщина-киллер,");

        subtitle = subtitles.getSubtitles().get(1);
        assertThat(subtitle.getLines()).hasSize(1);
        assertThat(subtitle.getLines().get(0)).isEqualTo("которая работает в разных странах");
    }

    /*
     * The first and last several lines in the file are empty, they should be ignored.
     */
    @Test
    public void testEmptyTrailingLines() throws IOException, SubtitleFormatException {
        Subtitles subtitles = SubRipParser.from(
                IOUtils.toString(
                        getClass().getResourceAsStream(
                                "/logic/subtitles/sub_rip_parser/empty_trailing_lines.srt"
                        ),
                        StandardCharsets.UTF_8
                )
        );

        assertThat(subtitles.getSubtitles()).hasSize(3);

        Subtitle subtitle = subtitles.getSubtitles().get(0);
        assertThat(subtitle.getLines()).hasSize(2);
        assertThat(subtitle.getLines().get(0)).isEqualTo("Я считаю,");
        assertThat(subtitle.getLines().get(1)).isEqualTo("что существует женщина-киллер,");

        subtitle = subtitles.getSubtitles().get(2);
        assertThat(subtitle.getLines()).hasSize(1);
        assertThat(subtitle.getLines().get(0)).isEqualTo("и устранила многих влиятельных людей.");
    }

    /*
     * The first subtitle has no lines and the second has three empty.
     */
    @Test
    public void testWithEmptySubtitle() throws IOException, SubtitleFormatException {
        Subtitles subtitles = SubRipParser.from(
                IOUtils.toString(
                        getClass().getResourceAsStream("/logic/subtitles/sub_rip_parser/with_empty_subtitle.srt"),
                        StandardCharsets.UTF_8
                )
        );

        assertThat(subtitles.getSubtitles()).hasSize(3);

        Subtitle subtitle = subtitles.getSubtitles().get(0);
        assertThat(subtitle.getLines()).isEmpty();

        subtitle = subtitles.getSubtitles().get(1);
        assertThat(subtitle.getLines()).hasSize(3);
        assertThat(subtitle.getLines().get(0)).isEqualTo("");
        assertThat(subtitle.getLines().get(1)).isEqualTo("");
        assertThat(subtitle.getLines().get(2)).isEqualTo("");

        subtitle = subtitles.getSubtitles().get(2);
        assertThat(subtitle.getLines()).hasSize(2);
        assertThat(subtitle.getLines().get(0)).isEqualTo("Я считаю,");
        assertThat(subtitle.getLines().get(1)).isEqualTo("что существует женщина-киллер,");
    }

    /*
     * There are two subtitles but the first one has lines that look like the beginning of the second.
     */
    @Test
    public void testTricky() throws IOException, SubtitleFormatException {
        Subtitles subtitles = SubRipParser.from(
                IOUtils.toString(
                        getClass().getResourceAsStream("/logic/subtitles/sub_rip_parser/tricky.srt"),
                        StandardCharsets.UTF_8
                )
        );

        assertThat(subtitles.getSubtitles()).hasSize(2);

        Subtitle subtitle = subtitles.getSubtitles().get(0);
        assertThat(subtitle.getLines()).hasSize(4);
        assertThat(subtitle.getLines().get(0)).startsWith("<font face=\"Franklin Gothic Medium\" size=\"63\" color=\"");
        assertThat(subtitle.getLines().get(1)).isEqualTo("");
        assertThat(subtitle.getLines().get(2)).isEqualTo("2");
        assertThat(subtitle.getLines().get(3)).isEqualTo("subtitles still go");

        subtitle = subtitles.getSubtitles().get(1);
        assertThat(subtitle.getLines()).hasSize(1);
        assertThat(subtitle.getLines().get(0)).isEqualTo("the actual second line");
    }

    @Test(expected = SubtitleFormatException.class)
    public void testIncorrect() throws SubtitleFormatException {
        SubRipParser.from("just an incorrect string");
    }
}
