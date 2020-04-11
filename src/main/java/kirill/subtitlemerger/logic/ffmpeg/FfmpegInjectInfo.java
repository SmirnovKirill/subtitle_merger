package kirill.subtitlemerger.logic.ffmpeg;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

@AllArgsConstructor
@Getter
public class FfmpegInjectInfo {
    private String subtitles;

    private int newStreamIndex;

    private LanguageAlpha3Code language;

    private String title;

    private boolean defaultDisposition;

    private List<Integer> defaultDispositionStreamIndices;

    private File fileWithVideo;

    private File directoryForTempFile;
}
