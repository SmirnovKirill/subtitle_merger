package kirill.subtitlemerger.logic.work_with_files.entities;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.SubtitleCodec;
import lombok.Getter;

@Getter
public class FfmpegSubtitleStream extends SubtitleStream {
    /*
     * The word "ffmpeg" there emphasizes the fact that it's not a regular index, but an index got from ffmpeg.
     * For example the first subtitle stream may have index 2 because the first two indices are assigned to the video
     * and audio streams.
     */
    private int ffmpegIndex;

    /**
     * We will keep track of all subtitles for the file even if they can't be used for subtitle merging
     * (for better diagnostics). Enum contains the reason why these subtitles can't be used for subtitle merging.
     */
    private UnavailabilityReason unavailabilityReason;

    private LanguageAlpha3Code language;

    private String title;

    private boolean defaultDisposition;

    public FfmpegSubtitleStream(
            SubtitleCodec codec,
            Subtitles subtitles,
            int ffmpegIndex,
            UnavailabilityReason unavailabilityReason,
            LanguageAlpha3Code language,
            String title,
            boolean defaultDisposition
    ) {
        super("ffmpeg-" + ffmpegIndex, codec, subtitles);

        this.ffmpegIndex = ffmpegIndex;
        this.unavailabilityReason = unavailabilityReason;
        this.language = language;
        this.title = title;
        this.defaultDisposition = defaultDisposition;
    }

    public enum UnavailabilityReason {
        NOT_ALLOWED_CODEC
    }
}
