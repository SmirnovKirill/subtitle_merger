package kirill.subtitles_merger.logic.ffmpeg.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class JsonFormat {
    @JsonProperty(value = "format_name")
    private String formatName;
}
