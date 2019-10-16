package kirill.subtitles_merger;

import java.util.Optional;

public enum SubtitlesCodec {
    SUBRIP;

    /**
     * Получение енума по строке из json-ответа ffprobe.
     */
    public static Optional<SubtitlesCodec> from(String rawCodec) {
        if ("subrip".equals(rawCodec)) {
            return Optional.of(SUBRIP);
        }

        return Optional.empty();
    }
}
