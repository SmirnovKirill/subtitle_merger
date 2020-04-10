package kirill.subtitlemerger.logic.work_with_files.ffmpeg.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class JsonFfprobeFileInfo {
    private List<JsonStream> streams;

    private JsonFormat format;

    @JsonCreator
    public JsonFfprobeFileInfo(
            @JsonProperty(value = "streams")
                    List<JsonStream> streams,
            @JsonProperty(value = "format")
                    JsonFormat format
    ) {
        this.streams = streams;
        this.format = format;
    }
}
