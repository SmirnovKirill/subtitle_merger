package kirill.subtitlemerger.logic.work_with_files.ffmpeg.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
