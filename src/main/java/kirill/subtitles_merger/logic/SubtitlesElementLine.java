package kirill.subtitles_merger.logic;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
class SubtitlesElementLine {
    private String text;

    private String source;
}
