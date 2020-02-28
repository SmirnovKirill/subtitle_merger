package kirill.subtitlemerger.gui.utils;

import javafx.scene.Node;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NodeAndController<T extends Node, S> {
    private T node;

    private S controller;
}
