package kirill.subtitles_merger;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class contains basic information about the subtitles, basically all information we can get with ffprobe
 * except the text of the subtitles (we have to use ffmpeg for it and it takes much time).
 */
@AllArgsConstructor
@Getter
class BriefSubtitlesStreamInfo {
    private int index;

    private SubtitlesCodec codec;

    /**
     * We will keep track of all subtitles for the file even if they can't be used for subtitles merging
     * (for better diagnostics).
     * Enum contains the reason why these subtitles can't be used for subtitles merging.
     */
    private BriefSubtitlesUnavailabilityReason unavailabilityReason;

    private LanguageAlpha3Code language;

    private String title;
}
