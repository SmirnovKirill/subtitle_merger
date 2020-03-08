package kirill.subtitlemerger.logic.core.entities;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.SubtitleWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class Subtitles {
    private List<Subtitle> subtitles;

    private LanguageAlpha3Code language;

    private int size;

    public Subtitles(List<Subtitle> subtitles, LanguageAlpha3Code language) {
        this.subtitles = subtitles;
        this.language = language;
        this.size = SubtitleWriter.toSubRipText(this).getBytes().length;;
    }
}
