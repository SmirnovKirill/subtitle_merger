package kirill.subtitlesmerger.gui.merge_in_videos_tab;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.TabView;
import lombok.Getter;

public class MergeInVideosTabView implements TabView {
    private static final String TAB_NAME = "Merge subtitles in videos";

    private Tab tab;

    @Getter
    private MissingSettingsPane missingSettingsPane;

    @Getter
    private RegularContentPane regularContentPane;

    @Getter
    private ProgressIndicator progressIndicator;

    public MergeInVideosTabView(Stage stage, boolean debug) {
        this.missingSettingsPane = new MissingSettingsPane();
        this.regularContentPane = new RegularContentPane(debug);
        this.progressIndicator = generateProgressIndicator();
        this.tab = generateTab(missingSettingsPane, regularContentPane, progressIndicator);
    }

    private static ProgressIndicator generateProgressIndicator() {
        ProgressIndicator result = new ProgressIndicator();

        result.setVisible(false);

        return result;
    }

    private static Tab generateTab(
            MissingSettingsPane missingSettingsPane,
            RegularContentPane regularContentPane,
            ProgressIndicator progressIndicator
    ) {
        Tab result = new Tab(TAB_NAME);

        result.setContent(generateTabContent(missingSettingsPane, regularContentPane, progressIndicator));

        return result;
    }

    private static Pane generateTabContent(
            MissingSettingsPane missingSettingsPane,
            RegularContentPane regularContentPane,
            ProgressIndicator progressIndicator
    ) {
        StackPane result = new StackPane();

        result.getChildren().addAll(
                missingSettingsPane.getMissingSettingsPane(),
                regularContentPane.getRegularContentPane(),
                progressIndicator
        );

        return result;
    }

    @Override
    public String getTabName() {
        return TAB_NAME;
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    void hideProgressIndicator() {
        progressIndicator.setVisible(false);
        regularContentPane.getRegularContentPane().setDisable(false);
    }

    void showProgressIndicator() {
        regularContentPane.getRegularContentPane().setDisable(true);
        progressIndicator.setVisible(true);
    }
}
