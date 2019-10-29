package kirill.subtitles_merger.logic.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public
class SubtitleLine {
    private String text;

    private String source;
}
