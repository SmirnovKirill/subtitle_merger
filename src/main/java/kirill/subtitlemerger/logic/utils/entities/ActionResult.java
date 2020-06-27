package kirill.subtitlemerger.logic.utils.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class helps to represent results of simple actions that don't consist of parts unlike with the
 * MultiPartActionResult. This class is a POJO and isn't bound to any gui library and so can easily be tested.
 *
 * @see MultiPartActionResult
 */
@AllArgsConstructor
@Getter
public class ActionResult {
    public static final ActionResult EMPTY = new ActionResult(ActionResultType.SUCCESS, null);

    private ActionResultType type;

    private String text;

    public static ActionResult success(String text) {
        return new ActionResult(ActionResultType.SUCCESS, text);
    }

    public static ActionResult warning(String text) {
        return new ActionResult(ActionResultType.WARNING, text);
    }

    public static ActionResult error(String text) {
        return new ActionResult(ActionResultType.ERROR, text);
    }
}
