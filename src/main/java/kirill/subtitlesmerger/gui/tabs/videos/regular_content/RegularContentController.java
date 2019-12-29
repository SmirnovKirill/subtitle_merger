package kirill.subtitlesmerger.gui.tabs.videos.regular_content;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.GuiSettings;
import kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files.TableWithFiles;
import kirill.subtitlesmerger.logic.LogicConstants;
import kirill.subtitlesmerger.logic.work_with_files.FileInfoGetter;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@CommonsLog
public class RegularContentController {
    private Stage stage;

    private GuiContext guiContext;

    @FXML
    private Pane pane;

    @FXML
    private Pane choicePane;

    @FXML
    private Pane resultPane;

    @FXML
    private Pane chosenDirectoryPane;

    @FXML
    private TextField chosenDirectoryField;

    @FXML
    private TableWithFiles tableWithFiles;

    private Mode mode;

    private File directory;

    private List<File> files;

    private BooleanProperty hideUnavailable;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.stage = stage;
        this.guiContext = guiContext;
        this.hideUnavailable = new SimpleBooleanProperty(this, "hideUnavailable", true);
        this.tableWithFiles.initialize();
    }

    public void show() {
        pane.setVisible(true);
    }

    public void hide() {
        pane.setVisible(false);
    }

    @FXML
    private void videosButtonClicked() {
        List<File> files = getFiles(stage, guiContext.getSettings());
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        this.files = files;

        try {
            guiContext.getSettings().saveLastDirectoryWithVideos(files.get(0).getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        mode = Mode.FILES;
        directory = null;
        //todo in background + progress
        List<GuiFileInfo> guiFilesInfo = getGuiFilesInfo(files, guiContext.getFfprobe());
        tableWithFiles.getItems().setAll(guiFilesInfo);
        hideUnavailable.setValue(hideUnavailable(guiFilesInfo));

        choicePane.setVisible(false);
        resultPane.setVisible(true);
        chosenDirectoryPane.setVisible(false);
        chosenDirectoryPane.setManaged(false);
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

    private static List<GuiFileInfo> getGuiFilesInfo(List<File> files, Ffprobe ffprobe) {
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

        return guiFilesInfoFrom(result);
    }

    private static List<GuiFileInfo> guiFilesInfoFrom(List<FileInfo> filesInfo) {
        return filesInfo.stream().map(RegularContentController::guiFileInfoFrom).collect(toList());
    }

    private static GuiFileInfo guiFileInfoFrom(FileInfo fileInfo) {
        return new GuiFileInfo(
                fileInfo.getFile().getAbsolutePath(),
                fileInfo.getLastModified(),
                LocalDateTime.now(),
                fileInfo.getSize(),
                convert(fileInfo.getUnavailabilityReason()),
                ""
        );
    }

    private static String convert(FileInfo.UnavailabilityReason unavailabilityReason) {
        if (unavailabilityReason == null) {
            return "";
        }

        return unavailabilityReason.toString(); //todo
    }

    /*
     * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
     * not be checked because the user will see just an empty file list which isn't user friendly.
     */
    private static boolean hideUnavailable(List<GuiFileInfo> files) {
        return files.stream().anyMatch(fileInfo -> StringUtils.isBlank(fileInfo.getUnavailabilityReason()));
    }

    @FXML
    private void directoryButtonClicked() {
        File directory = getDirectory(stage, guiContext.getSettings()).orElse(null);
        if (directory == null) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            log.error("failed to get directory files, directory " + directory.getAbsolutePath());
            files = new File[]{};
        }

        try {
            guiContext.getSettings().saveLastDirectoryWithVideos(directory.getAbsolutePath());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        mode = Mode.DIRECTORY;
        this.directory = directory;
        List<GuiFileInfo> guiFilesInfo = getGuiFilesInfo(Arrays.asList(files), guiContext.getFfprobe());
        //todo in background + progress
        chosenDirectoryField.setText(directory.getAbsolutePath());
        tableWithFiles.getItems().setAll(guiFilesInfo);
        hideUnavailable.setValue(hideUnavailable(guiFilesInfo));
        choicePane.setVisible(false);
        resultPane.setVisible(true);
        chosenDirectoryPane.setVisible(true);
        chosenDirectoryPane.setManaged(true);
    }

    private static Optional<File> getDirectory(Stage stage, GuiSettings settings) {
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("choose directory with videos");
        directoryChooser.setInitialDirectory(settings.getLastDirectoryWithVideos());

        return Optional.ofNullable(directoryChooser.showDialog(stage));
    }

    @FXML
    private void backToSelectionClicked() {
        choicePane.setVisible(true);
        resultPane.setVisible(false);
    }

    @FXML
    private void refreshButtonClicked() {
        File[] files = directory.listFiles();
        if (files == null) {
            log.error("failed to get directory files, directory " + directory.getAbsolutePath());
            files = new File[]{};
        }

        List<GuiFileInfo> guiFilesInfo = getGuiFilesInfo(Arrays.asList(files), guiContext.getFfprobe());
        //todo in background + progress
        tableWithFiles.getItems().setAll(guiFilesInfo);
        hideUnavailable.setValue(hideUnavailable(guiFilesInfo));
    }

    private enum Mode {
        FILES,
        DIRECTORY
    }
}
