package kirill.subtitlemerger.gui.forms.common.subtitle_preview;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
class SplitText {
    private List<String> lines;

    private boolean linesTruncated;
}
