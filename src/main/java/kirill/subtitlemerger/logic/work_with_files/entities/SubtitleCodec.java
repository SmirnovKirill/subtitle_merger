package kirill.subtitlemerger.logic.work_with_files.entities;

import java.util.Optional;

public enum SubtitleCodec {
    SUBRIP;

    /**
     * get enum by a string from ffprobe's json response
     */
    public static Optional<SubtitleCodec> from(String rawCodec) {
        if ("subrip".equals(rawCodec)) {
            return Optional.of(SUBRIP);
        }

        return Optional.empty();
    }
}
