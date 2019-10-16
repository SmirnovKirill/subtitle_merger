package kirill.subtitles_merger;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FileUnavailabilityReason {
    NO_EXTENSION,
    NOT_ALLOWED_EXTENSION,
    FAILED_TO_GET_MIME_TYPE,
    NOT_ALLOWED_MIME_TYPE,
    FAILED_TO_GET_FFPROBE_INFO,
    NOT_ALLOWED_CONTAINER
}
