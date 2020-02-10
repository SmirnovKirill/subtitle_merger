package kirill.subtitlemerger.gui.core.entities;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class MultiPartResult {
    private String success;

    private String warn;

    private String error;
}
