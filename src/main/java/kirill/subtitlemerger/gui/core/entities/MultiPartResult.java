package kirill.subtitlemerger.gui.core.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MultiPartResult {
    private String success;

    private String warn;

    private String error;
}
