package kirill.subtitlemerger.logic.video_files.entities;

public enum VideoFileNotValidReason {
    NO_EXTENSION,
    NOT_ALLOWED_EXTENSION,
    FFPROBE_FAILED,
    NOT_ALLOWED_FORMAT
}
