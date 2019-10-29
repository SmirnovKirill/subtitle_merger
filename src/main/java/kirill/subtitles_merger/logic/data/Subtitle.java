package kirill.subtitles_merger.logic.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public
class Subtitle {
    private int number;

    private LocalTime from;

    private LocalTime to;

    private List<SubtitleLine> lines = new ArrayList<>();
}
