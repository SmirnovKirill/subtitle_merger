package kirill.subtitlemerger.logic.core;

import kirill.subtitlemerger.logic.core.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;

public class SubtitleMergerTest {
    @Test
    public void testBasic() throws IOException, SubtitleFormatException, InterruptedException {
        Subtitles upperSubtitles = SubRipParser.from(
                IOUtils.toString(
                        getClass().getResourceAsStream("/logic/core/subtitle_merger/upper.srt"),
                        StandardCharsets.UTF_8
                )
        );
        Subtitles lowerSubtitles = SubRipParser.from(
                IOUtils.toString(
                        getClass().getResourceAsStream("/logic/core/subtitle_merger/lower.srt"),
                        StandardCharsets.UTF_8
                )
        );

        Subtitles merged = SubtitleMerger.mergeSubtitles(upperSubtitles, lowerSubtitles);
        String expected = IOUtils.toString(
                getClass().getResourceAsStream("/logic/core/subtitle_merger/result.srt"),
                StandardCharsets.UTF_8
        );

        assertThat(SubRipWriter.toText(merged, true)).isEqualTo(expected);
    }
}
