package kirill.subtitlemerger.logic.videos.entities;

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
public class VideoInfo {
    private String id;

    private File file;

    private LocalDateTime lastModified;

    private long size;

    private String format;

    /**
     * We will keep track of all the selected videos even if they can't be used for subtitle merging (for better
     * diagnostics). The enum contains the reason why this video can't be used for the subtitle merging.
     */
    private VideoNotValidReason notValidReason;

    private List<SubtitleOption> subtitleOptions;

    @Setter
    private MergedSubtitleInfo mergedSubtitleInfo;

    public VideoInfo(
            File file,
            String format,
            VideoNotValidReason notValidReason,
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

    public static VideoInfo getById(String id, List<VideoInfo> videosInfo) {
        VideoInfo result = videosInfo.stream()
                .filter(videoInfo -> Objects.equals(videoInfo.getId(), id))
                .findFirst().orElse(null);
        if (result == null) {
            log.error("no video info for id " + id + ", most likely a bug");
            throw new IllegalStateException();
        }

        return result;
    }

    public List<BuiltInSubtitleOption> getBuiltInSubtitleOptions() {
        if (subtitleOptions == null) {
            return null;
        }

        return subtitleOptions.stream()
                .filter(option -> option instanceof BuiltInSubtitleOption)
                .map(BuiltInSubtitleOption.class::cast)
                .collect(Collectors.toList());
    }

    public List<ExternalSubtitleOption> getExternalSubtitles() {
        if (subtitleOptions == null) {
            return null;
        }

        return subtitleOptions.stream()
                .filter(option -> option instanceof ExternalSubtitleOption)
                .map(ExternalSubtitleOption.class::cast)
                .collect(Collectors.toList());
    }
}
