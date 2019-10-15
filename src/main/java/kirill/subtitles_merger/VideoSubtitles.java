package kirill.subtitles_merger;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitles_merger.logic.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VideoSubtitles {
    private int index;

    private LanguageAlpha3Code language;

    private String title;

    private Subtitles subtitles;
}
