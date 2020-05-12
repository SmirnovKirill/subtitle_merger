package kirill.subtitlemerger.logic.videos.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains information about a chosen video. Note that it can be used for files that are not actual
 * videos as well. For example when the user chooses the directory with videos, each of the files from that directory
 * will be represented by this class with the appropriate notValidReason, it's done for better diagnostics.
 */
@CommonsLog
@Getter
public class Video {
    private String id;

    private File file;

    private LocalDateTime lastModified;

    private long size;

    /**
     * We will keep track of all the selected videos even if they can't be used for subtitle merging (for better
     * diagnostics). The enum contains the reason why this video can't be used for the subtitle merging.
     */
    private VideoNotValidReason notValidReason;

    private String format;

    private List<SubtitleOption> options;

    @Setter
    @Nullable
    private MergedSubtitleInfo mergedSubtitleInfo;

    public Video(
            File file,
            VideoNotValidReason notValidReason,
            String format,
            List<SubtitleOption> options,
            @Nullable MergedSubtitleInfo mergedSubtitleInfo
    ) {
        id = file.getAbsolutePath();
        this.file = file;
        setCurrentSizeAndLastModified();
        this.notValidReason = notValidReason;
        this.format = format;
        this.options = options;
        this.mergedSubtitleInfo = mergedSubtitleInfo;
    }

    public void setCurrentSizeAndLastModified() {
        lastModified = new LocalDateTime(file.lastModified());
        size = file.length();
    }

    public static Video getById(String id, List<Video> videos) {
        Video result = videos.stream().filter(video -> Objects.equals(video.getId(), id)).findFirst().orElse(null);
        if (result == null) {
            log.error("no video for id " + id + ", most likely a bug");
            throw new IllegalStateException();
        }

        return result;
    }

    public SubtitleOption getOption(String id) {
        return SubtitleOption.getById(id, options);
    }

    public BuiltInSubtitleOption getBuiltInOption(String id) {
        return SubtitleOption.getById(id, getBuiltInOptions());
    }

    public List<BuiltInSubtitleOption> getBuiltInOptions() {
        if (options == null) {
            return null;
        }

        return options.stream()
                .filter(option -> option instanceof BuiltInSubtitleOption)
                .map(BuiltInSubtitleOption.class::cast)
                .collect(Collectors.toList());
    }

    public List<ExternalSubtitleOption> getExternalOptions() {
        if (options == null) {
            return null;
        }

        return options.stream()
                .filter(option -> option instanceof ExternalSubtitleOption)
                .map(ExternalSubtitleOption.class::cast)
                .collect(Collectors.toList());
    }

    public List<BuiltInSubtitleOption> getOptionsToLoad() {
        return getBuiltInOptions().stream()
                .filter(option -> option.getNotValidReason() == null && option.getSubtitlesAndInput() == null)
                .collect(Collectors.toList());
    }
}
