package kirill.subtitlemerger.logic.work_with_files.entities;

import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Getter
public abstract class SubtitleStream {
    private SubtitleCodec codec;

    @Setter
    private Subtitles subtitles;

    public abstract String getUniqueId();

    public static SubtitleStream getByUniqueId(String id, List<SubtitleStream> allSubtitleStreams) {
        return allSubtitleStreams.stream()
                .filter(stream -> Objects.equals(stream.getUniqueId(), id))
                .findFirst().orElseThrow(IllegalAccessError::new);
    }
}
