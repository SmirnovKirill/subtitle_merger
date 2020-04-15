package kirill.subtitlemerger.logic.ffmpeg;

import java.util.Optional;

public enum SubtitleFormat {
    SUBRIP,
    ASS;

    /**
     * Get enum by a string from ffprobe's json response.
     */
    public static Optional<SubtitleFormat> from(String rawCodec) {
        if ("subrip".equals(rawCodec)) {
            return Optional.of(SUBRIP);
        } else if ("ass".equals(rawCodec)) {
            return Optional.of(ASS);
        }

        return Optional.empty();
    }
}
