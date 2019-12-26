package kirill.subtitlesmerger.gui.tabs.videos.regular_content;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiConstants;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.GuiSettings;
import kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files.TableWithFiles;
import kirill.subtitlesmerger.logic.LogicConstants;
import kirill.subtitlesmerger.logic.work_with_files.FileInfoGetter;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@CommonsLog
public class RegularContentController {
    private Stage stage;

    private GuiContext guiContext;

    @FXML
    private Pane pane;

    @FXML
    private TitledPane chooseTitledPane;

    @FXML
    private Label resultLabel;

    @FXML
    private TableWithFiles tableWithFiles;

    private Mode mode;

    private File directory;

    private List<FileInfo> filesInfo;

    private BooleanProperty hideUnavailable;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.stage = stage;
        this.guiContext = guiContext;
        this.hideUnavailable = new SimpleBooleanProperty(this, "hideUnavailable", true);
    }

    public void show() {
        pane.setVisible(true);
    }

    public void hide() {
        pane.setVisible(false);
    }

    @FXML
    private void videosButtonClicked() {
        clearResult();

        List<File> files = getFiles(stage, guiContext.getSettings());
        if (CollectionUtils.isEmpty(files)) {
            return;
        }

        try {
            guiContext.getSettings().saveLastDirectoryWithVideos(files.get(0).getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        mode = Mode.FILES;
        directory = null;
        //todo in background + progress
        filesInfo = getFilesInfo(files, guiContext.getFfprobe());
        hideUnavailable.setValue(hideUnavailable(filesInfo));
        tableWithFiles.setVisible(true);
        chooseTitledPane.setExpanded(false);
    }

    private void clearResult() {
        resultLabel.setText("");
        resultLabel.getStyleClass().removeAll(GuiConstants.LABEL_SUCCESS_CLASS, GuiConstants.LABEL_ERROR_CLASS);
    }

    private static List<File> getFiles(Stage stage, GuiSettings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("choose videos");
        fileChooser.setInitialDirectory(settings.getLastDirectoryWithVideos());
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("mkv files (*.mkv)", "*.mkv")
        );

        return fileChooser.showOpenMultipleDialog(stage);
    }

    private static List<FileInfo> getFilesInfo(List<File> files, Ffprobe ffprobe) {
        List<FileInfo> result = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory() || !file.exists()) {
                continue;
            }

            result.add(
                    FileInfoGetter.getFileInfoWithoutSubtitles(
                            file,
                            LogicConstants.ALLOWED_VIDEO_EXTENSIONS,
                            LogicConstants.ALLOWED_VIDEO_MIME_TYPES,
                            ffprobe
                    )
            );
        }

        return result;
    }

    /*
     * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
     * not be checked because the user will see just an empty file list which isn't user friendly.
     */
    private static boolean hideUnavailable(List<FileInfo> filesInfo) {
        return filesInfo.stream().anyMatch(fileInfo -> fileInfo.getUnavailabilityReason() == null);
    }

    @FXML
    private void directoryButtonClicked() {
        clearResult();

        File directory = getDirectory(stage, guiContext.getSettings()).orElse(null);
        if (directory == null) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            log.error("failed to get directory files, directory " + directory.getAbsolutePath());
            return;
        }

        try {
            guiContext.getSettings().saveLastDirectoryWithVideos(directory.getAbsolutePath());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        mode = Mode.DIRECTORY;
        this.directory = directory;
        filesInfo = getFilesInfo(Arrays.asList(files), guiContext.getFfprobe());

        if (CollectionUtils.isEmpty(filesInfo)) {
            tableWithFiles.setVisible(false);
            chooseTitledPane.setExpanded(true);
            showErrorMessage("directory is empty, please choose another one");
            return;
        }

        //todo in background + progress
        hideUnavailable.setValue(hideUnavailable(filesInfo));
        tableWithFiles.setVisible(true);
        chooseTitledPane.setExpanded(false);
    }

    private static Optional<File> getDirectory(Stage stage, GuiSettings settings) {
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("choose directory with videos");
        directoryChooser.setInitialDirectory(settings.getLastDirectoryWithVideos());

        return Optional.ofNullable(directoryChooser.showDialog(stage));
    }

    private void showErrorMessage(String text) {
        resultLabel.setText(text);

        resultLabel.getStyleClass().remove(GuiConstants.LABEL_SUCCESS_CLASS);
        if (!resultLabel.getStyleClass().contains(GuiConstants.LABEL_ERROR_CLASS)) {
            resultLabel.getStyleClass().add(GuiConstants.LABEL_ERROR_CLASS);
        }
    }

    private enum Mode {
        FILES,
        DIRECTORY
    }
}
