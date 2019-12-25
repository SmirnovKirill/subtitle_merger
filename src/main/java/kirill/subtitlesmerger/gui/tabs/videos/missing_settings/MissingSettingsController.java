package kirill.subtitlesmerger.gui.tabs.videos.missing_settings;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.tabs.videos.VideosTabController;

public class MissingSettingsController {
    private VideosTabController videosTabController;

    @FXML
    private Pane pane;

    @FXML
    private PaneWithMissingSettingLabels paneWithMissingSettingLabels;

    public void initialize(VideosTabController videosTabController, GuiContext context) {
        this.videosTabController = videosTabController;
        this.paneWithMissingSettingLabels.setMissingSettings(context.getSettings().getMissingSettings());
    }

    public void show() {
        pane.setVisible(true);
    }

    public void hide() {
        pane.setVisible(false);
    }

    @FXML
    private void goToSettingsLinkClicked() {
        videosTabController.openSettingsTab();
    }
}
