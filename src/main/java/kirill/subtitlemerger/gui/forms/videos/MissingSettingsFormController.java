package kirill.subtitlemerger.gui.forms.videos;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.logic.settings.SettingType;
import org.apache.commons.collections4.CollectionUtils;

public class MissingSettingsFormController {
    @FXML
    private Pane missingSettingsPane;

    @FXML
    private Pane labelsPane;

    private VideosFormController videosTabController;

    private GuiContext context;

    public void initialize(VideosFormController videosTabController, GuiContext context) {
        this.videosTabController = videosTabController;
        this.context = context;

        updateLabels();
        context.getMissingSettings().addListener((InvalidationListener) observable -> updateLabels());
    }

    private void updateLabels() {
        labelsPane.getChildren().clear();

        if (CollectionUtils.isEmpty(context.getMissingSettings())) {
            return;
        }

        for (SettingType settingType : context.getMissingSettings()) {
            labelsPane.getChildren().add(new Label("\u2022 " + getText(settingType)));
        }
    }

    private static String getText(SettingType settingType) {
        switch (settingType) {
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
