package kirill.subtitlemerger.gui.application_specific.videos_tab;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.MainPaneController;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

@CommonsLog
public class VideosTabController {
    @FXML
    private MissingSettingsPaneController missingSettingsPaneController;

    @FXML
    private ChoicePaneController choicePaneController;

    @FXML
    private ContentPaneController contentPaneController;

    private ActivePane activePane;

    private MainPaneController mainPaneController;

    public void initialize(MainPaneController mainPaneController, Stage stage, GuiContext context) {
        activePane = haveMissingSettings(context.getSettings()) ? ActivePane.MISSING_SETTINGS : ActivePane.CHOICE;
        this.mainPaneController = mainPaneController;

        this.missingSettingsPaneController.initialize(this, context);
        this.choicePaneController.initialize(this, contentPaneController, stage, context);
        this.contentPaneController.initialize(this, stage, context);

        showActivePane();

        context.getSettings().getMissingSettings().addListener((InvalidationListener) observable -> {
            if (haveMissingSettings(context.getSettings())) {
                activePane = ActivePane.MISSING_SETTINGS;
            }

            showActivePane();
        });
    }

    private static boolean haveMissingSettings(GuiSettings settings) {
        return !CollectionUtils.isEmpty(settings.getMissingSettings());
    }

    private void showActivePane() {
        if (activePane == ActivePane.MISSING_SETTINGS) {
            missingSettingsPaneController.show();
            choicePaneController.hide();
            contentPaneController.hide();
        } else if (activePane == ActivePane.CHOICE) {
            missingSettingsPaneController.hide();
            choicePaneController.show();
            contentPaneController.hide();
        } else if (activePane == ActivePane.CONTENT) {
            missingSettingsPaneController.hide();
            choicePaneController.hide();
            contentPaneController.show();
        } else {
            throw new IllegalStateException();
        }
    }

    void setActivePane(ActivePane activePane) {
        this.activePane = activePane;

        showActivePane();
    }

    void openSettingsTab() {
        mainPaneController.openSettingsTab();
    }

    public enum ActivePane {
        MISSING_SETTINGS,
        CHOICE,
        CONTENT
    }
}
