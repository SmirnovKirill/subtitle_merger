package kirill.subtitles_merger.ffmpeg.json;

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
     * Ffmpeg использует ISO 639-2 в качестве кода языка, ниже ответ на SO разработчика ffmpeg
     * https://stackoverflow.com/questions/44351606/ffmpeg-set-the-language-of-an-audio-stream.
     * Потом оказалось что не только такой формат, есть еще через дефис, это есть в документации
     * https://www.ffmpeg.org/ffmpeg-formats.html#matroska.
     */
    @JsonProperty(value = "tags")
    private Map<String, String> tags;
}
