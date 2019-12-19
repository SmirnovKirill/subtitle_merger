package kirill.subtitlesmerger.gui.tabs.merge_in_directory;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import kirill.subtitlesmerger.gui.GuiContext;

public class MissingSettingsController {
    private MergeInDirectoryTabController mergeInDirectoryTabController;

    @FXML
    private Pane pane;

    @FXML
    private PaneWithMissingSettingLabels paneWithMissingSettingLabels;

    public void initialize(MergeInDirectoryTabController mergeInDirectoryTabController, GuiContext context) {
        this.mergeInDirectoryTabController = mergeInDirectoryTabController;
        this.paneWithMissingSettingLabels.setMissingSettings(context.getSettings().getMissingSettings());
    }

    void show() {
        pane.setVisible(true);
    }

    void hide() {
        pane.setVisible(false);
    }

    @FXML
    private void goToSettingsLinkClicked() {
        mergeInDirectoryTabController.openSettingsTab();
    }
}
