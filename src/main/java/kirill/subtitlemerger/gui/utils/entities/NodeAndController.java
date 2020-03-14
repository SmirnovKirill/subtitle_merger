package kirill.subtitlemerger.gui.utils.entities;

import javafx.scene.Parent;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NodeAndController {
    private Object node;

    private Object controller;

    public <T extends Parent> T getNode() {
        /* We can suppress the warning because JavaFX itself has no type checks for nodes and controllers. */
        @SuppressWarnings("unchecked")
        T result  = (T) node;

        return result;
    }

    public <T> T getController() {
        /* We can suppress the warning because JavaFX itself has no type checks for nodes and controllers. */
        @SuppressWarnings("unchecked")
        T result = (T) controller;

        return result;
    }
}
