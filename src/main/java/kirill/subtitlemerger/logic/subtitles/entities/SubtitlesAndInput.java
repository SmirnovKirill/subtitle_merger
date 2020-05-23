package kirill.subtitlemerger.logic.subtitles.entities;

import kirill.subtitlemerger.logic.subtitles.SubRipParser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;

/**
 * A helper class that stores subtitles and their raw data. It is helpful because if the user provides a file with
 * subtitles it's impossible to guess the correct encoding so we should store the original raw data and give the ability
 * to change the encoding.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
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
