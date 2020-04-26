package kirill.subtitlemerger.logic.video_files.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CommonsLog
@Getter
public class VideoFile {
    private String id;

    private File file;

    private LocalDateTime lastModified;

    private long size;

    private String format;

    /**
     * We will keep track of all the selected files even if they can't be used for subtitle merging (for better
     * diagnostics). The enum contains the reason why this file can't be used for the subtitle merging.
     */
    private VideoFileNotValidReason notValidReason;

    private List<SubtitleOption> subtitleOptions;

    @Setter
    private MergedSubtitleInfo mergedSubtitleInfo;

    public VideoFile(
            File file,
            String format,
            VideoFileNotValidReason notValidReason,
            List<SubtitleOption> subtitleOptions,
            MergedSubtitleInfo mergedSubtitleInfo
    ) {
        id = file.getAbsolutePath();
        this.file = file;
        setCurrentSizeAndLastModified();
        this.format = format;
        this.notValidReason = notValidReason;
        this.subtitleOptions = subtitleOptions;
        this.mergedSubtitleInfo = mergedSubtitleInfo;
    }

    public void setCurrentSizeAndLastModified() {
        lastModified = new LocalDateTime(file.lastModified());
        size = file.length();
    }

    public static VideoFile getById(String id, List<VideoFile> filesInfo) {
        VideoFile result = filesInfo.stream()
                .filter(fileInfo -> Objects.equals(fileInfo.getId(), id))
                .findFirst().orElse(null);
        if (result == null) {
            log.error("no file info for id " + id + ", most likely a bug");
            throw new IllegalStateException();
        }

        return result;
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
