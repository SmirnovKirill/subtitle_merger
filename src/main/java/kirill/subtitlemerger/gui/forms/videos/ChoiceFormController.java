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
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class ChoiceFormController {
    @FXML
    private Pane choicePane;

    private VideosFormController videosFormController;

    private Stage stage;

    private Settings settings;

    void initialize(VideosFormController videosTabController, Stage stage, GuiContext context) {
        this.videosFormController = videosTabController;
        this.stage = stage;
        settings = context.getSettings();
    }

    @FXML
    private void separateVideosButtonClicked() {
        List<File> videoFiles = getVideoFiles(stage, settings);
        if (CollectionUtils.isEmpty(videoFiles)) {
            return;
        }

        if (videoFiles.size() > GuiConstants.VIDEO_TABLE_LIMIT) {
            String message = "Unfortunately, it's impossible to add more than " +  GuiConstants.VIDEO_TABLE_LIMIT + " "
                    + "videos";
            Popups.showError(message, stage);
            return;
        }

        videosFormController.setActivePane(ActivePane.MAIN);
        videosFormController.processChosenVideoFiles(videoFiles);
    }

    private static List<File> getVideoFiles(Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose videos");
        fileChooser.setInitialDirectory(settings.getLastDirectoryWithVideos());
        fileChooser.getExtensionFilters().add(GuiConstants.VIDEO_EXTENSION_FILTER);

        return fileChooser.showOpenMultipleDialog(stage);
    }

    @FXML
    private void directoryButtonClicked() {
        File directory = getDirectory(stage, settings);
        if (directory == null) {
            return;
        }

        videosFormController.setActivePane(ActivePane.MAIN);
        videosFormController.processChosenDirectory(directory);
    }

    @Nullable
    private static File getDirectory(Stage stage, Settings settings) {
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("Choose a directory with videos");

        /* We have to validate this value because otherwise JavaFX will throw an exception. */
        File initialDirectory = settings.getLastDirectoryWithVideos();
        if (initialDirectory != null && initialDirectory.isDirectory()) {
            directoryChooser.setInitialDirectory(settings.getLastDirectoryWithVideos());
        }

        return directoryChooser.showDialog(stage);
    }

    void show() {
        choicePane.setVisible(true);
    }

    void hide() {
        choicePane.setVisible(false);
    }
}
