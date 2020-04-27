package kirill.subtitlemerger.logic.subtitles.entities;

import kirill.subtitlemerger.logic.subtitles.SubRipParser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
//todo comment
public class SubtitlesAndInput {
    private byte[] rawData;

    private Charset encoding;

    private Subtitles subtitles;

    private boolean correctFormat;

    public static SubtitlesAndInput from(byte[] rawData, Charset encoding) {
        Subtitles subtitles;
        boolean correctFormat;
        try {
            subtitles = SubRipParser.from(new String(rawData, encoding));
            correctFormat = true;
        } catch (SubtitleFormatException e) {
            subtitles = null;
            correctFormat = false;
        }

        return new SubtitlesAndInput(rawData, encoding, subtitles, correctFormat);
    }

    public int getSize() {
        return rawData.length;
    }

    public SubtitlesAndInput changeEncoding(Charset newEncoding) {
        return from(rawData, newEncoding);
    }
}
