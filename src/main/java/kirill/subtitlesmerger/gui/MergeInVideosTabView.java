package kirill.subtitlesmerger.gui;

import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

class MergeInVideosTabView implements TabView {
    private static final String TAB_NAME = "Merge subtitles in videos";

    private Stage stage;

    private boolean debug;

    private Tab tab;

    private Hyperlink goToSettingsLink;

    MergeInVideosTabView(Stage stage, boolean debug) {
        this.stage = stage;
        this.debug = debug;
    }

    Tab generateTab() {
        tab = new Tab(TAB_NAME);

        return tab;
    }

    @Override
    public String getTabName() {
        return TAB_NAME;
    }

    void showMissingSettings(
            List<String> missingSettings
    ) {
        tab.setContent(generateContentPaneMissingSettings(missingSettings));
    }

    private Node generateContentPaneMissingSettings(
            List<String> missingSettings
    ) {
        VBox result = new VBox();
        result.setPadding(GuiLauncher.TAB_PADDING);
        result.setSpacing(10);

        result.getChildren().add(new Label("The following settings are missing:"));
        for (String setting : missingSettings) {
            result.getChildren().add(new Label("\u2022 " + setting));
        }

        goToSettingsLink = new Hyperlink();
        goToSettingsLink.setText("open settings tab");
        result.getChildren().add(goToSettingsLink);

        return result;
    }

    void showRegularContent() {
        tab.setContent(generateRegularContentPane());
    }

    private Node generateRegularContentPane() {
        HBox result = new HBox();
        result.setPadding(GuiLauncher.TAB_PADDING);

        result.getChildren().add(new Label("everything is ok"));

        return result;
    }
}
