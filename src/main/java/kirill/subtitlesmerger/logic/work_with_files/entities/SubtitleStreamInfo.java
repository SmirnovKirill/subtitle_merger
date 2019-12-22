package kirill.subtitlesmerger.logic.work_with_files.entities;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.Getter;

@Getter
public
class SubtitleStreamInfo {
    private int index;

    private boolean merged;

    private SubtitleCodec codec;

    /**
     * We will keep track of all subtitles for the file even if they can't be used for subtitle merging
     * (for better diagnostics). Enum contains the reason why these subtitles can't be used for subtitle merging.
     */
    private UnavailabilityReason unavailabilityReason;

    private LanguageAlpha3Code language;

    private String title;

    public SubtitleStreamInfo(
            int index,
            SubtitleCodec codec,
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
