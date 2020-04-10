package kirill.subtitlemerger.logic.work_with_files.ffmpeg.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class JsonDisposition {
    private int defaultDisposition;

    @JsonCreator
    public JsonDisposition(
            @JsonProperty(value = "default", required = true)
                    int defaultDisposition
    ) {
        this.defaultDisposition = defaultDisposition;
    }
}
