package kirill.subtitles_merger;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
class Config {
    private String ffmpegPath;

    private String ffprobePath;

    private LanguageAlpha3Code upperLanguage;

    private LanguageAlpha3Code lowerLanguage;
}
