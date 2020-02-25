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
    private String id;

    private SubtitleCodec codec;

    @Setter
    private Subtitles subtitles;

    public static <T extends SubtitleStream> T getById(String id, List<T> allSubtitleStreams) {
        return allSubtitleStreams.stream()
                .filter(stream -> Objects.equals(stream.getId(), id))
                .findFirst().orElseThrow(IllegalAccessError::new);
    }
}
