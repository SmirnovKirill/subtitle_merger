package kirill.subtitlemerger.gui.tabs.videos.regular_content;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.GuiUtils;
import kirill.subtitlemerger.gui.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.*;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.LoadFilesAllSubtitlesTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.LoadSingleFileAllSubtitlesTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.LoadSingleSubtitleTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.LoadSubtitlesTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiExternalSubtitleFile;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStream;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.TableWithFiles;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.work_with_files.SubtitleInjector;
import kirill.subtitlemerger.logic.work_with_files.entities.ExternalSubtitleFile;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@CommonsLog
public class RegularContentController {
    private static final String SORT_BY_NAME_TEXT = "By _Name";

    private static final String SORT_BY_MODIFICATION_TIME_TEXT = "By _Modification Time";

    private static final String SORT_BY_SIZE_TEXT = "By _Size";

    private static final String SORT_ASCENDING_TEXT = "_Ascending";

    private static final String SORT_DESCENDING_TEXT = "_Descending";

    private Stage stage;

    private GuiContext context;

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
    private Label resultLabelSuccess;

    @FXML
    private Label resultLabelWarn;

    @FXML
    private Label resultLabelError;

    @FXML
    private Label selectedForMergeLabel;

    @FXML
    private CheckBox hideUnavailableCheckbox;

    @FXML
    private Pane autoSelectButtonWrapper;

    @FXML
    private Button autoSelectButton;

    @FXML
    private Pane getAllSizesButtonWrapper;

    @FXML
    private Button getAllSizesButton;

    @FXML
    private Pane goButtonWrapper;

    @FXML
    private Button goButton;

    @FXML
    private TableWithFiles tableWithFiles;

    @FXML
    private Pane addRemoveFilesPane;

    @FXML
    private Button removeSelectedButton;

    //todo message property probably
    private BooleanProperty cancelTaskPaneVisible = new SimpleBooleanProperty(false);

    private ToggleGroup sortByGroup;

    private ToggleGroup sortDirectionGroup;

    private BackgroundTask<?> currentTask;

    private File directory;

    private List<FileInfo> filesInfo;

    private List<GuiFileInfo> allGuiFilesInfo;

    private Map<String, FilePanes> filePanes;

    private BooleanProperty allSelected;

    private LongProperty selected;

    private IntegerProperty allAvailableCount;

    //todo comment
    private GuiFileInfo lastProcessedFileInfo;

    public boolean getCancelTaskPaneVisible() {
        return cancelTaskPaneVisible.get();
    }

    public BooleanProperty cancelTaskPaneVisibleProperty() {
        return cancelTaskPaneVisible;
    }

    public void setCancelTaskPaneVisible(boolean cancelTaskPaneVisible) {
        this.cancelTaskPaneVisible.set(cancelTaskPaneVisible);
    }

    public void initialize(Stage stage, GuiContext guiContext) {
        this.stage = stage;
        this.context = guiContext;
        saveDefaultSortSettingsIfNotSet(guiContext.getSettings());
        this.sortByGroup = new ToggleGroup();
        this.sortDirectionGroup = new ToggleGroup();
        this.allSelected = new SimpleBooleanProperty(false);
        this.selected = new SimpleLongProperty(0);
        this.allAvailableCount = new SimpleIntegerProperty(0);
        this.tableWithFiles.initialize(allSelected, selected, allAvailableCount);
        this.tableWithFiles.setContextMenu(
                generateContextMenu(
                        this.sortByGroup,
                        this.sortDirectionGroup,
                        guiContext.getSettings()
                )
        );
        this.selectedProperty().addListener(this::selectedCountChangeListener);

        this.sortByGroup.selectedToggleProperty().addListener(this::sortByChanged);
        this.sortDirectionGroup.selectedToggleProperty().addListener(this::sortDirectionChanged);
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

    private void selectedCountChangeListener(Observable observable) {
        setActionButtonsVisibility();

        String suffix = "selected for merge";
        if (getSelected() == 1) {
            selectedForMergeLabel.setText("1 video " + suffix);
        } else {
            selectedForMergeLabel.setText(getSelected() + " videos " + suffix);
        }
    }

    private void setActionButtonsVisibility() {
        if (getSelected() == 0) {
            String tooltipText = "no videos are selected for merge";

            autoSelectButton.setDisable(true);
            Tooltip.install(autoSelectButtonWrapper, GuiUtils.generateTooltip(tooltipText));

            getAllSizesButton.setDisable(true);
            Tooltip.install(getAllSizesButtonWrapper, GuiUtils.generateTooltip(tooltipText));

            goButton.setDisable(true);
            Tooltip.install(goButtonWrapper, GuiUtils.generateTooltip(tooltipText));
        } else {
            autoSelectButton.setDisable(false);
            Tooltip.install(autoSelectButtonWrapper, null);

            getAllSizesButton.setDisable(false);
            Tooltip.install(getAllSizesButtonWrapper, null);

            goButton.setDisable(false);
            Tooltip.install(goButtonWrapper, null);
        }
    }

    @FXML
    private void autoSelectButtonClicked() {
        clearGeneralResult();
        lastProcessedFileInfo = null;

        AutoSelectSubtitlesTask task = new AutoSelectSubtitlesTask(
                filesInfo,
                tableWithFiles.getItems(),
                context.getFfmpeg(),
                context.getSettings(),
                result -> {
                    showResult(result);
                    stopProgress();
                }
        );

        startBackgroundTask(task);
    }

    private void startBackgroundTask(BackgroundTask<?> task) {
        currentTask = task;
        showProgress(task);
        cancelTaskPaneVisible.bind(task.cancellationPossibleProperty());
        task.start();
    }

    //todo refactor somehow
    private void showResult(AutoSelectSubtitlesTask.Result result) {
        if (result.getProcessedCount() == 0) {
            setResult(null, "Haven't done anything because of the cancellation", null);
        } else if (result.getFinishedSuccessfullyCount() == result.getAllFileCount()) {
            if (result.getAllFileCount() == 1) {
                setResult("Subtitles have been successfully auto-selected for the file", null, null);
            } else {
                setResult("Subtitles have been successfully auto-selected for all " + result.getAllFileCount() + " files", null, null);
            }
        } else if (result.getNotEnoughStreamsCount() == result.getAllFileCount()) {
            if (result.getAllFileCount() == 1) {
                setResult(null, "Auto-selection is not possible (no proper subtitles to choose from) for the file", null);
            } else {
                setResult(null, "Auto-selection is not possible (no proper subtitles to choose from) for all " + result.getAllFileCount() + " files", null);
            }
        } else if (result.getFailedCount() == result.getAllFileCount()) {
            if (result.getAllFileCount() == 1) {
                setResult(null, null, "Failed to perform auto-selection for the file");
            } else {
                setResult(null, null, "Failed to perform auto-selection for all " + result.getAllFileCount() + " files");
            }
        } else {
            String success = "";
            if (result.getFinishedSuccessfullyCount() != 0) {
                success += result.getFinishedSuccessfullyCount() + "/" + result.getAllFileCount();
                success += " auto-selected successfully";
            }

            String warn = "";
            if (result.getProcessedCount() != result.getAllFileCount()) {
                warn += (result.getAllFileCount() - result.getProcessedCount()) + "/" + result.getAllFileCount();
                warn += " cancelled";
            }

            if (result.getNotEnoughStreamsCount() != 0) {
                if (!StringUtils.isBlank(warn)) {
                    warn += ", ";
                }

                warn += "auto-selection is not possible for ";
                warn += result.getNotEnoughStreamsCount() + "/" + result.getAllFileCount();
            }

            String error = "";
            if (result.getFailedCount() != 0) {
                error += result.getFailedCount() + "/" + result.getAllFileCount();
                error += " failed";
            }

            setResult(success, warn, error);
        }
    }

    @FXML
    private void getAllSizesButtonClicked() {
        clearGeneralResult();
        lastProcessedFileInfo = null;

        LoadFilesAllSubtitlesTask task = new LoadFilesAllSubtitlesTask(
                filesInfo,
                tableWithFiles.getItems(),
                context.getFfmpeg(),
                result -> {
                    showResult(result);
                    stopProgress();
                }
        );

        startBackgroundTask(task);
    }

    private void showResult(LoadSubtitlesTask.Result result) {
        if (result.getAllSubtitleCount() == 0) {
            setResult("No subtitles to load", null, null);
        } else if (result.getProcessedCount() == 0) {
            setResult(null, "Haven't load anything because of the cancellation", null);
        } else if (result.getLoadedSuccessfullyCount() == result.getAllSubtitleCount()) {
            if (result.getAllSubtitleCount() == 1) {
                setResult("Subtitle size has been loaded successfully", null, null);
            } else {
                setResult("All " + result.getAllSubtitleCount() + " subtitle sizes have been loaded successfully", null, null);
            }
        } else if (result.getLoadedBeforeCount() == result.getAllSubtitleCount()) {
            if (result.getAllSubtitleCount() == 1) {
                setResult("Subtitle size has already been loaded successfully", null, null);
            } else {
                setResult("All " + result.getAllSubtitleCount() + " subtitle sizes have already been loaded successfully", null, null);
            }
        } else if (result.getFailedToLoadCount() == result.getAllSubtitleCount()) {
            if (result.getAllSubtitleCount() == 1) {
                setResult(null, null, "Failed to load subtitle size");
            } else {
                setResult(null, null, "Failed to load all " + result.getAllSubtitleCount() + " subtitle sizes");
            }
        } else {
            String success = "";
            if (result.getLoadedSuccessfullyCount() != 0) {
                success += result.getLoadedSuccessfullyCount() + "/" + result.getAllSubtitleCount();
                success += " loaded successfully";
            }

            if (result.getLoadedBeforeCount() != 0) {
                if (!StringUtils.isBlank(success)) {
                    success += ", ";
                }

                success += result.getLoadedBeforeCount() + "/" + result.getAllSubtitleCount();
                success += " loaded before";
            }

            String warn = "";
            if (result.getProcessedCount() != result.getAllSubtitleCount()) {
                warn += (result.getAllSubtitleCount() - result.getProcessedCount()) + "/" + result.getAllSubtitleCount();
                warn += " cancelled";
            }

            String error = "";
            if (result.getFailedToLoadCount() != 0) {
                error += "failed to load " + result.getFailedToLoadCount() + "/" + result.getAllSubtitleCount();
                error += " subtitles";
            }

            setResult(success, warn, error);
        }
    }

    @FXML
    private void goButtonClicked() throws FfmpegException {
        GuiFileInfo guiFileInfo = tableWithFiles.getItems().get(0);
        FileInfo fileInfo = findMatchingFileInfo(guiFileInfo, filesInfo);

        GuiSubtitleStream guiUpperSubtitles = guiFileInfo.getSubtitleStreams().stream()
                .filter(GuiSubtitleStream::isSelectedAsUpper)
                .findFirst().orElseThrow(IllegalStateException::new);
        SubtitleStream upperSubtitles = SubtitleStream.getByFfmpegIndex(
                guiUpperSubtitles.getFfmpegIndex(),
                fileInfo.getSubtitleStreams()
        );

        GuiSubtitleStream guiLowerSubtitles = guiFileInfo.getSubtitleStreams().stream()
                .filter(GuiSubtitleStream::isSelectedAsLower)
                .findFirst().orElseThrow(IllegalStateException::new);
        SubtitleStream lowerSubtitles = SubtitleStream.getByFfmpegIndex(
                guiLowerSubtitles.getFfmpegIndex(),
                fileInfo.getSubtitleStreams()
        );

        SubtitleInjector.mergeAndInjectSubtitlesToFile(
                upperSubtitles.getSubtitles(),
                lowerSubtitles.getSubtitles(),
                context.getSettings().isMarkMergedStreamAsDefault(),
                fileInfo,
                context.getFfmpeg()
        );
    }

    private void sortByChanged(Observable observable) {
        clearGeneralResult();
        clearLastProcessedResult();

        RadioMenuItem radioMenuItem = (RadioMenuItem) sortByGroup.getSelectedToggle();

        try {
            switch (radioMenuItem.getText()) {
                case SORT_BY_NAME_TEXT:
                    context.getSettings().saveSortBy(GuiSettings.SortBy.NAME.toString());
                    break;
                case SORT_BY_MODIFICATION_TIME_TEXT:
                    context.getSettings().saveSortBy(GuiSettings.SortBy.MODIFICATION_TIME.toString());
                    break;
                case SORT_BY_SIZE_TEXT:
                    context.getSettings().saveSortBy(GuiSettings.SortBy.SIZE.toString());
                    break;
                default:
                    throw new IllegalStateException();
            }
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save sort by, should not happen: " + ExceptionUtils.getStackTrace(e));
        }

        SortOrShowHideUnavailableTask task = new SortOrShowHideUnavailableTask(
                allGuiFilesInfo,
                hideUnavailableCheckbox.isSelected(),
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                result -> {
                    updateTableContent(result, filePanes);
                    stopProgress();
                }
        );

        startBackgroundTask(task);
    }

    private void clearGeneralResult() {
        resultLabelSuccess.setText("");
        resultLabelWarn.setText("");
        resultLabelError.setText("");
    }

    private void clearLastProcessedResult() {
        if (lastProcessedFileInfo != null) {
            lastProcessedFileInfo.clearResult();
        }
    }

    private void updateTableContent(List<GuiFileInfo> guiFilesToShowInfo, Map<String, FilePanes> filePanes) {
        long oldSelected = getSelected();
        setSelected(guiFilesToShowInfo.stream().filter(GuiFileInfo::isSelected).count());
        if (oldSelected == getSelected()) {
            setActionButtonsVisibility();
        }

        allAvailableCount.setValue(
                (int) guiFilesToShowInfo.stream()
                        .filter(filesInfo -> StringUtils.isBlank(filesInfo.getUnavailabilityReason()))
                        .count()
        );

        tableWithFiles.getFilePanes().clear();
        tableWithFiles.getFilePanes().putAll(filePanes);
        tableWithFiles.setItems(FXCollections.observableArrayList(guiFilesToShowInfo));
        setAllSelected(getSelected() == allAvailableCount.get());
    }

    private void stopProgress() {
        progressPane.setVisible(false);
        resultPane.setDisable(false);
    }

    private void showProgress(BackgroundTask<?> task) {
        choicePane.setVisible(false);
        progressPane.setVisible(true);
        resultPane.setVisible(true);
        resultPane.setDisable(true);

        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
    }

    private void sortDirectionChanged(Observable observable) {
        clearGeneralResult();
        clearLastProcessedResult();

        RadioMenuItem radioMenuItem = (RadioMenuItem) sortDirectionGroup.getSelectedToggle();

        try {
            switch (radioMenuItem.getText()) {
                case SORT_ASCENDING_TEXT:
                    context.getSettings().saveSortDirection(GuiSettings.SortDirection.ASCENDING.toString());
                    break;
                case SORT_DESCENDING_TEXT:
                    context.getSettings().saveSortDirection(GuiSettings.SortDirection.DESCENDING.toString());
                    break;
                default:
                    throw new IllegalStateException();
            }
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save sort direction, should not happen: " + ExceptionUtils.getStackTrace(e));
        }

        SortOrShowHideUnavailableTask task = new SortOrShowHideUnavailableTask(
                allGuiFilesInfo,
                hideUnavailableCheckbox.isSelected(),
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                result -> {
                    updateTableContent(result, filePanes);
                    stopProgress();
                }
        );


        startBackgroundTask(task);
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

    @FXML
    private void cancelTaskClicked() {
        if (currentTask == null) {
            log.error("task is null, that shouldn't happen");
            return;
        }

        currentTask.cancel();
    }

    public void show() {
        pane.setVisible(true);
    }

    public void hide() {
        pane.setVisible(false);
    }

    void setResult(String success, String warn, String error) {
        if (!StringUtils.isBlank(success) && (!StringUtils.isBlank(warn) || !StringUtils.isBlank(error))) {
            resultLabelSuccess.setText(success + ", ");
        } else if (!StringUtils.isBlank(success)) {
            resultLabelSuccess.setText(success);
        } else {
            resultLabelSuccess.setText("");
        }

        if (!StringUtils.isBlank(warn) && !StringUtils.isBlank(error)) {
            resultLabelWarn.setText(warn + ", ");
        } else if (!StringUtils.isBlank(warn)) {
            resultLabelWarn.setText(warn);
        } else {
            resultLabelWarn.setText("");
        }

        if (!StringUtils.isBlank(error)) {
            resultLabelError.setText(error);
        } else {
            resultLabelError.setText("");
        }
    }

    @FXML
    private void separateFilesButtonClicked() {
        //todo check if > 10000

        List<File> files = getFiles(stage, context.getSettings());
        if (CollectionUtils.isEmpty(files)) {
            return;
        }

        context.setWorkWithVideosInProgress(true);

        try {
            context.getSettings().saveLastDirectoryWithVideos(files.get(0).getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, shouldn't happen: " + getStackTrace(e));
        }

        directory = null;

        LoadSeparateFilesTask task = new LoadSeparateFilesTask(
                files,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context,
                selected,
                allSelected,
                allAvailableCount,
                this::loadAllFileSubtitleSizes,
                this::loadSingleFileSubtitleSize,
                this::addExternalSubtitleFileClicked,
                this::removeExternalSubtitleFileClicked,
                result -> {
                    filesInfo = result.getFilesInfo();
                    allGuiFilesInfo = result.getAllGuiFilesInfo();
                    filePanes = result.getFilePanes();
                    updateTableContent(result.getGuiFilesToShowInfo(), result.getFilePanes());
                    hideUnavailableCheckbox.setSelected(result.isHideUnavailable());

                    chosenDirectoryPane.setVisible(false);
                    chosenDirectoryPane.setManaged(false);
                    addRemoveFilesPane.setVisible(true);
                    addRemoveFilesPane.setManaged(true);

                    stopProgress();
                }
        );

        startBackgroundTask(task);
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
        //todo check if > 10000

        File directory = getDirectory(stage, context.getSettings()).orElse(null);
        if (directory == null) {
            return;
        }

        context.setWorkWithVideosInProgress(true);

        try {
            context.getSettings().saveLastDirectoryWithVideos(directory.getAbsolutePath());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        this.directory = directory;

        LoadDirectoryFilesTask task = new LoadDirectoryFilesTask(
                this.directory,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context,
                selected,
                allSelected,
                allAvailableCount,
                this::loadAllFileSubtitleSizes,
                this::loadSingleFileSubtitleSize,
                this::addExternalSubtitleFileClicked,
                this::removeExternalSubtitleFileClicked,
                result -> {
                    filesInfo = result.getFilesInfo();
                    allGuiFilesInfo = result.getAllGuiFilesInfo();
                    filePanes = result.getFilePanes();
                    updateTableContent(result.getGuiFilesToShowInfo(), result.getFilePanes());
                    hideUnavailableCheckbox.setSelected(result.isHideUnavailable());
                    chosenDirectoryField.setText(directory.getAbsolutePath());

                    chosenDirectoryPane.setVisible(true);
                    chosenDirectoryPane.setManaged(true);
                    addRemoveFilesPane.setVisible(false);
                    addRemoveFilesPane.setManaged(false);

                    stopProgress();
                }
        );

        startBackgroundTask(task);
    }

    private static Optional<File> getDirectory(Stage stage, GuiSettings settings) {
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("choose directory with videos");
        directoryChooser.setInitialDirectory(settings.getLastDirectoryWithVideos());

        return Optional.ofNullable(directoryChooser.showDialog(stage));
    }

    @FXML
    private void backToSelectionClicked() {
        /* Just in case. See the huge comment in the hideUnavailableClicked() method. */
        setSelected(0);
        tableWithFiles.setItems(FXCollections.emptyObservableList());
        tableWithFiles.getFilePanes().clear();
        clearGeneralResult();
        lastProcessedFileInfo = null;
        allAvailableCount.setValue(0);
        setAllSelected(false);
        context.setWorkWithVideosInProgress(false);

        choicePane.setVisible(true);
        resultPane.setVisible(false);
    }

    @FXML
    private void refreshButtonClicked() {
        //todo check if > 10000

        lastProcessedFileInfo = null;
        clearGeneralResult();

        LoadDirectoryFilesTask task = new LoadDirectoryFilesTask(
                this.directory,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context,
                selected,
                allSelected,
                allAvailableCount,
                this::loadAllFileSubtitleSizes,
                this::loadSingleFileSubtitleSize,
                this::addExternalSubtitleFileClicked,
                this::removeExternalSubtitleFileClicked,
                result -> {
                    filesInfo = result.getFilesInfo();
                    allGuiFilesInfo = result.getAllGuiFilesInfo();
                    filePanes = result.getFilePanes();
                    updateTableContent(result.getGuiFilesToShowInfo(), result.getFilePanes());
                    hideUnavailableCheckbox.setSelected(result.isHideUnavailable());

                    /* See the huge comment in the hideUnavailableClicked() method. */
                    tableWithFiles.scrollTo(0);

                    stopProgress();
                }
        );

        startBackgroundTask(task);
    }

    @FXML
    private void hideUnavailableClicked() {
        clearGeneralResult();
        clearLastProcessedResult();

        SortOrShowHideUnavailableTask task = new SortOrShowHideUnavailableTask(
                allGuiFilesInfo,
                hideUnavailableCheckbox.isSelected(),
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                result -> {
                    updateTableContent(result, filePanes);

                    /*
                     * There is a strange bug with TableView - when the list is shrunk in size (because for example
                     * "hide unavailable" checkbox is checked but it can also happen when refresh is clicked I suppose) and both
                     * big list and shrunk list have vertical scrollbars table isn't shrunk unless you move the scrollbar.
                     * I've tried many workaround but this one seems the best so far - just show the beginning of the table.
                     * I couldn't find a bug with precise description but these ones fit quite well -
                     * https://bugs.openjdk.java.net/browse/JDK-8095384, https://bugs.openjdk.java.net/browse/JDK-8087833.
                     */
                    tableWithFiles.scrollTo(0);

                    stopProgress();
                }
        );

        startBackgroundTask(task);
    }

    @FXML
    private void removeButtonClicked() {
        clearGeneralResult();
        clearLastProcessedResult();

        List<Integer> indices = tableWithFiles.getSelectionModel().getSelectedIndices();
        if (CollectionUtils.isEmpty(indices)) {
            return;
        }

        RemoveFilesTask task = new RemoveFilesTask(
                filesInfo,
                allGuiFilesInfo,
                tableWithFiles.getItems(),
                filePanes,
                indices,
                result -> {
                    filesInfo = result.getFilesInfo();
                    allGuiFilesInfo = result.getAllGuiFilesInfo();
                    filePanes = result.getFilePanes();
                    updateTableContent(result.getGuiFilesToShowInfo(), result.getFilePanes());

                    if (result.getRemovedCount() == 0) {
                        throw new IllegalStateException();
                    } else if (result.getRemovedCount() == 1) {
                        setResult("File has been removed from the list successfully", null, null);
                    } else {
                        setResult(result.getRemovedCount() + " files have been removed from the list successfully", null, null);
                    }

                    stopProgress();
                }
        );

        startBackgroundTask(task);
    }

    @FXML
    private void addButtonClicked() {
        clearGeneralResult();
        clearLastProcessedResult();

        List<File> filesToAdd = getFiles(stage, context.getSettings());
        if (CollectionUtils.isEmpty(filesToAdd)) {
            return;
        }

        try {
            context.getSettings().saveLastDirectoryWithVideos(filesToAdd.get(0).getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        AddFilesTask task = new AddFilesTask(
                filesInfo,
                filesToAdd,
                allGuiFilesInfo,
                hideUnavailableCheckbox.isSelected(),
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context,
                selected,
                allSelected,
                allAvailableCount,
                this::loadAllFileSubtitleSizes,
                this::loadSingleFileSubtitleSize,
                this::addExternalSubtitleFileClicked,
                this::removeExternalSubtitleFileClicked,
                result -> {
                    filesInfo = result.getFilesInfo();
                    allGuiFilesInfo = result.getAllGuiFilesInfo();
                    filePanes = result.getFilePanes();
                    updateTableContent(result.getGuiFilesToShowInfo(), result.getFilePanes());

                    if (result.getAddedCount() == 0) {
                        if (filesToAdd.size() == 1) {
                            setResult("File has been added already", null, null);
                        } else {
                            setResult("All " + filesToAdd.size() + " files have been added already", null, null);
                        }
                    } else if (result.getAddedCount() == filesToAdd.size()) {
                        if (result.getAddedCount() == 1) {
                            setResult("File has been added successfully", null, null);
                        } else {
                            setResult("All " + result.getAddedCount() + " files have been added successfully", null, null);
                        }
                    } else {
                        String message = result.getAddedCount() + "/" + filesToAdd.size() + " successfully added, "
                                + (filesToAdd.size() - result.getAddedCount()) + "/" + filesToAdd.size() + " added before";

                        setResult(message, null, null);
                    }

                    stopProgress();
                }
        );

        startBackgroundTask(task);
    }

    public static GuiFileInfo findMatchingGuiFileInfo(FileInfo fileInfo, List<GuiFileInfo> guiFilesInfo) {
        return guiFilesInfo.stream()
                .filter(guiFileInfo -> Objects.equals(guiFileInfo.getFullPath(), fileInfo.getFile().getAbsolutePath()))
                .findFirst().orElseThrow(IllegalStateException::new);
    }

    public static FileInfo findMatchingFileInfo(GuiFileInfo guiFileInfo, List<FileInfo> filesInfo) {
        return filesInfo.stream()
                .filter(fileInfo -> Objects.equals(fileInfo.getFile().getAbsolutePath(), guiFileInfo.getFullPath()))
                .findFirst().orElseThrow(IllegalStateException::new);
    }

    public static GuiSubtitleStream findMatchingGuiStream(int ffmpegIndex, List<GuiSubtitleStream> guiStreams) {
        return guiStreams.stream()
                .filter(stream -> stream.getFfmpegIndex() == ffmpegIndex)
                .findFirst().orElseThrow(IllegalStateException::new);
    }

    public static boolean haveSubtitlesToLoad(FileInfo fileInfo) {
        if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
            return false;
        }

        return fileInfo.getSubtitleStreams().stream()
                .anyMatch(stream -> stream.getSubtitles() == null);
    }

    public static boolean isExtra(SubtitleStream subtitleStream, GuiSettings guiSettings) {
        return subtitleStream.getLanguage() != guiSettings.getUpperLanguage()
                && subtitleStream.getLanguage() != guiSettings.getLowerLanguage();
    }

    public static int getSubtitleCanBeHiddenCount(FileInfo fileInfo, GuiSettings guiSettings) {
        if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
            return 0;
        }

        boolean hasSubtitlesWithUpperLanguage = fileInfo.getSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .anyMatch(stream -> stream.getLanguage() == guiSettings.getUpperLanguage());
        boolean hasSubtitlesWithLowerLanguage = fileInfo.getSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .anyMatch(stream -> stream.getLanguage() == guiSettings.getLowerLanguage());
        int subtitlesWithOtherLanguage = (int) fileInfo.getSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .filter(stream -> stream.getLanguage() != guiSettings.getUpperLanguage())
                .filter(stream -> stream.getLanguage() != guiSettings.getLowerLanguage())
                .count();

        if (!hasSubtitlesWithUpperLanguage || !hasSubtitlesWithLowerLanguage) {
            return 0;
        }

        return subtitlesWithOtherLanguage;
    }

    private void addExternalSubtitleFileClicked(GuiFileInfo guiFileInfo) {
        clearGeneralResult();
        clearLastProcessedResult();
        guiFileInfo.clearResult();
        lastProcessedFileInfo = guiFileInfo;

        File file = getFile(guiFileInfo, stage, context.getSettings()).orElse(null);
        if (file == null) {
            return;
        }

        try {
            context.getSettings().saveLastDirectoryWithExternalSubtitles(file.getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error(
                    "failed to save last directory , file " + file.getAbsolutePath() + ": "
                            + ExceptionUtils.getStackTrace(e)
            );
        }

        FileInfo fileInfo = findMatchingFileInfo(guiFileInfo, filesInfo);

        if (isDuplicate(file, fileInfo)) {
            guiFileInfo.setErrorMessage("This file is already added");
            return;
        }

        if (file.length() / 1024 / 1024 > GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES) {
            guiFileInfo.setErrorMessage("File is too big (>" + GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES + " megabytes)");
            return;
        }

        try {
            Subtitles subtitles = Parser.fromSubRipText(
                    FileUtils.readFileToString(file, StandardCharsets.UTF_8),
                    "external",
                    null
            );

            GuiExternalSubtitleFile guiExternalSubtitleFile;
            if (fileInfo.getExternalSubtitleFiles().size() == 0) {
                guiExternalSubtitleFile = guiFileInfo.getExternalSubtitleFiles().get(1);
            } else if (fileInfo.getExternalSubtitleFiles().size() == 1) {
                guiExternalSubtitleFile = guiFileInfo.getExternalSubtitleFiles().get(0);
            } else {
                throw new IllegalStateException();
            }

            int subtitleSize = SubtitleStream.calculateSubtitleSize(subtitles);

            guiExternalSubtitleFile.setFileName(file.getName());
            guiExternalSubtitleFile.setSize(subtitleSize);

            fileInfo.getExternalSubtitleFiles().add(new ExternalSubtitleFile(file, subtitles, subtitleSize));
            guiFileInfo.setSuccessMessage("Subtitle file has been added to the list successfully");
        } catch (IOException e) {
            guiFileInfo.setErrorMessage("Can't read the file");
        } catch (Parser.IncorrectFormatException e) {
            guiFileInfo.setErrorMessage("Can't add the file because it has incorrect format");
        }
    }

    private Optional<File> getFile(GuiFileInfo fileInfo, Stage stage, GuiSettings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose the file with the subtitles");

        File initialDirectory = settings.getLastDirectoryWithExternalSubtitles();
        if (initialDirectory == null) {
            File directoryWithFile = new File(fileInfo.getFullPath()).getParentFile();
            if (directoryWithFile != null && directoryWithFile.exists()) {
                initialDirectory = directoryWithFile;
            }
        }

        fileChooser.setInitialDirectory(initialDirectory);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("subrip files (*.srt)", "*.srt")
        );

        return Optional.ofNullable(fileChooser.showOpenDialog(stage));
    }

    private boolean isDuplicate(File file, FileInfo fileInfo) {
        for (ExternalSubtitleFile externalSubtitleFile : fileInfo.getExternalSubtitleFiles()) {
            if (Objects.equals(file, externalSubtitleFile.getFile())) {
                return true;
            }
        }

        return false;
    }

    private void removeExternalSubtitleFileClicked(int index, GuiFileInfo guiFileInfo) {
        clearGeneralResult();
        clearLastProcessedResult();
        guiFileInfo.clearResult();
        lastProcessedFileInfo = guiFileInfo;

        FileInfo fileInfo = findMatchingFileInfo(guiFileInfo, filesInfo);
        fileInfo.getExternalSubtitleFiles().remove(1 - index);

        guiFileInfo.getExternalSubtitleFiles().get(index).setFileName(null);
        guiFileInfo.getExternalSubtitleFiles().get(index).setSize(-1);
        guiFileInfo.getExternalSubtitleFiles().get(index).setSelectedAsUpper(false);
        guiFileInfo.getExternalSubtitleFiles().get(index).setSelectedAsLower(false);

        guiFileInfo.setSuccessMessage("Subtitle file has been removed from the list successfully");
    }

    private void loadAllFileSubtitleSizes(GuiFileInfo guiFileInfo) {
        clearGeneralResult();
        clearLastProcessedResult();
        guiFileInfo.clearResult();
        lastProcessedFileInfo = guiFileInfo;

        LoadSingleFileAllSubtitlesTask task = new LoadSingleFileAllSubtitlesTask(
                findMatchingFileInfo(guiFileInfo, filesInfo),
                guiFileInfo,
                context.getFfmpeg(),
                (result) -> {
                    showResult(result);
                    stopProgress();
                }
        );

        startBackgroundTask(task);
    }

    private void loadSingleFileSubtitleSize(GuiFileInfo guiFileInfo, int ffmpegIndex) {
        clearGeneralResult();
        clearLastProcessedResult();
        guiFileInfo.clearResult();
        lastProcessedFileInfo = guiFileInfo;

        LoadSingleSubtitleTask task = new LoadSingleSubtitleTask(
                ffmpegIndex,
                findMatchingFileInfo(guiFileInfo, filesInfo),
                guiFileInfo,
                context.getFfmpeg(),
                (result) -> {
                    //todo fix showResult(result);
                    stopProgress();
                }
        );

        startBackgroundTask(task);
    }

    public boolean isAllSelected() {
        return allSelected.get();
    }

    public BooleanProperty allSelectedProperty() {
        return allSelected;
    }

    public void setAllSelected(boolean allSelected) {
        this.allSelected.set(allSelected);
    }

    public long getSelected() {
        return selected.get();
    }

    public LongProperty selectedProperty() {
        return selected;
    }

    public void setSelected(long selected) {
        this.selected.set(selected);
    }
}
