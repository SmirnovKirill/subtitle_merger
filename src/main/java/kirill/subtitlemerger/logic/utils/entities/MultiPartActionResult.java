package kirill.subtitlemerger.logic.utils.entities;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * This class helps to represent results of complex actions that may for example finish successfully partly and partly
 * fail. This class is a POJO and isn't bound to any gui library and so can easily be tested.
 *
 * @see ActionResult
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode /* For tests only. */
@ToString /* For tests only. */
public class MultiPartActionResult {
    public static final MultiPartActionResult EMPTY = new MultiPartActionResult(null, null, null);

    private String success;

    private String warning;

    private String error;

    public static MultiPartActionResult onlySuccess(String text) {
        return new MultiPartActionResult(text, null, null);
    }

    public static MultiPartActionResult onlyWarning(String text) {
        return new MultiPartActionResult(null, text, null);
    }

    public static MultiPartActionResult onlyError(String text) {
        return new MultiPartActionResult(null, null, text);
    }

    public boolean haveWarnings() {
        return !StringUtils.isBlank(warning);
    }

    public boolean haveErrors() {
        return !StringUtils.isBlank(error);
    }
}
