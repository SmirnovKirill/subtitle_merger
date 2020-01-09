package kirill.subtitlemerger.logic.work_with_files.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.List;

@AllArgsConstructor
@Getter
public
class FileInfo {
    private File file;

    private LocalDateTime lastModified;

    private long size;

    /**
     * We will keep track of all files from the selected directory even if they can't be used for subtitle merging
     * (for better diagnostics). Enum contains the reason why this file can't be used for subtitle merging.
     */
    private UnavailabilityReason unavailabilityReason;

    private VideoFormat videoContainer;

    private List<SubtitleStreamInfo> subtitleStreamsInfo;

    public FileInfo(
            File file,
            UnavailabilityReason unavailabilityReason,
            VideoFormat videoContainer,
            List<SubtitleStreamInfo> subtitleStreamsInfo
    ) {
        this.file = file;
        this.lastModified = new LocalDateTime(file.lastModified());
        this.size = file.length();
        this.unavailabilityReason = unavailabilityReason;
        this.videoContainer = videoContainer;
        this.subtitleStreamsInfo = subtitleStreamsInfo;
    }

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
