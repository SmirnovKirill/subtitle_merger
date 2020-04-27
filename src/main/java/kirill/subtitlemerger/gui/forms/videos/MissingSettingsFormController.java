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

    private VideosFormController videosFormController;

    private GuiContext context;

    void initialize(VideosFormController videosTabController, GuiContext context) {
        this.videosFormController = videosTabController;
        this.context = context;

        context.getMissingSettings().addListener((InvalidationListener) observable -> updateLabels());
        updateLabels();
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
                return "the preferred language for upper subtitles";
            case LOWER_LANGUAGE:
                return "the preferred language for lower subtitles";
            case MERGE_MODE:
                return "the video merge mode";
            default:
                throw new IllegalStateException();
        }
    }

    void show() {
        missingSettingsPane.setVisible(true);
    }

    void hide() {
        missingSettingsPane.setVisible(false);
    }

    @FXML
    private void goToSettingsClicked() {
        videosFormController.openSettingsForm();
    }
}
