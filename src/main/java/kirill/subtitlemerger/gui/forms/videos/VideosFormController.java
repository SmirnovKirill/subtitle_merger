package kirill.subtitlemerger.gui.forms.videos;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.forms.MainFormController;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

@CommonsLog
public class VideosFormController {
    @FXML
    private MissingSettingsFormController missingSettingsFormController;

    @FXML
    private ChoiceFormController choiceFormController;

    @FXML
    private TableFormController contentFormController;

    private MainFormController mainFormController;

    public void initialize(MainFormController mainFormController, Stage stage, GuiContext context) {
        this.mainFormController = mainFormController;

        this.missingSettingsFormController.initialize(this, context);
        this.choiceFormController.initialize(this, contentFormController, stage, context);
        this.contentFormController.initialize(this, stage, context);

        context.getMissingSettings().addListener((InvalidationListener) observable -> {
            setActivePane(haveMissingSettings(context) ? ActivePane.MISSING_SETTINGS : ActivePane.CHOICE);
        });

        setActivePane(haveMissingSettings(context) ? ActivePane.MISSING_SETTINGS : ActivePane.CHOICE);
    }

    private static boolean haveMissingSettings(GuiContext context) {
        return !CollectionUtils.isEmpty(context.getMissingSettings());
    }

    void setActivePane(ActivePane activePane) {
        if (activePane == ActivePane.MISSING_SETTINGS) {
            missingSettingsFormController.show();
            choiceFormController.hide();
            contentFormController.hide();
        } else if (activePane == ActivePane.CHOICE) {
            missingSettingsFormController.hide();
            choiceFormController.show();
            contentFormController.hide();
        } else if (activePane == ActivePane.CONTENT) {
            missingSettingsFormController.hide();
            choiceFormController.hide();
            contentFormController.show();
        } else {
            throw new IllegalStateException();
        }
    }

    void openSettingsForm() {
        mainFormController.openSettingsForm();
    }

    public enum ActivePane {
        MISSING_SETTINGS,
        CHOICE,
        CONTENT
    }
}
