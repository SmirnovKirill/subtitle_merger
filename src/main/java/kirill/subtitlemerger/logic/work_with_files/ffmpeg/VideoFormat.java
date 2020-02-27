package kirill.subtitlemerger.logic.work_with_files.ffmpeg;

import java.util.Optional;

public enum VideoFormat {
    MATROSKA;

    /**
     * get enum by a string from ffprobe's json response
     */
    public static Optional<VideoFormat> from(String rawContainer) {
        if ("matroska,webm".equals(rawContainer)) {
            return Optional.of(MATROSKA);
        }

        return Optional.empty();
    }
}
