package kirill.subtitlemerger.gui.tabs.videos.regular_content;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import kirill.subtitlemerger.gui.core.GuiUtils;
import kirill.subtitlemerger.gui.core.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.core.custom_controls.MultiColorResultLabels;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.*;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private MultiColorResultLabels generalResult;

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

    @FXML
    private Pane cancelTaskPane;

    private ToggleGroup sortByGroup;

    private ToggleGroup sortDirectionGroup;

    private BackgroundTask<?> currentTask;

    private File directory;

    private List<FileInfo> filesInfo;

    private List<GuiFileInfo> allGuiFilesInfo;

    private Map<String, FilePanes> filePanes;

    private BooleanProperty allSelected;

    private IntegerProperty selected;

    private IntegerProperty allAvailableCount;

    /*
     * Before performing a one-file operation it is better to clean the result of the previous one-file operation, it
     * will look better. We can't just clear all files' results because it may take a lot of time if there are many
     * files and it's unacceptable for generally fast one-file operations. So we will keep track of the last processed
     * file to clear its result very fast when starting next one-file operation.
     */
    private GuiFileInfo lastProcessedFileInfo;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.stage = stage;
        this.context = guiContext;
        this.sortByGroup = new ToggleGroup();
        this.sortDirectionGroup = new ToggleGroup();
        this.allSelected = new SimpleBooleanProperty(false);
        /*
         * Set negative value so that value is definitely changed after loading files and invalidation listener is
         * triggered.
         */
        this.selected = new SimpleIntegerProperty(-1);
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
        /*//todo restore this.removeSelectedButton.disableProperty().bind(
                Bindings.isEmpty(tableWithFiles.getSelectionModel().getSelectedIndices())
        );*/
    }

    private void selectedCountChangeListener(Observable observable) {
        setActionButtonsVisibility();

        selectedForMergeLabel.setText(
                GuiUtils.getTextDependingOnTheCount(
                        getSelected(),
                        "1 video selected",
                        "%d videos selected"
                )
        );
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
        generalResult.clear();
        lastProcessedFileInfo = null;

        AutoSelectSubtitlesTask task = new AutoSelectSubtitlesTask(
                filesInfo,
                tableWithFiles.getItems(),
                context.getFfmpeg(),
                context.getSettings(),
                result -> {
                    generalResult.update(AutoSelectSubtitlesTask.generateMultiPartResult(result));
                    stopProgress();
                }
        );

        prepareAndStartBackgroundTask(task);
    }

    private void prepareAndStartBackgroundTask(BackgroundTask<?> task) {
        currentTask = task;
        showProgress(task);
        cancelTaskPane.visibleProperty().bind(task.cancellationPossibleProperty());
        task.start();
    }

    @FXML
    private void getAllSizesButtonClicked() {
        generalResult.clear();
        lastProcessedFileInfo = null;

        LoadFilesAllSubtitlesTask task = new LoadFilesAllSubtitlesTask(
                filesInfo,
                tableWithFiles.getItems(),
                result -> {
                    generalResult.update(LoadFilesAllSubtitlesTask.generateMultiPartResult(result));
                    stopProgress();
                },
                context.getFfmpeg()
        );

        prepareAndStartBackgroundTask(task);
    }

    @FXML
    private void goButtonClicked() throws FfmpegException {
        GuiFileInfo guiFileInfo = tableWithFiles.getItems().get(0);
        FileInfo fileInfo = GuiUtils.findMatchingFileInfo(guiFileInfo, filesInfo);

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
        generalResult.clear();
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

        prepareAndStartBackgroundTask(task);
    }

    private void clearLastProcessedResult() {
        if (lastProcessedFileInfo != null) {
            lastProcessedFileInfo.clearResult();
        }
    }

    private void updateTableContent(List<GuiFileInfo> guiFilesToShowInfo, Map<String, FilePanes> filePanes) {
        setSelected((int) guiFilesToShowInfo.stream().filter(GuiFileInfo::isSelected).count());

        allAvailableCount.setValue(
                (int) guiFilesToShowInfo.stream()
                        .filter(filesInfo -> StringUtils.isBlank(filesInfo.getUnavailabilityReason()))
                        .count()
        );

        tableWithFiles.getFilePanes().clear();
        tableWithFiles.getFilePanes().putAll(filePanes);
        tableWithFiles.setItems(FXCollections.observableArrayList(guiFilesToShowInfo));
        setAllSelected(allAvailableCount.get() > 0 && getSelected() == allAvailableCount.get());
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
        generalResult.clear();
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


        prepareAndStartBackgroundTask(task);
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

        prepareAndStartBackgroundTask(task);
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

        prepareAndStartBackgroundTask(task);
    }

    private static Optional<File> getDirectory(Stage stage, GuiSettings settings) {
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("choose directory with videos");
        directoryChooser.setInitialDirectory(settings.getLastDirectoryWithVideos());

        return Optional.ofNullable(directoryChooser.showDialog(stage));
    }

    @FXML
    private void backToSelectionClicked() {
        /*
         * Set negative value so that value is definitely changed after loading files and invalidation listener is
         * triggered.
         */
        setSelected(-1);

        tableWithFiles.setItems(FXCollections.emptyObservableList());
        tableWithFiles.getFilePanes().clear();
        generalResult.clear();
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
        generalResult.clear();

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

        prepareAndStartBackgroundTask(task);
    }

    @FXML
    private void hideUnavailableClicked() {
        generalResult.clear();
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

        prepareAndStartBackgroundTask(task);
    }

    @FXML
    private void removeButtonClicked() {
        generalResult.clear();
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
                        generalResult.setOnlySuccess("File has been removed from the list successfully");
                    } else {
                        generalResult.setOnlySuccess(result.getRemovedCount() + " files have been removed from the list successfully");
                    }

                    stopProgress();
                }
        );

        prepareAndStartBackgroundTask(task);
    }

    @FXML
    private void addButtonClicked() {
        generalResult.clear();
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
                    generalResult.update(AddFilesTask.generateMultiPartResult(result));
                    stopProgress();
                }
        );

        prepareAndStartBackgroundTask(task);
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
        generalResult.clear();
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

        FileInfo fileInfo = GuiUtils.findMatchingFileInfo(guiFileInfo, filesInfo);

        if (isDuplicate(file, fileInfo)) {
            guiFileInfo.setResultOnlyError("This file is already added");
            return;
        }

        if (file.length() / 1024 / 1024 > GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES) {
            guiFileInfo.setResultOnlyError("File is too big (>" + GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES + " megabytes)");
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

            guiExternalSubtitleFile.setFileName(file.getName());
            guiExternalSubtitleFile.setSize(subtitles.getSize());

            fileInfo.getExternalSubtitleFiles().add(new ExternalSubtitleFile(file, subtitles));
            guiFileInfo.setResultOnlySuccess("Subtitle file has been added to the list successfully");
        } catch (IOException e) {
            guiFileInfo.setResultOnlyError("Can't read the file");
        } catch (Parser.IncorrectFormatException e) {
            guiFileInfo.setResultOnlyError("Can't add the file because it has incorrect format");
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
        generalResult.clear();
        clearLastProcessedResult();
        guiFileInfo.clearResult();
        lastProcessedFileInfo = guiFileInfo;

        FileInfo fileInfo = GuiUtils.findMatchingFileInfo(guiFileInfo, filesInfo);
        fileInfo.getExternalSubtitleFiles().remove(1 - index);

        guiFileInfo.getExternalSubtitleFiles().get(index).setFileName(null);
        guiFileInfo.getExternalSubtitleFiles().get(index).setSize(-1);
        guiFileInfo.getExternalSubtitleFiles().get(index).setSelectedAsUpper(false);
        guiFileInfo.getExternalSubtitleFiles().get(index).setSelectedAsLower(false);

        guiFileInfo.setResultOnlySuccess("Subtitle file has been removed from the list successfully");
    }

    private void loadAllFileSubtitleSizes(GuiFileInfo guiFileInfo) {
        generalResult.clear();
        clearLastProcessedResult();
        guiFileInfo.clearResult();
        lastProcessedFileInfo = guiFileInfo;

        LoadSingleFileAllSubtitlesTask task = new LoadSingleFileAllSubtitlesTask(
                GuiUtils.findMatchingFileInfo(guiFileInfo, filesInfo),
                guiFileInfo,
                (result) -> {
                    guiFileInfo.setResult(LoadFilesAllSubtitlesTask.generateMultiPartResult(result));
                    stopProgress();
                },
                context.getFfmpeg()
        );

        prepareAndStartBackgroundTask(task);
    }

    private void loadSingleFileSubtitleSize(GuiFileInfo guiFileInfo, int ffmpegIndex) {
        generalResult.clear();
        clearLastProcessedResult();
        guiFileInfo.clearResult();
        lastProcessedFileInfo = guiFileInfo;

        LoadSingleSubtitleTask task = new LoadSingleSubtitleTask(
                ffmpegIndex,
                GuiUtils.findMatchingFileInfo(guiFileInfo, filesInfo),
                guiFileInfo,
                (result) -> {
                    generalResult.update(LoadSingleSubtitleTask.generateMultiPartResult(result));
                    stopProgress();
                },
                context.getFfmpeg()
        );

        prepareAndStartBackgroundTask(task);
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

    public int getSelected() {
        return selected.get();
    }

    public IntegerProperty selectedProperty() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected.set(selected);
    }
}
