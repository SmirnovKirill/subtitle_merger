package kirill.subtitlemerger.gui.forms.videos;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.logic.settings.Settings;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;

@CommonsLog
public class ChoiceFormController {
    @FXML
    private Pane choicePane;

    private VideosFormController videosTabController;

    private TableFormController contentPaneController;

    private Stage stage;

    private GuiContext context;

    void initialize(
            VideosFormController videosTabController,
            TableFormController contentPaneController,
            Stage stage,
            GuiContext context
    ) {
        this.videosTabController = videosTabController;
        this.contentPaneController = contentPaneController;
        this.stage = stage;
        this.context = context;
    }

    public void show() {
        choicePane.setVisible(true);
    }

    public void hide() {
        choicePane.setVisible(false);
    }

    @FXML
    private void separateFilesButtonClicked() {
        List<File> files = getFiles(stage, context.getSettings());
        if (CollectionUtils.isEmpty(files)) {
            return;
        }

        if (files.size() > GuiConstants.TABLE_FILE_LIMIT) {
            GuiUtils.showErrorPopup(
                    String.format(
                            "Unfortunately, it's impossible to add more than %d files",
                            GuiConstants.TABLE_FILE_LIMIT
                    ),
                    stage
            );

            return;
        }

        videosTabController.setActivePane(VideosFormController.ActivePane.CONTENT);
        contentPaneController.handleChosenFiles(files);
    }

    private static List<File> getFiles(Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("choose videos");
        fileChooser.setInitialDirectory(settings.getVideosDirectory());
        fileChooser.getExtensionFilters().add(GuiConstants.VIDEO_EXTENSION_FILTER);

        return fileChooser.showOpenMultipleDialog(stage);
    }

    @FXML
    private void directoryButtonClicked() {
        File directory = getDirectory(stage, context.getSettings()).orElse(null);
        if (directory == null) {
            return;
        }

        videosTabController.setActivePane(VideosFormController.ActivePane.CONTENT);
        contentPaneController.handleChosenDirectory(directory);
    }

    private static Optional<File> getDirectory(Stage stage, Settings settings) {
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("choose directory with videos");
        directoryChooser.setInitialDirectory(settings.getVideosDirectory());

        return Optional.ofNullable(directoryChooser.showDialog(stage));
    }
}
