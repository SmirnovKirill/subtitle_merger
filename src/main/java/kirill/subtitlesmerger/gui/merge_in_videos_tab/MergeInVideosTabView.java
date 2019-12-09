package kirill.subtitlesmerger.gui.merge_in_videos_tab;

import javafx.scene.control.Tab;
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

    public MergeInVideosTabView(Stage stage, boolean debug) {
        this.tab = new Tab(TAB_NAME);
        this.missingSettingsPane = new MissingSettingsPane();
        this.regularContentPane = new RegularContentPane(debug);
    }

    @Override
    public String getTabName() {
        return TAB_NAME;
    }

    @Override
    public Tab getTab() {
        return tab;
    }
}
