package kirill.subtitlemerger.logic.work_with_files.entities;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.Getter;
import lombok.Setter;

@Getter
public
class SubtitleStreamInfo {
    private int id;

    private boolean merged;

    private SubtitleCodec codec;

    /**
     * We will keep track of all subtitles for the file even if they can't be used for subtitle merging
     * (for better diagnostics). Enum contains the reason why these subtitles can't be used for subtitle merging.
     */
    private UnavailabilityReason unavailabilityReason;

    private LanguageAlpha3Code language;

    private String title;

    @Setter
    private Subtitles subtitles;

    public SubtitleStreamInfo(
            int id,
            SubtitleCodec codec,
            UnavailabilityReason unavailabilityReason,
            LanguageAlpha3Code language,
            String title,
            Subtitles subtitles
    ) {
        this.id = id;
        this.merged = title != null && title.startsWith("Merged subtitles");
        this.codec = codec;
        this.unavailabilityReason = unavailabilityReason;
        this.language = language;
        this.title = title;
        this.subtitles = subtitles;
    }

    public enum UnavailabilityReason {
        NOT_ALLOWED_CODEC
    }
}
