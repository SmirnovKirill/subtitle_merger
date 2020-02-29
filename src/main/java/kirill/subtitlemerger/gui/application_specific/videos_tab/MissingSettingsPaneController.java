package kirill.subtitlemerger.gui.application_specific.videos_tab;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import org.apache.commons.collections4.CollectionUtils;

public class MissingSettingsPaneController {
    @FXML
    private Pane missingSettingsPane;

    @FXML
    private Pane labelsPane;

    private VideosTabController videosTabController;

    private GuiContext context;

    public void initialize(VideosTabController videosTabController, GuiContext context) {
        this.videosTabController = videosTabController;
        this.context = context;

        updateLabels();
        context.getSettings().getMissingSettings().addListener((InvalidationListener) observable -> updateLabels());
    }

    private void updateLabels() {
        labelsPane.getChildren().clear();

        if (CollectionUtils.isEmpty(context.getSettings().getMissingSettings())) {
            return;
        }

        for (GuiSettings.SettingType settingType : context.getSettings().getMissingSettings()) {
            labelsPane.getChildren().add(new Label("\u2022 " + getText(settingType)));
        }
    }

    private static String getText(GuiSettings.SettingType settingType) {
        switch (settingType) {
            case FFPROBE_PATH:
                return "path to ffprobe";
            case FFMPEG_PATH:
                return "path to ffmpeg";
            case UPPER_LANGUAGE:
                return "preferred language for upper subtitles";
            case LOWER_LANGUAGE:
                return "preferred language for lower subtitles";
            case MERGE_MODE:
                return "video merge mode";
            default:
                throw new IllegalStateException();
        }
    }

    public void show() {
        missingSettingsPane.setVisible(true);
    }

    public void hide() {
        missingSettingsPane.setVisible(false);
    }

    @FXML
    private void goToSettingsLinkClicked() {
        videosTabController.openSettingsTab();
    }
}
