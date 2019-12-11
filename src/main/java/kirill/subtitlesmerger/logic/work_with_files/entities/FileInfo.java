package kirill.subtitlesmerger.logic.work_with_files.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

@AllArgsConstructor
@Getter
public
class FileInfo {
    private File file;

    /**
     * We will keep track of all files from the selected directory even if they can't be used for subtitle merging
     * (for better diagnostics). Enum contains the reason why this file can't be used for subtitle merging.
     */
    private UnavailabilityReason unavailabilityReason;

    private VideoFormat videoContainer;

    private List<SubtitleStream> subtitleStreams;

    @AllArgsConstructor
    @Getter
    public enum UnavailabilityReason {
        NO_EXTENSION,
        NOT_ALLOWED_EXTENSION,
        FAILED_TO_GET_MIME_TYPE,
        NOT_ALLOWED_MIME_TYPE,
        FAILED_TO_GET_FFPROBE_INFO,
        NOT_ALLOWED_CONTAINER
    }
}
