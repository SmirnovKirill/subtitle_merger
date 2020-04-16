package kirill.subtitlemerger.gui.utils.entities;

import kirill.subtitlemerger.gui.utils.forms_and_controls.ActionResultLabels;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class helps to represent results of complex actions that may for example finish successfully partly and partly
 * fail. This class is a POJO and isn't bound to any gui library and so can be easily tested. To display the data there
 * is a special class ActionResultLabels.
 * @see ActionResultLabels
 */
@AllArgsConstructor
@Getter
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
}
