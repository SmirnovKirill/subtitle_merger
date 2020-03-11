package kirill.subtitlemerger.logic.work_with_files.entities;

import kirill.subtitlemerger.logic.work_with_files.ffmpeg.VideoFormat;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public
class FileInfo {
    private String id;

    private File file;

    private LocalDateTime lastModified;

    private long size;

    private VideoFormat videoContainer;

    /**
     * We will keep track of all files from the selected directory even if they can't be used for subtitle merging
     * (for better diagnostics). Enum contains the reason why this file can't be used for subtitle merging.
     */
    private UnavailabilityReason unavailabilityReason;

    private List<SubtitleOption> subtitleOptions;

    @Setter
    private MergedSubtitleInfo mergedSubtitleInfo;

    public FileInfo(
            File file,
            VideoFormat videoContainer,
            UnavailabilityReason unavailabilityReason,
            List<SubtitleOption> subtitleOptions,
            MergedSubtitleInfo mergedSubtitleInfo
    ) {
        this.id = file.getAbsolutePath();
        this.file = file;
        lastModified = new LocalDateTime(file.lastModified());
        size = file.length();
        this.videoContainer = videoContainer;
        this.unavailabilityReason = unavailabilityReason;
        this.subtitleOptions = subtitleOptions;
        this.mergedSubtitleInfo = mergedSubtitleInfo;
    }

    public static FileInfo getById(String id, List<FileInfo> filesInfo) {
        return filesInfo.stream()
                .filter(fileInfo -> Objects.equals(fileInfo.getId(), id))
                .findFirst().orElseThrow(IllegalAccessError::new);
    }

    public List<FfmpegSubtitleStream> getFfmpegSubtitleStreams() {
        if (subtitleOptions == null) {
            return null;
        }

        return subtitleOptions.stream()
                .filter(option -> option instanceof FfmpegSubtitleStream)
                .map(FfmpegSubtitleStream.class::cast)
                .collect(Collectors.toList());
    }

    public List<FileWithSubtitles> getFilesWithSubtitles() {
        if (subtitleOptions == null) {
            return null;
        }

        return subtitleOptions.stream()
                .filter(option -> option instanceof FileWithSubtitles)
                .map(FileWithSubtitles.class::cast)
                .collect(Collectors.toList());
    }

    public int getNewFfmpegStreamIndex() {
        return getFfmpegSubtitleStreams().size();
    }

    public enum UnavailabilityReason {
        NO_EXTENSION,
        NOT_ALLOWED_EXTENSION,
        FAILED_TO_GET_MIME_TYPE,
        NOT_ALLOWED_MIME_TYPE,
        FAILED_TO_GET_FFPROBE_INFO,
        NOT_ALLOWED_CONTAINER
    }
}
