package kirill.subtitlesmerger.gui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;

class MergeInVideosTab {
    private Stage stage;

    private boolean debug;

    private Tab tab;

    MergeInVideosTab(Stage stage, boolean debug) {
        this.stage = stage;
        this.debug = debug;
    }

    Tab generateTab() {
        tab = new Tab("Merge subtitles in videos");

        return tab;
    }

    void showMissingSettings(
            List<String> missingSettings
    ) {
        tab.setContent(generateContentPaneMissingSettings(missingSettings));
    }

    private Node generateContentPaneMissingSettings(
            List<String> missingSettings
    ) {
        HBox result = new HBox();

        result.getChildren().add(new Label("settings are missing: " + missingSettings));

        return result;
    }

    void showRegularContent() {
        tab.setContent(generateRegularContentPane());
    }

    private Node generateRegularContentPane() {
        HBox result = new HBox();

        result.getChildren().add(new Label("everything is ok"));

        return result;
    }
}
