package kirill.subtitlemerger.gui.utils.entities;

import javafx.scene.Parent;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FormInfo {
    private Parent rootNode;

    private Object controller;

    public <T extends Parent> T getRootNode() {
        /* We can suppress the warning because JavaFX itself has no type checks for nodes and controllers. */
        @SuppressWarnings("unchecked")
        T result  = (T) rootNode;

        return result;
    }

    public <T> T getController() {
        /* We can suppress the warning because JavaFX itself has no type checks for nodes and controllers. */
        @SuppressWarnings("unchecked")
        T result = (T) controller;

        return result;
    }
}
