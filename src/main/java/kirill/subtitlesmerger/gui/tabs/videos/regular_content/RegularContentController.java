package kirill.subtitlesmerger.gui.tabs.videos.regular_content;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.GuiSettings;
import kirill.subtitlesmerger.gui.GuiUtils;
import kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files.TableWithFiles;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@CommonsLog
public class RegularContentController {
    private static final String SORT_BY_NAME_TEXT = "By _Name";

    private static final String SORT_BY_MODIFICATION_TIME_TEXT = "By _Modification Time";

    private static final String SORT_BY_SIZE_TEXT = "By _Size";

    private static final String SORT_ASCENDING_TEXT = "_Ascending";

    private static final String SORT_DESCENDING_TEXT = "_Descending";

    private Stage stage;

    private GuiContext guiContext;

    @FXML
    private Pane pane;

    @FXML
    private Pane choicePane;

    @FXML
    private Pane progressPane;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label progressLabel;

    @FXML
    private Pane resultPane;

    @FXML
    private Pane chosenDirectoryPane;

    @FXML
    private TextField chosenDirectoryField;

    @FXML
    private CheckBox hideUnavailableCheckbox;

    @FXML
    private TableWithFiles tableWithFiles;

    @FXML
    private Pane addRemoveFilesPane;

    @FXML
    private Button removeSelectedButton;

    private ToggleGroup sortByGroup;

    private ToggleGroup sortDirectionGroup;

    private File directory;

    private List<FileInfo> filesInfo;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.stage = stage;
        this.guiContext = guiContext;
        saveDefaultSortSettingsIfNotSet(guiContext.getSettings());
        this.sortByGroup = new ToggleGroup();
        this.sortByGroup.selectedToggleProperty().addListener(this::sortByChanged);
        this.sortDirectionGroup = new ToggleGroup();
        this.sortDirectionGroup.selectedToggleProperty().addListener(this::sortDirectionChanged);
        this.tableWithFiles.initialize();
        this.tableWithFiles.setContextMenu(
                generateContextMenu(
                        this.sortByGroup,
                        this.sortDirectionGroup,
                        guiContext.getSettings()
                )
        );
        this.removeSelectedButton.disableProperty().bind(
                Bindings.isEmpty(tableWithFiles.getSelectionModel().getSelectedIndices())
        );
    }

    private static void saveDefaultSortSettingsIfNotSet(GuiSettings settings) {
        try {
            if (settings.getSortBy() == null) {
                settings.saveSortBy(GuiSettings.SortBy.MODIFICATION_TIME.toString());
            }

            if (settings.getSortDirection() == null) {
                settings.saveSortDirection(GuiSettings.SortDirection.ASCENDING.toString());
            }
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save sort parameters, should not happen: " + ExceptionUtils.getStackTrace(e));
        }
    }

    private void sortByChanged(Observable observable) {
        RadioMenuItem radioMenuItem = (RadioMenuItem) sortByGroup.getSelectedToggle();

        try {
            switch (radioMenuItem.getText()) {
                case SORT_BY_NAME_TEXT:
                    guiContext.getSettings().saveSortBy(GuiSettings.SortBy.NAME.toString());
                    break;
                case SORT_BY_MODIFICATION_TIME_TEXT:
                    guiContext.getSettings().saveSortBy(GuiSettings.SortBy.MODIFICATION_TIME.toString());
                    break;
                case SORT_BY_SIZE_TEXT:
                    guiContext.getSettings().saveSortBy(GuiSettings.SortBy.SIZE.toString());
                    break;
                default:
                    throw new IllegalStateException();
            }
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save sort by, should not happen: " + ExceptionUtils.getStackTrace(e));
        }

        Task<GetFilesInfoTask.FilesInfo> task = new GetFilesInfoTask(
                filesInfo,
                shouldShowFullFileName(),
                hideUnavailableCheckbox.isSelected(),
                guiContext.getSettings().getSortBy(),
                guiContext.getSettings().getSortDirection()
        );

        task.setOnSucceeded(e -> {
            tableWithFiles.getItems().setAll(task.getValue().getGuiFilesInfo());
            stopProgress();
        });

        showProgress(task);
        GuiUtils.startTask(task);
    }

    private boolean shouldShowFullFileName() {
        return directory == null;
    }

    private void stopProgress() {
        progressPane.setVisible(false);
        resultPane.setDisable(false);
    }

    private void showProgress(Task<?> task) {
        choicePane.setVisible(false);
        progressPane.setVisible(true);
        resultPane.setVisible(true);
        resultPane.setDisable(true);

        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
    }

    private void sortDirectionChanged(Observable observable) {
        RadioMenuItem radioMenuItem = (RadioMenuItem) sortDirectionGroup.getSelectedToggle();

        try {
            switch (radioMenuItem.getText()) {
                case SORT_ASCENDING_TEXT:
                    guiContext.getSettings().saveSortDirection(GuiSettings.SortDirection.ASCENDING.toString());
                    break;
                case SORT_DESCENDING_TEXT:
                    guiContext.getSettings().saveSortDirection(GuiSettings.SortDirection.DESCENDING.toString());
                    break;
                default:
                    throw new IllegalStateException();
            }
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save sort direction, should not happen: " + ExceptionUtils.getStackTrace(e));
        }

        Task<GetFilesInfoTask.FilesInfo> task = new GetFilesInfoTask(
                filesInfo,
                shouldShowFullFileName(),
                hideUnavailableCheckbox.isSelected(),
                guiContext.getSettings().getSortBy(),
                guiContext.getSettings().getSortDirection()
        );

        task.setOnSucceeded(e -> {
            tableWithFiles.getItems().setAll(task.getValue().getGuiFilesInfo());
            stopProgress();
        });

        showProgress(task);
        GuiUtils.startTask(task);
    }

    private static ContextMenu generateContextMenu(
            ToggleGroup sortByGroup,
            ToggleGroup sortDirectionGroup,
            GuiSettings settings
    ) {
        ContextMenu result = new ContextMenu();

        Menu menu = new Menu("_Sort files");

        GuiSettings.SortBy sortBy = settings.getSortBy();
        GuiSettings.SortDirection sortDirection = settings.getSortDirection();

        RadioMenuItem byName = new RadioMenuItem(SORT_BY_NAME_TEXT);
        byName.setToggleGroup(sortByGroup);
        if (sortBy == GuiSettings.SortBy.NAME) {
            byName.setSelected(true);
        }

        RadioMenuItem byModificationTime = new RadioMenuItem(SORT_BY_MODIFICATION_TIME_TEXT);
        byModificationTime.setToggleGroup(sortByGroup);
        if (sortBy == GuiSettings.SortBy.MODIFICATION_TIME) {
            byModificationTime.setSelected(true);
        }

        RadioMenuItem bySize = new RadioMenuItem(SORT_BY_SIZE_TEXT);
        bySize.setToggleGroup(sortByGroup);
        if (sortBy == GuiSettings.SortBy.SIZE) {
            bySize.setSelected(true);
        }

        RadioMenuItem ascending = new RadioMenuItem(SORT_ASCENDING_TEXT);
        ascending.setToggleGroup(sortDirectionGroup);
        if (sortDirection == GuiSettings.SortDirection.ASCENDING) {
            ascending.setSelected(true);
        }

        RadioMenuItem descending = new RadioMenuItem(SORT_DESCENDING_TEXT);
        descending.setToggleGroup(sortDirectionGroup);
        if (sortDirection == GuiSettings.SortDirection.DESCENDING) {
            descending.setSelected(true);
        }

        menu.getItems().addAll(
                byName,
                byModificationTime,
                bySize,
                new SeparatorMenuItem(),
                ascending,
                descending
        );

        result.getItems().add(menu);

        return result;
    }

    public void show() {
        pane.setVisible(true);
    }

    public void hide() {
        pane.setVisible(false);
    }

    @FXML
    private void separateFilesButtonClicked() {
        List<File> files = getFiles(stage, guiContext.getSettings());
        if (CollectionUtils.isEmpty(files)) {
            return;
        }

        try {
            guiContext.getSettings().saveLastDirectoryWithVideos(files.get(0).getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, shouldn't happen: " + getStackTrace(e));
        }

        directory = null;

        Task<GetFilesInfoTask.FilesInfo> task = new GetFilesInfoTask(
                files,
                guiContext.getSettings().getSortBy(),
                guiContext.getSettings().getSortDirection(),
                guiContext.getFfprobe()
        );

        task.setOnSucceeded(e -> {
            filesInfo = task.getValue().getFilesInfo();
            hideUnavailableCheckbox.setSelected(task.getValue().isHideUnavailable());
            tableWithFiles.getItems().setAll(task.getValue().getGuiFilesInfo());

            chosenDirectoryPane.setVisible(false);
            chosenDirectoryPane.setManaged(false);
            addRemoveFilesPane.setVisible(true);
            addRemoveFilesPane.setManaged(true);

            stopProgress();
        });

        showProgress(task);
        GuiUtils.startTask(task);
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

    @FXML
    private void directoryButtonClicked() {
        File directory = getDirectory(stage, guiContext.getSettings()).orElse(null);
        if (directory == null) {
            return;
        }

        try {
            guiContext.getSettings().saveLastDirectoryWithVideos(directory.getAbsolutePath());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        this.directory = directory;

        Task<GetFilesInfoTask.FilesInfo> task = new GetFilesInfoTask(
                this.directory,
                guiContext.getSettings().getSortBy(),
                guiContext.getSettings().getSortDirection(),
                guiContext.getFfprobe()
        );

        task.setOnSucceeded(e -> {
            filesInfo = task.getValue().getFilesInfo();
            hideUnavailableCheckbox.setSelected(task.getValue().isHideUnavailable());
            tableWithFiles.getItems().setAll(task.getValue().getGuiFilesInfo());
            chosenDirectoryField.setText(directory.getAbsolutePath());

            chosenDirectoryPane.setVisible(true);
            chosenDirectoryPane.setManaged(true);
            addRemoveFilesPane.setVisible(false);
            addRemoveFilesPane.setManaged(false);

            stopProgress();
        });

        showProgress(task);
        GuiUtils.startTask(task);
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
        Task<GetFilesInfoTask.FilesInfo> task = new GetFilesInfoTask(
                this.directory,
                guiContext.getSettings().getSortBy(),
                guiContext.getSettings().getSortDirection(),
                guiContext.getFfprobe()
        );

        task.setOnSucceeded(e -> {
            filesInfo = task.getValue().getFilesInfo();
            hideUnavailableCheckbox.setSelected(task.getValue().isHideUnavailable());
            tableWithFiles.getItems().setAll(task.getValue().getGuiFilesInfo());
            stopProgress();
        });

        showProgress(task);
        GuiUtils.startTask(task);
    }

    @FXML
    private void hideUnavailableClicked() {
        Task<GetFilesInfoTask.FilesInfo> task = new GetFilesInfoTask(
                filesInfo,
                shouldShowFullFileName(),
                hideUnavailableCheckbox.isSelected(),
                guiContext.getSettings().getSortBy(),
                guiContext.getSettings().getSortDirection()
        );

        task.setOnSucceeded(e -> {
            tableWithFiles.getItems().setAll(task.getValue().getGuiFilesInfo());
            stopProgress();
        });

        showProgress(task);
        GuiUtils.startTask(task);
    }

    @FXML
    private void removeButtonClicked() {
        List<Integer> indices = tableWithFiles.getSelectionModel().getSelectedIndices();
        if (CollectionUtils.isEmpty(indices)) {
            return;
        }

        List<String> selectedPaths = new ArrayList<>();
        for (int index : indices) {
            selectedPaths.add(tableWithFiles.getItems().get(index).getPath());
        }

        filesInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getFile().getAbsolutePath()));

        Task<GetFilesInfoTask.FilesInfo> task = new GetFilesInfoTask(
                filesInfo,
                shouldShowFullFileName(),
                hideUnavailableCheckbox.isSelected(),
                guiContext.getSettings().getSortBy(),
                guiContext.getSettings().getSortDirection()
        );

        task.setOnSucceeded(e -> {
            tableWithFiles.getItems().setAll(task.getValue().getGuiFilesInfo());
            stopProgress();
        });

        showProgress(task);
        GuiUtils.startTask(task);
    }

    @FXML
    private void addButtonClicked() {
        List<File> chosenFiles = getFiles(stage, guiContext.getSettings());
        if (CollectionUtils.isEmpty(chosenFiles)) {
            return;
        }

        try {
            guiContext.getSettings().saveLastDirectoryWithVideos(chosenFiles.get(0).getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        Task<GetFilesInfoTask.FilesInfo> task = new GetFilesInfoTask(
                filesInfo,
                chosenFiles,
                hideUnavailableCheckbox.isSelected(),
                guiContext.getSettings().getSortBy(),
                guiContext.getSettings().getSortDirection(),
                guiContext.getFfprobe()
        );

        task.setOnSucceeded(e -> {
            filesInfo = task.getValue().getFilesInfo();
            tableWithFiles.getItems().setAll(task.getValue().getGuiFilesInfo());
            stopProgress();
        });

        showProgress(task);
        GuiUtils.startTask(task);
    }
}
