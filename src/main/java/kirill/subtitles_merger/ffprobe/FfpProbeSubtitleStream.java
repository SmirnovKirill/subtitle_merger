package kirill.subtitles_merger.ffprobe;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FfpProbeSubtitleStream {
    private int index;

    /*
     * Ffmpeg использует ISO 639-2 в качестве кода языка, ниже ответ на SO разработчика ffmpeg
     * https://stackoverflow.com/questions/44351606/ffmpeg-set-the-language-of-an-audio-stream
     */
    private LanguageAlpha3Code language;

    private String title;
}
