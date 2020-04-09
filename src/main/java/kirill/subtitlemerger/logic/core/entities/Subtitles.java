package kirill.subtitlemerger.logic.core.entities;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.SubRipWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Getter
public class Subtitles {
    private List<Subtitle> subtitles;

    private LanguageAlpha3Code language;

    private String text;

    public Subtitles(List<Subtitle> subtitles, LanguageAlpha3Code language) {
        this.subtitles = subtitles;
        this.language = language;
        this.text = SubRipWriter.toText(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Subtitles other = (Subtitles) o;
        return Objects.equals(text, other.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    public int getTextSize() {
        return text.getBytes().length;
    }
}
