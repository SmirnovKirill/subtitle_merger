package kirill.subtitlemerger.gui.forms.videos;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.utils.Popups;
import kirill.subtitlemerger.logic.settings.Settings;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

@CommonsLog
public class ChoiceFormController {
    @FXML
    private Pane choicePane;

    private VideosFormController videosFormController;

    private Stage stage;

    private GuiContext context;

    void initialize(VideosFormController videosTabController, Stage stage, GuiContext context) {
        this.videosFormController = videosTabController;
        this.stage = stage;
        this.context = context;
    }

    void show() {
        choicePane.setVisible(true);
    }

    void hide() {
        choicePane.setVisible(false);
    }

    @FXML
    private void separateVideosButtonClicked() {
        List<File> videoFiles = getVideoFiles(stage, context.getSettings());
        if (CollectionUtils.isEmpty(videoFiles)) {
            return;
        }

        if (videoFiles.size() > GuiConstants.VIDEO_TABLE_LIMIT) {
            String message = "Unfortunately, it's impossible to add more than " +  GuiConstants.VIDEO_TABLE_LIMIT + " "
                    + "videos";
            Popups.showError(message, stage);
            return;
        }

        context.setVideosInProgress(true);
        videosFormController.setActivePane(ActivePane.MAIN);
        videosFormController.processChosenVideoFiles(videoFiles);
    }

    private static List<File> getVideoFiles(Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose videos");
        fileChooser.setInitialDirectory(settings.getVideoDirectory());
        fileChooser.getExtensionFilters().add(GuiConstants.VIDEO_EXTENSION_FILTER);

        return fileChooser.showOpenMultipleDialog(stage);
    }

    @FXML
    private void directoryButtonClicked() {
        File directory = getDirectory(stage, context.getSettings());
        if (directory == null) {
            return;
        }

        context.setVideosInProgress(true);
        videosFormController.setActivePane(ActivePane.MAIN);
        videosFormController.processChosenDirectory(directory);
    }

    @Nullable
    private static File getDirectory(Stage stage, Settings settings) {
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("Choose a directory with videos");
        directoryChooser.setInitialDirectory(settings.getVideoDirectory());

        return directoryChooser.showDialog(stage);
    }
}
