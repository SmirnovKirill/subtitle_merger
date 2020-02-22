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

    private List<LanguageAlpha3Code> languages;

    private int size;

    public Subtitles(List<Subtitle> subtitles, List<LanguageAlpha3Code> languages) {
        this.subtitles = subtitles;
        this.languages = languages;
        this.size = SubtitleWriter.toSubRipText(this).getBytes().length;;
    }
}
