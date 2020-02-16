package kirill.subtitlemerger.logic.core.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.time.LocalTime;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Subtitle {
    private int number;

    private LocalTime from;

    private LocalTime to;

    private List<String> lines;
}
