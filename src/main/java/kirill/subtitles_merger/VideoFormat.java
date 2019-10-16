package kirill.subtitles_merger;

import java.util.Optional;

public enum VideoFormat {
    MATROSKA;

    /**
     * Получение енума по строке из json-ответа ffprobe.
     */
    public static Optional<VideoFormat> from(String rawContainer) {
        if ("matroska,webm".equals(rawContainer)) {
            return Optional.of(MATROSKA);
        }

        return Optional.empty();
    }
}
