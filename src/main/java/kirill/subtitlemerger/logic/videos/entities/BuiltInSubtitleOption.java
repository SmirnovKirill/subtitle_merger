package kirill.subtitlemerger.logic.videos.entities;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
@Getter
public class BuiltInSubtitleOption extends SubtitleOption {
    private static final String MERGED_SUBTITLE_REGEXP = "^merged-"
            + "(external|unknown|undefined|[a-z]{3})-(external|unknown|undefined|[a-z]{3})$";
    /*
     * The word "ffmpeg" here emphasizes the fact that it's not a regular index, but an index got from ffmpeg. For
     * example the first subtitle stream may have an index 2 because the first two indices are assigned to the video and
     * audio streams.
     */
    private int ffmpegIndex;

    private String format;

    private LanguageAlpha3Code language;

    private String title;

    private boolean defaultDisposition;

    private boolean merged;

    public BuiltInSubtitleOption(
            int ffmpegIndex,
            SubtitlesAndInput subtitlesAndInput,
            SubtitleOptionNotValidReason notValidReason,
            String format,
            LanguageAlpha3Code language,
            String title,
            boolean defaultDisposition
    ) {
        super("ffmpeg-" + ffmpegIndex, subtitlesAndInput, notValidReason);

        this.ffmpegIndex = ffmpegIndex;
        this.format = format;
        this.language = language;
        this.title = title;
        this.defaultDisposition = defaultDisposition;

        merged  = title != null && title.matches(MERGED_SUBTITLE_REGEXP);
    }

    public void disableDefaultDisposition() {
        if (defaultDisposition) {
            defaultDisposition = false;
        }
    }

    /**
     * @return the three-letter code of the upper language used for the merge if it was known, "unknown" if the
     * language was unknown and "external" if an external file was used (thus no language code).
     */
    public String getMergedUpperCode() {
        if (!merged) {
            log.error("subtitle option " + getId() + " isn't merged, can't extract the language, most likely a bug");
            throw new IllegalStateException();
        }

        return getValidatedMergedCode(title.split("-")[1]);
    }

    private static String getValidatedMergedCode(String code) {
        switch (code) {
            case "external":
            case "unknown":
                return code;
            case "undefined":
                return "unknown";
            default:
                LanguageAlpha3Code languageCode = LanguageAlpha3Code.getByCode(code);
                if (languageCode == null) {
                    log.warn(code + " is an incorrect language code, maybe user just messed up the title");
                    return "unknown";
                }

                return code;
        }
    }

    /**
     * @return the three-letter code of the lower language used for the merge if it was known, "unknown" if the
     * language was unknown and "external" if an external file was used (thus no language code).
     */
    public String getMergedLowerCode() {
        if (!merged) {
            log.error("subtitle option " + getId() + " isn't merged, can't extract the language, most likely a bug");
            throw new IllegalStateException();
        }

        return getValidatedMergedCode(title.split("-")[2]);
    }
}
