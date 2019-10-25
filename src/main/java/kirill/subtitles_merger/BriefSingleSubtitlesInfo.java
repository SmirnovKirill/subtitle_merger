package kirill.subtitles_merger;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * В классе содержится основная информация о субтитре, кроме самого текста субтитров. Информация получается путем
 * вызова ffprobe и происходит быстро (в отличие от ffmpeg).
 */
@AllArgsConstructor
@Getter
class BriefSingleSubtitlesInfo {
    private int index;

    private SubtitlesCodec codec;

    /**
     * Информация о всех найденных ffprobe'ом для данного видео субтитрах будет храниться, даже для тех которые не
     * подходят для использования в данной программе. В этом енуме содержатся причины непригодности к использованию
     * для лучшей диагностики.
     */
    private BriefSubtitlesUnavailabilityReason unavailabilityReason;

    private LanguageAlpha3Code language;

    private String title;
}
