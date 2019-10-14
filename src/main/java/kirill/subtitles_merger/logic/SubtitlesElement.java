package kirill.subtitles_merger.logic;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
class SubtitlesElement {
    private int number;

    private LocalTime from;

    private LocalTime to;

    private List<SubtitlesElementLine> lines = new ArrayList<>();
}
