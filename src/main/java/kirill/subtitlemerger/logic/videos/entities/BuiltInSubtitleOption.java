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
            boolean selectedAsUpper,
            boolean selectedAsLower,
            String format,
            LanguageAlpha3Code language,
            String title,
            boolean defaultDisposition
    ) {
        super(
                "ffmpeg-" + ffmpegIndex,
                subtitlesAndInput,
                notValidReason,
                selectedAsUpper,
                selectedAsLower
        );

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
}
