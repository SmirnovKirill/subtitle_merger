package kirill.subtitlemerger.logic.subtitles.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class Subtitles {
    private List<Subtitle> subtitles;
}
