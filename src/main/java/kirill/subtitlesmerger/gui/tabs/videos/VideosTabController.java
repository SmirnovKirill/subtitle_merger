package kirill.subtitlesmerger.gui.tabs.videos;

import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.GuiSettings;
import kirill.subtitlesmerger.gui.tabs.TabPaneController;
import kirill.subtitlesmerger.gui.tabs.videos.missing_settings.MissingSettingsController;
import kirill.subtitlesmerger.gui.tabs.videos.regular_content.RegularContentController;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

@CommonsLog
public class VideosTabController {
    private TabPaneController tabPaneController;

    private GuiSettings settings;

    @FXML
    private MissingSettingsController missingSettingsController;

    @FXML
    private RegularContentController regularContentController;

    public void initialize(Stage stage, TabPaneController tabPaneController, GuiContext context) {
        this.tabPaneController = tabPaneController;
        this.settings = context.getSettings();

        this.missingSettingsController.initialize(this, context);
        this.regularContentController.initialize(stage, context);

        setActivePane(CollectionUtils.isEmpty(this.settings.getMissingSettings()));

        context.getSettings().getMissingSettings().addListener(this::missingSettingsChanged);
    }

    private void setActivePane(boolean noMissingSettings) {
        if (noMissingSettings) {
            this.regularContentController.show();
            this.missingSettingsController.hide();
        } else {
            this.regularContentController.hide();
            this.missingSettingsController.show();
        }
    }

    private void missingSettingsChanged(SetChangeListener.Change<? extends GuiSettings.SettingType> change) {
        setActivePane(CollectionUtils.isEmpty(this.settings.getMissingSettings()));
    }

    public void openSettingsTab() {
        tabPaneController.openSettingsTab();
    }
}
