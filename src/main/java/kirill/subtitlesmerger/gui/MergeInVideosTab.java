package kirill.subtitlesmerger.gui;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

class MergeInVideosTab {
    private Stage stage;

    private TabPane mainPane;

    MergeInVideosTab(Stage stage, TabPane mainPane) {
        this.stage = stage;
        this.mainPane = mainPane;
    }

    MergeInVideosTab(TabPane mainPane) {
        this.mainPane = mainPane;
    }

    Tab generateTab() {
        Tab result = new Tab("Merge subtitles in videos");

        return result;
    }
}
