package kirill.subtitlesmerger.logic.ffmpeg.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class JsonStream {
    @JsonProperty(value = "index")
    private int index;

    @JsonProperty(value = "codec_type")
    private String codecType;

    @JsonProperty(value = "codec_name")
    private String codecName;

    /*
     * Ffmpeg uses ISO 639-2 for language codes, below is the SO answer of one of the ffmpeg's developers.
     * https://stackoverflow.com/questions/44351606/ffmpeg-set-the-language-of-an-audio-stream.
     * Later it turned out that there's more to the format, it's possible to use hyphens as well, see the documentation
     * https://www.ffmpeg.org/ffmpeg-formats.html#matroska.
     */
    @JsonProperty(value = "tags")
    private Map<String, String> tags;
}
