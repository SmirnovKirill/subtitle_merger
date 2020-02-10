package kirill.subtitlemerger.logic.work_with_files.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
public
class FileInfo {
    //todo check all file related invocations
    private File file;

    private LocalDateTime lastModified;

    private long size;

    /**
     * We will keep track of all files from the selected directory even if they can't be used for subtitle merging
     * (for better diagnostics). Enum contains the reason why this file can't be used for subtitle merging.
     */
    private UnavailabilityReason unavailabilityReason;

    private VideoFormat videoContainer;

    private List<SubtitleStream> subtitleStreams;

    private List<ExternalSubtitleFile> externalSubtitleFiles;

    public FileInfo(
            File file,
            UnavailabilityReason unavailabilityReason,
            VideoFormat videoContainer,
            List<SubtitleStream> subtitleStreams
    ) {
        this.file = file;
        this.lastModified = new LocalDateTime(file.lastModified());
        this.size = file.length();
        this.unavailabilityReason = unavailabilityReason;
        this.videoContainer = videoContainer;
        this.subtitleStreams = subtitleStreams;
        this.externalSubtitleFiles = new ArrayList<>();
    }

    public boolean haveSubtitlesToLoad() {
        if (CollectionUtils.isEmpty(subtitleStreams)) {
            return false;
        }

        return subtitleStreams.stream().anyMatch(stream -> stream.getSubtitles() == null);
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
