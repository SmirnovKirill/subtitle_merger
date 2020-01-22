package kirill.subtitlemerger.logic.work_with_files.ffmpeg.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
