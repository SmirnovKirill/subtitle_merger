package kirill.subtitlesmerger.gui;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MergeInVideosTab {
    private TabPane mainPane;

    public MergeInVideosTab(TabPane mainPane) {
        this.mainPane = mainPane;
    }

    public Tab generateTab() {
        Tab result = new Tab("Merge subtitles in videos");

        return result;
    }
}
