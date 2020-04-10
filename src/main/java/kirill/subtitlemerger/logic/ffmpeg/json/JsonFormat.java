package kirill.subtitlemerger.logic.ffmpeg.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class JsonFormat {
    private String formatName;

    @JsonCreator
    public JsonFormat(
            @JsonProperty(value = "format_name")
                    String formatName
    ) {
        this.formatName = formatName;
    }
}
