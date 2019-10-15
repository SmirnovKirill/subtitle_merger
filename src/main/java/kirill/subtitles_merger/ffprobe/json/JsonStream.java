package kirill.subtitles_merger.ffprobe.json;

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

    @JsonProperty(value = "tags")
    private Map<String, String> tags;
}
