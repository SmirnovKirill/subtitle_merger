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

    /**
     * This value will be used to get an index of a new stream.
     */
    private int currentSubtitleCount;

    private LanguageAlpha3Code language;

    private String title;

    private boolean makeDefault;

    private List<Integer> streamsToMakeNotDefaultIndices;

    private File originalVideoFile;

    private File tempVideoDirectory;
}
