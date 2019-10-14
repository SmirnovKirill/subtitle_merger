package kirill.subtitles_merger;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class SubtitlesElementLine {
    private String text;

    private String source;
}
