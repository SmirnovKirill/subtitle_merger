package kirill.subtitlemerger.logic.videos.entities;

import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the subtitles to choose from for a video. There are two kinds of subtitles - external subtitles
 * (an arbitrary file with subtitles) and subtitles that are already built-in.
 */
@CommonsLog
@AllArgsConstructor
@Getter
public abstract class SubtitleOption {
    private String id;

    @Setter
    @Nullable
    private SubtitlesAndInput subtitlesAndInput;

    /**
     * We will keep track of all options for the video even if they can't be used for subtitle merging (for better
     * diagnostics). The enum contains the reason why this option can't be used for subtitle merging.
     */
    private SubtitleOptionNotValidReason notValidReason;

    @Nullable
    public Charset getEncoding() {
        return subtitlesAndInput != null ? subtitlesAndInput.getEncoding() : null;
    }

    @Nullable
    public Subtitles getSubtitles() {
        return subtitlesAndInput != null && subtitlesAndInput.isCorrectFormat()
                ? subtitlesAndInput.getSubtitles()
                : null;
    }

    @Nullable
    public Integer getSize() {
        return subtitlesAndInput != null ? subtitlesAndInput.getSize() : null;
    }

    static <T extends SubtitleOption> T getById(String id, List<T> options) {
        T result = options.stream().filter(option -> Objects.equals(option.getId(), id)).findFirst().orElse(null);
        if (result == null) {
            log.error("no subtitle options with id " + id + ", most likely a bug");
            throw new IllegalStateException();
        }

        return result;
    }
}