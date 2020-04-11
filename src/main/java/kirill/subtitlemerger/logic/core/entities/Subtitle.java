package kirill.subtitlemerger.logic.core.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.LocalTime;

import java.util.List;

@AllArgsConstructor
@Getter
public class Subtitle {
    private LocalTime from;

    private LocalTime to;

    private List<String> lines;
}
