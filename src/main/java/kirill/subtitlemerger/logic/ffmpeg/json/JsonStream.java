package kirill.subtitlemerger.logic.ffmpeg.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;

@Getter
public class JsonStream {
    private int index;

    private String codecType;

    private String codecName;

    /*
     * Ffmpeg uses ISO 639-2 for language codes, below is the SO answer from one of the ffmpeg's developers.
     * https://stackoverflow.com/questions/44351606/ffmpeg-set-the-language-of-an-audio-stream.
     * Later it turned out that there's more to the format, it's possible to use hyphens as well, see the documentation
     * https://www.ffmpeg.org/ffmpeg-formats.html#matroska.
     */
    private Map<String, String> tags;

    private JsonDisposition disposition;

    @JsonCreator
    public JsonStream(
            @JsonProperty(value = "index", required = true)
                    int index,
            @JsonProperty(value = "codec_type")
                    String codecType,
            @JsonProperty(value = "codec_name")
                    String codecName,
            @JsonProperty(value = "tags")
                    Map<String, String> tags,
            @JsonProperty(value = "disposition", required = true)
                    JsonDisposition disposition
    ) {
        this.index = index;
        this.codecType = codecType;
        this.codecName = codecName;
        this.tags = tags;
        this.disposition = disposition;
    }
}
