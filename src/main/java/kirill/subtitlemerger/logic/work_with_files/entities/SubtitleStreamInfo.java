package kirill.subtitlemerger.logic.work_with_files.entities;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.Writer;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.Getter;

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

    private boolean defaultDisposition;

    private Subtitles subtitles;

    private Integer subtitleSize;

    public SubtitleStreamInfo(
            int id,
            SubtitleCodec codec,
            UnavailabilityReason unavailabilityReason,
            LanguageAlpha3Code language,
            String title,
            boolean defaultDisposition,
            Subtitles subtitles
    ) {
        this.id = id;
        this.merged = title != null && title.startsWith("Merged subtitles");
        this.codec = codec;
        this.unavailabilityReason = unavailabilityReason;
        this.language = language;
        this.title = title;
        this.defaultDisposition = defaultDisposition;
        this.subtitles = subtitles;
        if (subtitles != null) {
            this.subtitleSize = getSubtitleSize(subtitles);
        }
    }

    public void setSubtitlesAndSize(Subtitles subtitles) {
        this.subtitles = subtitles;
        this.subtitleSize = getSubtitleSize(subtitles);
    }

    private static int getSubtitleSize(Subtitles subtitles) {
        return Writer.toSubRipText(subtitles).getBytes().length;
    }

    public enum UnavailabilityReason {
        NOT_ALLOWED_CODEC
    }
}
