package kirill.subtitlesmerger.gui.tabs.videos.regular_content;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.GuiSettings;
import kirill.subtitlesmerger.gui.GuiUtils;
import kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files.TableWithFiles;
import kirill.subtitlesmerger.logic.LogicConstants;
import kirill.subtitlesmerger.logic.work_with_files.FileInfoGetter;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.toList;
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

    private Mode mode;

    private File directory;

    private List<FileInfo> allFilesInfo;

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

        updateTableWithFiles();
    }

    private void updateTableWithFiles() {
        tableWithFiles.getItems().setAll(
                getGuiFilesInfoToShow(
                        allFilesInfo,
                        mode,
                        hideUnavailableCheckbox.isSelected(),
                        guiContext.getSettings().getSortBy(),
                        guiContext.getSettings().getSortDirection()
                )
        );
    }

    private static List<GuiFileInfo> getGuiFilesInfoToShow(
            List<FileInfo> allFilesInfo,
            Mode mode,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection
    ) {
        if (CollectionUtils.isEmpty(allFilesInfo)) {
            return new ArrayList<>();
        }

        List<FileInfo> result = getSortedFilesInfo(allFilesInfo, sortBy, sortDirection);

        if (hideUnavailable) {
            result = result.stream().filter(fileInfo -> fileInfo.getUnavailabilityReason() == null).collect(toList());
        }

        return result.stream()
                .map(fileInfo -> guiFileInfoFrom(fileInfo, mode))
                .collect(toList());
    }

    private static List<FileInfo> getSortedFilesInfo(
            List<FileInfo> filesInfo,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection
    ) {
        List<FileInfo> result = new ArrayList<>(filesInfo);

        Comparator<FileInfo> comparator;
        switch (sortBy) {
            case NAME:
                comparator = Comparator.comparing(fileInfo -> fileInfo.getFile().getAbsolutePath());
                break;
            case MODIFICATION_TIME:
                comparator = Comparator.comparing(FileInfo::getLastModified);
                break;
            case SIZE:
                comparator = Comparator.comparing(FileInfo::getSize);
                break;
            default:
                throw new IllegalStateException();
        }

        if (sortDirection == GuiSettings.SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        result.sort(comparator);

        return result;
    }

    private static GuiFileInfo guiFileInfoFrom(FileInfo fileInfo, Mode mode) {
        String path;
        if (mode == Mode.FILES) {
            path = fileInfo.getFile().getAbsolutePath();
        } else if (mode == Mode.DIRECTORY) {
            path = fileInfo.getFile().getName();
        } else {
            throw new IllegalStateException();
        }

        return new GuiFileInfo(
                path,
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

        updateTableWithFiles();
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
    private void videosButtonClicked() {
        List<File> files = getFiles(stage, guiContext.getSettings());
        if (CollectionUtils.isEmpty(files)) {
            return;
        }

        try {
            guiContext.getSettings().saveLastDirectoryWithVideos(files.get(0).getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, shouldn't happen: " + getStackTrace(e));
        }

        mode = Mode.FILES;
        directory = null;

        Task<List<FileInfo>> task = new GetFilesInfoTask(files, guiContext.getFfprobe());

        task.setOnSucceeded(e -> {
            allFilesInfo = task.getValue();
            hideUnavailableCheckbox.setSelected(hideUnavailable(allFilesInfo));
            updateTableWithFiles();
            chosenDirectoryPane.setVisible(false);
            chosenDirectoryPane.setManaged(false);
            addRemoveFilesPane.setVisible(true);
            addRemoveFilesPane.setManaged(true);
            stopProgress();
        });

        task.setOnFailed(RegularContentController::taskFailed);
        task.setOnCancelled(RegularContentController::taskCancelled);

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

    /*
     * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
     * not be checked because the user will see just an empty file list which isn't user friendly.
     */
    private static boolean hideUnavailable(List<FileInfo> files) {
        return files.stream().anyMatch(fileInfo -> fileInfo.getUnavailabilityReason() == null);
    }

    private void stopProgress() {
        progressPane.setVisible(false);
        resultPane.setDisable(false);
    }

    private static void taskFailed(Event e) {
        log.error("task has failed, shouldn't happen");
        throw new IllegalStateException();
    }

    private static void taskCancelled(Event e) {
        log.error("task has been cancelled, shouldn't happen");
        throw new IllegalStateException();
    }

    private void showProgress(Task<?> task) {
        choicePane.setVisible(false);
        progressPane.setVisible(true);
        resultPane.setVisible(true);
        resultPane.setDisable(true);

        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
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

        mode = Mode.DIRECTORY;
        this.directory = directory;

        Task<List<FileInfo>> task = new GetFilesInfoTask(this.directory, guiContext.getFfprobe());

        task.setOnSucceeded(e -> {
            allFilesInfo = task.getValue();
            chosenDirectoryField.setText(directory.getAbsolutePath());
            hideUnavailableCheckbox.setSelected(hideUnavailable(allFilesInfo));
            updateTableWithFiles();
            chosenDirectoryPane.setVisible(true);
            chosenDirectoryPane.setManaged(true);
            addRemoveFilesPane.setVisible(false);
            addRemoveFilesPane.setManaged(false);
            stopProgress();
        });

        task.setOnFailed(RegularContentController::taskFailed);
        task.setOnCancelled(RegularContentController::taskCancelled);

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
        Task<List<FileInfo>> task = new GetFilesInfoTask(this.directory, guiContext.getFfprobe());

        task.setOnSucceeded(e -> {
            allFilesInfo = task.getValue();
            hideUnavailableCheckbox.setSelected(hideUnavailable(allFilesInfo));
            updateTableWithFiles();
            stopProgress();
        });

        task.setOnFailed(RegularContentController::taskFailed);
        task.setOnCancelled(RegularContentController::taskCancelled);

        showProgress(task);
        GuiUtils.startTask(task);
    }

    @FXML
    private void hideUnavailableClicked() {
        updateTableWithFiles();
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

        allFilesInfo.removeIf(fileInfo -> selectedPaths.contains(fileInfo.getFile().getAbsolutePath()));

        updateTableWithFiles();
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

        Task<List<FileInfo>> task = new GetFilesInfoTask(chosenFiles, guiContext.getFfprobe());

        task.setOnSucceeded(e -> {
            List<FileInfo> chosenFilesInfo = task.getValue();
            for (FileInfo chosenFileInfo : chosenFilesInfo) {
                boolean alreadyAdded = allFilesInfo.stream()
                        .anyMatch(fileInfo -> Objects.equals(fileInfo.getFile(), chosenFileInfo.getFile()));
                if (!alreadyAdded) {
                    allFilesInfo.add(chosenFileInfo);
                }
            }

            updateTableWithFiles();
            stopProgress();
        });

        task.setOnFailed(RegularContentController::taskFailed);
        task.setOnCancelled(RegularContentController::taskCancelled);

        showProgress(task);
        GuiUtils.startTask(task);
    }

    private enum Mode {
        FILES,
        DIRECTORY
    }

    private static class GetFilesInfoTask extends Task<List<FileInfo>> {
        private List<File> files;

        private File directory;

        private Ffprobe ffprobe;

        GetFilesInfoTask(List<File> files, Ffprobe ffprobe) {
            this.files = files;
            this.ffprobe = ffprobe;
        }

        GetFilesInfoTask(File directory, Ffprobe ffprobe) {
            this.directory = directory;
            this.ffprobe = ffprobe;
        }

        @Override
        protected List<FileInfo> call() {
            List<File> files;
            if (directory != null) {
                updateMessage("getting file list...");
                File[] directoryFiles = directory.listFiles();
                if (directoryFiles == null) {
                    log.error("failed to get directory files, directory " + directory.getAbsolutePath());
                    files = new ArrayList<>();
                } else {
                    files = Arrays.asList(directoryFiles);
                }
            } else {
                files = this.files;
            }

            return getFilesInfo(files);
        }

        private List<FileInfo> getFilesInfo(List<File> files) {
            List<FileInfo> result = new ArrayList<>();

            updateProgress(0, files.size());

            int i = 0;
            for (File file : files) {
                updateMessage("processing " + file.getName() + "...");

                if (file.isDirectory() || !file.exists()) {
                    updateProgress(i + 1, files.size());
                    i++;
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

                updateProgress(i + 1, files.size());
                i++;
            }

            return result;
        }
    }
}
