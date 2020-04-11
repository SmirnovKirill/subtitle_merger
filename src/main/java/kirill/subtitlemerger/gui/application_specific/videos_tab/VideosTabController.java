package kirill.subtitlemerger.gui.application_specific.videos_tab;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.logic.settings.Settings;
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

    private MainPaneController mainPaneController;

    public void initialize(MainPaneController mainPaneController, Stage stage, GuiContext context) {
        this.mainPaneController = mainPaneController;

        this.missingSettingsPaneController.initialize(this, context);
        this.choicePaneController.initialize(this, contentPaneController, stage, context);
        this.contentPaneController.initialize(this, stage, context);

        context.getSettings().getMissingSettings().addListener((InvalidationListener) observable -> {
            setActivePane(haveMissingSettings(context.getSettings()) ? ActivePane.MISSING_SETTINGS : ActivePane.CHOICE);
        });

        setActivePane(haveMissingSettings(context.getSettings()) ? ActivePane.MISSING_SETTINGS : ActivePane.CHOICE);
    }

    private static boolean haveMissingSettings(Settings settings) {
        return !CollectionUtils.isEmpty(settings.getMissingSettings());
    }

    void setActivePane(ActivePane activePane) {
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

    void openSettingsTab() {
        mainPaneController.openSettingsTab();
    }

    public enum ActivePane {
        MISSING_SETTINGS,
        CHOICE,
        CONTENT
    }
}
