package kirill.subtitlemerger.logic.videos.entities;

import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains information about a chosen video. Note that it can be used for files that are not actual videos
 * as well. For example when the user chooses a directory with videos, each of the files from that directory will be
 * represented by this class with an appropriate notValidReason, it's done for better diagnostics.
 */
@CommonsLog
@Getter
public class Video {
    private String id;

    private File file;

    /**
     * We will keep track of all the selected videos even if they can't be used for subtitle merging (for better
     * diagnostics). The enum contains the reason why this video can't be used for subtitle merging.
     */
    private VideoNotValidReason notValidReason;

    private String format;

    private List<SubtitleOption> options;

    public Video(
            File file,
            VideoNotValidReason notValidReason,
            String format,
            List<SubtitleOption> options
    ) {
        id = file.getAbsolutePath();
        this.file = file;
        this.notValidReason = notValidReason;
        this.format = format;
        this.options = options;
    }

    public long getSize() {
        return file.length();
    }

    public LocalDateTime getLastModified() {
        return new LocalDateTime(file.lastModified());
    }

    public static Video getById(String id, List<Video> videos) {
        Video result = videos.stream().filter(video -> Objects.equals(video.getId(), id)).findFirst().orElse(null);
        if (result == null) {
            log.error("no videos with id " + id + ", most likely a bug");
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
