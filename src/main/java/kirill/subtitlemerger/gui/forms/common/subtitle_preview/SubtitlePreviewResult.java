package kirill.subtitlemerger.gui.forms.common.subtitle_preview;

import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;

@AllArgsConstructor
@Getter
public class SubtitlePreviewResult {
    private Subtitles subtitles;

    private Charset encoding;
}
