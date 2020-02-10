package kirill.subtitlemerger.logic.work_with_files.entities;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class SubtitleStream {
    /*
     * The word "ffmpeg" there emphasizes the fact that it's not a regular index, but an index got from ffmpeg.
     * For example the first subtitle stream may have index 2 because the first two indices are assigned to the video
     * and audio streams.
     */
    private int ffmpegIndex;

    private SubtitleCodec codec;

    /**
     * We will keep track of all subtitles for the file even if they can't be used for subtitle merging
     * (for better diagnostics). Enum contains the reason why these subtitles can't be used for subtitle merging.
     */
    private UnavailabilityReason unavailabilityReason;

    private LanguageAlpha3Code language;

    private String title;

    private boolean defaultDisposition;

    @Setter
    private Subtitles subtitles;

    public SubtitleStream(
            int ffmpegIndex,
            SubtitleCodec codec,
            UnavailabilityReason unavailabilityReason,
            LanguageAlpha3Code language,
            String title,
            boolean defaultDisposition,
            Subtitles subtitles
    ) {
        this.ffmpegIndex = ffmpegIndex;
        this.codec = codec;
        this.unavailabilityReason = unavailabilityReason;
        this.language = language;
        this.title = title;
        this.defaultDisposition = defaultDisposition;
        this.subtitles = subtitles;
    }

    public static SubtitleStream getByFfmpegIndex(int ffmpegIndex, List<SubtitleStream> allSubtitleStreams) {
        return allSubtitleStreams.stream()
                .filter(stream -> stream.getFfmpegIndex() == ffmpegIndex)
                .findFirst().orElseThrow(IllegalAccessError::new);
    }

    public enum UnavailabilityReason {
        NOT_ALLOWED_CODEC
    }
}
