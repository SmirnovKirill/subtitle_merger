package kirill.subtitlemerger.logic.subtitles.entities;

import kirill.subtitlemerger.logic.subtitles.SubRipWriter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A helper class that stores both subtitles and their textual representation. It is helpful because it usually makes
 * sense to generate a textual representation right after generating merged subtitles so the methods that do this should
 * be able to return a wrapper object containing both these values.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SubtitlesAndOutput {
    private Subtitles subtitles;

    private String text;

    public static SubtitlesAndOutput from(Subtitles subtitles, boolean plainText) {
        return new SubtitlesAndOutput(subtitles, SubRipWriter.toText(subtitles, plainText));
    }
}
