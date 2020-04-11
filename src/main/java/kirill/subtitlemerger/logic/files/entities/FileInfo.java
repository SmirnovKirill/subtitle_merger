package kirill.subtitlemerger.logic.files.entities;

import kirill.subtitlemerger.logic.ffmpeg.VideoFormat;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class FileInfo {
    private String id;

    private File file;

    private LocalDateTime lastModified;

    private long size;

    private VideoFormat format;

    /**
     * We will keep track of all the selected files even if they can't be used for subtitle merging (for better
     * diagnostics). Enum contains the reason why this file can't be used for subtitle merging.
     */
    private FileUnavailabilityReason unavailabilityReason;

    private List<SubtitleOption> subtitleOptions;

    @Setter
    private MergedSubtitleInfo mergedSubtitleInfo;

    public FileInfo(
            File file,
            VideoFormat format,
            FileUnavailabilityReason unavailabilityReason,
            List<SubtitleOption> subtitleOptions,
            MergedSubtitleInfo mergedSubtitleInfo
    ) {
        this.id = file.getAbsolutePath();
        this.file = file;
        setCurrentSizeAndLastModified();
        this.format = format;
        this.unavailabilityReason = unavailabilityReason;
        this.subtitleOptions = subtitleOptions;
        this.mergedSubtitleInfo = mergedSubtitleInfo;
    }

    public void setCurrentSizeAndLastModified() {
        lastModified = new LocalDateTime(file.lastModified());
        size = file.length();
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
}
