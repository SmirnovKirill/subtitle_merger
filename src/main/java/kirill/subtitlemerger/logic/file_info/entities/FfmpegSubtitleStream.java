package kirill.subtitlemerger.logic.file_info.entities;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.ffmpeg.SubtitleFormat;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;

import java.nio.charset.StandardCharsets;

@CommonsLog
@Getter
public class FfmpegSubtitleStream extends SubtitleOption {
    /*
     * The word "ffmpeg" here emphasizes the fact that it's not a regular index, but an index got from ffmpeg. For
     * example the first subtitle stream may have index 2 because the first two indices are assigned to the video and
     * audio streams.
     */
    private int ffmpegIndex;

    private SubtitleFormat format;

    private LanguageAlpha3Code language;

    private String title;

    //todo change!
    private boolean defaultDisposition;

    public FfmpegSubtitleStream(
            int ffmpegIndex,
            Subtitles subtitles,
            Integer size,
            SubtitleOptionUnavailabilityReason unavailabilityReason,
            boolean selectedAsUpper,
            boolean selectedAsLower,
            SubtitleFormat format,
            LanguageAlpha3Code language,
            String title,
            boolean defaultDisposition
    ) {
        super(
                "ffmpeg-" + ffmpegIndex,
                subtitles,
                size,
                StandardCharsets.UTF_8,
                unavailabilityReason,
                selectedAsUpper,
                selectedAsLower
        );

        this.ffmpegIndex = ffmpegIndex;
        this.format = format;
        this.language = language;
        this.title = title;
        this.defaultDisposition = defaultDisposition;
    }

    public void setSubtitlesAndSize(Subtitles subtitles, int size) {
        this.subtitles = subtitles;
        this.size = size;
    }
}
