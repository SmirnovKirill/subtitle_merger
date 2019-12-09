package kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg.json;

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
