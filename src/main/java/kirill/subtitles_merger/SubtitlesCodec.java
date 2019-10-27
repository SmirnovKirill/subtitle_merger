package kirill.subtitles_merger;

import java.util.Optional;

public enum SubtitlesCodec {
    SUBRIP;

    /**
     * get enum by a string from ffprobe's json response
     */
    public static Optional<SubtitlesCodec> from(String rawCodec) {
        if ("subrip".equals(rawCodec)) {
            return Optional.of(SUBRIP);
        }

        return Optional.empty();
    }
}
