package kirill.subtitlemerger.logic.ffmpeg.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class JsonFfprobeVideoInfo {
    private List<JsonStream> streams;

    private JsonFormat format;

    @JsonCreator
    public JsonFfprobeVideoInfo(
            @JsonProperty(value = "streams")
                    List<JsonStream> streams,
            @JsonProperty(value = "format")
                    JsonFormat format
    ) {
        this.streams = streams;
        this.format = format;
    }
}
