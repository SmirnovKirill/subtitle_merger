package kirill.subtitlemerger.logic.subtitles.entities;

import kirill.subtitlemerger.logic.subtitles.SubRipWriter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
//todo comment
public class SubtitlesAndOutput {
    private Subtitles subtitles;

    private String text;

    public static SubtitlesAndOutput from(Subtitles subtitles, boolean plainText) {
        return new SubtitlesAndOutput(subtitles, SubRipWriter.toText(subtitles, plainText));
    }
}
