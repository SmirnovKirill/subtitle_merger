package kirill.subtitlesmerger.logic.merge_in_files.entities;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.Getter;

/**
 * This class contains basic information about the subtitles, basically all information we can get with ffprobe
 * except the text of the subtitles (we have to use ffmpeg for it and it takes much time).
 */
@Getter
public
class BriefSubtitlesStreamInfo {
    private int index;

    private boolean merged;

    private SubtitlesCodec codec;

    /**
     * We will keep track of all subtitles for the file even if they can't be used for subtitles merging
     * (for better diagnostics).
     * Enum contains the reason why these subtitles can't be used for subtitles merging.
     */
    private UnavailabilityReason unavailabilityReason;

    private LanguageAlpha3Code language;

    private String title;

    public BriefSubtitlesStreamInfo(
            int index,
            SubtitlesCodec codec,
            UnavailabilityReason unavailabilityReason,
            LanguageAlpha3Code language,
            String title
    ) {
        this.index = index;
        this.merged = title != null && title.startsWith("Merged subtitles");
        this.codec = codec;
        this.unavailabilityReason = unavailabilityReason;
        this.language = language;
        this.title = title;
    }

    public enum UnavailabilityReason {
        NOT_ALLOWED_CODEC
    }
}
