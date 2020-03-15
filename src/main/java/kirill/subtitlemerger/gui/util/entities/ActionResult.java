package kirill.subtitlemerger.gui.util.entities;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class ActionResult {
    public static final ActionResult NO_RESULT = new ActionResult(null, null, null);

    private String success;

    private String warn;

    private String error;

    public static ActionResult onlySuccess(String text) {
        return new ActionResult(text, null, null);
    }

    public static ActionResult onlyWarn(String text) {
        return new ActionResult(null, text, null);
    }

    public static ActionResult onlyError(String text) {
        return new ActionResult(null, null, text);
    }

    public boolean empty() {
        return StringUtils.isBlank(success) && StringUtils.isBlank(warn) && StringUtils.isBlank(error);
    }
}
