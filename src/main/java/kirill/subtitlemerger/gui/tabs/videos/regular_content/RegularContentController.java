package kirill.subtitlemerger.gui.tabs.videos.regular_content;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.core.GuiUtils;
import kirill.subtitlemerger.gui.core.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.core.custom_controls.MultiColorLabels;
import kirill.subtitlemerger.gui.core.custom_controls.SimpleSubtitlePreview;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.*;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiExternalSubtitleStream;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStream;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.TableWithFiles;
import kirill.subtitlemerger.logic.core.SubtitleParser;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.utils.FileValidator;
import kirill.subtitlemerger.logic.work_with_files.SubtitleInjector;
import kirill.subtitlemerger.logic.work_with_files.entities.*;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
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
    private MultiColorLabels generalResult;

    @FXML
    private Label selectedForMergeLabel;

    @FXML
    private CheckBox hideUnavailableCheckbox;

    @FXML
    private Pane autoSelectButtonWrapper;

    @FXML
    private Button autoSelectButton;

    @FXML
    private Pane loadAllSubtitlesButtonWrapper;

    @FXML
    private Button loadAllSubtitlesButton;

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

    public void initialize(Stage stage, GuiContext guiContext) {
        this.stage = stage;
        this.context = guiContext;
        sortByGroup = new ToggleGroup();
        sortDirectionGroup = new ToggleGroup();
        allSelected = new SimpleBooleanProperty(false);
        selected = new SimpleIntegerProperty(0);
        allAvailableCount = new SimpleIntegerProperty(0);

        setSelectedForMergeLabelText(getSelected());
        setActionButtonsVisibility(getSelected());
        selected.addListener(this::selectedCountChanged);

        ContextMenu contextMenu = generateContextMenu(sortByGroup, sortDirectionGroup, guiContext.getSettings());
        sortByGroup.selectedToggleProperty().addListener(this::sortByChanged);
        sortDirectionGroup.selectedToggleProperty().addListener(this::sortDirectionChanged);

        tableWithFiles.initialize(
                allSelected,
                selected,
                allAvailableCount,
                this::loadAllFileSubtitleSizes,
                this::loadSingleFileSubtitleSize,
                this::addExternalSubtitleFileClicked,
                this::removeExternalSubtitleFileClicked,
                this::showFfmpegStreamPreview
        );
        tableWithFiles.setContextMenu(contextMenu);

        removeSelectedButton.disableProperty().bind(selected.isEqualTo(0));
    }

    private void setSelectedForMergeLabelText(int selected) {
        selectedForMergeLabel.setText(
                GuiUtils.getTextDependingOnTheCount(
                        selected,
                        "1 video selected",
                        "%d videos selected"
                )
        );
    }

    private void setActionButtonsVisibility(int selected) {
        if (selected == 0) {
            String tooltipText = "no videos are selected for merge";

            autoSelectButton.setDisable(true);
            Tooltip.install(autoSelectButtonWrapper, GuiUtils.generateTooltip(tooltipText));

            loadAllSubtitlesButton.setDisable(true);
            Tooltip.install(loadAllSubtitlesButtonWrapper, GuiUtils.generateTooltip(tooltipText));

            goButton.setDisable(true);
            Tooltip.install(goButtonWrapper, GuiUtils.generateTooltip(tooltipText));
        } else {
            autoSelectButton.setDisable(false);
            Tooltip.install(autoSelectButtonWrapper, null);

            loadAllSubtitlesButton.setDisable(false);
            Tooltip.install(loadAllSubtitlesButtonWrapper, null);

            goButton.setDisable(false);
            Tooltip.install(goButtonWrapper, null);
        }
    }

    private void selectedCountChanged(Observable observable) {
        setActionButtonsVisibility(getSelected());
        setSelectedForMergeLabelText(getSelected());
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
                    generalResult.set(AutoSelectSubtitlesTask.generateMultiPartResult(result));
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
    private void loadAllSubtitlesClicked() {
        generalResult.clear();
        lastProcessedFileInfo = null;

        LoadFilesAllSubtitlesTask task = new LoadFilesAllSubtitlesTask(
                filesInfo,
                tableWithFiles.getItems(),
                result -> {
                    generalResult.set(LoadFilesAllSubtitlesTask.generateMultiPartResult(result));
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
        SubtitleStream upperSubtitles = SubtitleStream.getByUniqueId(
                guiUpperSubtitles.getUniqueId(),
                fileInfo.getSubtitleStreams()
        );

        GuiSubtitleStream guiLowerSubtitles = guiFileInfo.getSubtitleStreams().stream()
                .filter(GuiSubtitleStream::isSelectedAsLower)
                .findFirst().orElseThrow(IllegalStateException::new);
        SubtitleStream lowerSubtitles = SubtitleStream.getByUniqueId(
                guiLowerSubtitles.getUniqueId(),
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
                    updateTableContent(result, tableWithFiles.getMode(), false);
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

    private void updateTableContent(
            List<GuiFileInfo> guiFilesToShowInfo,
            TableWithFiles.Mode mode,
            boolean clearTableCache
    ) {
        setSelected((int) guiFilesToShowInfo.stream().filter(GuiFileInfo::isSelected).count());

        allAvailableCount.setValue(
                (int) guiFilesToShowInfo.stream()
                        .filter(filesInfo -> StringUtils.isBlank(filesInfo.getUnavailabilityReason()))
                        .count()
        );

        if (clearTableCache) {
            tableWithFiles.clearCache();
        }
        tableWithFiles.setMode(mode);
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
                    updateTableContent(result, tableWithFiles.getMode(), false);
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

        try {
            context.getSettings().saveLastDirectoryWithVideos(files.get(0).getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, shouldn't happen: " + getStackTrace(e));
        }

        context.setWorkWithVideosInProgress(true);

        directory = null;

        LoadSeparateFilesTask task = new LoadSeparateFilesTask(
                files,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context,
                result -> {
                    filesInfo = result.getFilesInfo();
                    allGuiFilesInfo = result.getAllGuiFilesInfo();
                    updateTableContent(result.getGuiFilesToShowInfo(), TableWithFiles.Mode.SEPARATE_FILES, true);
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

        try {
            context.getSettings().saveLastDirectoryWithVideos(directory.getAbsolutePath());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        context.setWorkWithVideosInProgress(true);

        this.directory = directory;

        LoadDirectoryFilesTask task = new LoadDirectoryFilesTask(
                this.directory,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context,
                result -> {
                    filesInfo = result.getFilesInfo();
                    allGuiFilesInfo = result.getAllGuiFilesInfo();
                    updateTableContent(result.getGuiFilesToShowInfo(), TableWithFiles.Mode.DIRECTORY, true);
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
        setSelected(0);

        tableWithFiles.clearCache();
        tableWithFiles.setMode(null);
        tableWithFiles.setItems(FXCollections.emptyObservableList());
        generalResult.clear();
        lastProcessedFileInfo = null;
        allAvailableCount.setValue(0);
        setAllSelected(false);
        context.setWorkWithVideosInProgress(false);

        choicePane.setVisible(true);
        resultPane.setVisible(false);

        System.gc();
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
                result -> {
                    filesInfo = result.getFilesInfo();
                    allGuiFilesInfo = result.getAllGuiFilesInfo();
                    updateTableContent(result.getGuiFilesToShowInfo(), tableWithFiles.getMode(), true);
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
                    updateTableContent(result, tableWithFiles.getMode(), false);

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

        RemoveFilesTask task = new RemoveFilesTask(
                filesInfo,
                allGuiFilesInfo,
                tableWithFiles.getItems(),
                result -> {
                    filesInfo = result.getFilesInfo();
                    allGuiFilesInfo = result.getAllGuiFilesInfo();
                    updateTableContent(result.getGuiFilesToShowInfo(), tableWithFiles.getMode(), false);

                    if (result.getRemovedCount() == 0) {
                        log.error("nothing has been removed, that shouldn't happen");
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
                result -> {
                    filesInfo = result.getFilesInfo();
                    allGuiFilesInfo = result.getAllGuiFilesInfo();
                    updateTableContent(result.getGuiFilesToShowInfo(), tableWithFiles.getMode(), false);
                    generalResult.set(AddFilesTask.generateMultiPartResult(result));
                    stopProgress();
                }
        );

        prepareAndStartBackgroundTask(task);
    }

    public static boolean isExtra(FfmpegSubtitleStream subtitleStream, GuiSettings guiSettings) {
        return subtitleStream.getLanguage() != guiSettings.getUpperLanguage()
                && subtitleStream.getLanguage() != guiSettings.getLowerLanguage();
    }

    public static int getSubtitleCanBeHiddenCount(FileInfo fileInfo, GuiSettings guiSettings) {
        if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
            return 0;
        }

        boolean hasSubtitlesWithUpperLanguage = fileInfo.getFfmpegSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .anyMatch(stream -> stream.getLanguage() == guiSettings.getUpperLanguage());
        boolean hasSubtitlesWithLowerLanguage = fileInfo.getFfmpegSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .anyMatch(stream -> stream.getLanguage() == guiSettings.getLowerLanguage());
        int subtitlesWithOtherLanguage = (int) fileInfo.getFfmpegSubtitleStreams().stream()
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

        FileInfo fileInfo = GuiUtils.findMatchingFileInfo(guiFileInfo, filesInfo);

        GuiExternalSubtitleStream guiExternalSubtitleStream;
        File otherFile;
        if (CollectionUtils.isEmpty(fileInfo.getExternalSubtitleStreams())) {
            otherFile = null;
            guiExternalSubtitleStream = guiFileInfo.getExternalSubtitleStreams().get(0);
        } else if (fileInfo.getExternalSubtitleStreams().size() == 1) {
            otherFile = fileInfo.getExternalSubtitleStreams().get(0).getFile();
            guiExternalSubtitleStream = guiFileInfo.getExternalSubtitleStreams().get(1);
        } else {
            log.error("unexpected amount of subtitle streams: " + fileInfo.getExternalSubtitleStreams().size());
            throw new IllegalStateException();
        }

        //todo refactor, logic is cluttered
        BackgroundTask<ExternalSubtitleFileInfo> task = new BackgroundTask<>() {
            @Override
            protected ExternalSubtitleFileInfo run() {
                updateMessage("processing file " + file.getAbsolutePath() + "...");
                return getInputFileInfo(file, otherFile).orElseThrow(IllegalStateException::new);
            }

            @Override
            protected void onFinish(ExternalSubtitleFileInfo result) {
                try {
                    if (result.getParent() != null) {
                        context.getSettings().saveLastDirectoryWithExternalSubtitles(file.getParent());
                    }
                } catch (GuiSettings.ConfigException e) {
                    log.error(
                            "failed to save last directory , file " + file.getAbsolutePath() + ": "
                                    + ExceptionUtils.getStackTrace(e)
                    );
                }

                if (result.getIncorrectFileReason() != null) {
                    if (result.getIncorrectFileReason() == IncorrectSubtitleFileReason.INCORRECT_SUBTITLE_FORMAT) {
                       // guiExternalSubtitleStream.setFileName(file.getName());
                        //guiExternalSubtitleStream.setSize((int) file.length());

                        fileInfo.getSubtitleStreams().add(new ExternalSubtitleStream(SubtitleCodec.SUBRIP, result.getSubtitles(), file));
                    }

                    guiFileInfo.setResultOnlyError(getErrorText(file.getAbsolutePath(), result.getIncorrectFileReason()));
                } else if (result.isDuplicate()) {
                    guiFileInfo.setResultOnlyError("This has already been added");
                } else {
                    guiExternalSubtitleStream.setFileName(file.getName());
                    guiExternalSubtitleStream.setSize((int) file.length());

                    fileInfo.getSubtitleStreams().add(new ExternalSubtitleStream(SubtitleCodec.SUBRIP, result.getSubtitles(), file));
                    guiFileInfo.setResultOnlySuccess("Subtitle file has been added to the list successfully");
                }

                stopProgress();
            }
        };

        prepareAndStartBackgroundTask(task);
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

    private static Optional<ExternalSubtitleFileInfo> getInputFileInfo(File file, File otherSubtitleFile) {
        FileValidator.InputFileInfo validatorFileInfo = FileValidator.getInputFileInfo(
                file.getAbsolutePath(),
                Collections.singletonList("srt"),
                false,
                GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024,
                true
        ).orElseThrow(IllegalStateException::new);

        if (validatorFileInfo.getIncorrectFileReason() != null) {
            return Optional.of(
                    new ExternalSubtitleFileInfo(
                            validatorFileInfo.getFile(),
                            validatorFileInfo.getParent(),
                            false,
                            validatorFileInfo.getContent(),
                            null,
                            ExternalSubtitleFileInfo.from(validatorFileInfo.getIncorrectFileReason())
                    )
            );
        }

        try {
            Subtitles subtitles = SubtitleParser.fromSubRipText(
                    new String(validatorFileInfo.getContent(), StandardCharsets.UTF_8),
                    null
            );

            return Optional.of(
                    new ExternalSubtitleFileInfo(
                            validatorFileInfo.getFile(),
                            validatorFileInfo.getParent(),
                            Objects.equals(file, otherSubtitleFile),
                            validatorFileInfo.getContent(),
                            subtitles,
                            null
                    )
            );
        } catch (SubtitleParser.IncorrectFormatException e) {
            return Optional.of(
                    new ExternalSubtitleFileInfo(
                            validatorFileInfo.getFile(),
                            validatorFileInfo.getParent(),
                            false,
                            validatorFileInfo.getContent(),
                            null,
                            IncorrectSubtitleFileReason.INCORRECT_SUBTITLE_FORMAT
                    )
            );
        }
    }

    private static String getErrorText(String path, IncorrectSubtitleFileReason reason) {
        path = GuiUtils.getShortenedStringIfNecessary(path, 20, 40);

        switch (reason) {
            case PATH_IS_TOO_LONG:
                return "File path is too long";
            case INVALID_PATH:
                return "File path is invalid";
            case IS_A_DIRECTORY:
                return path + " is a directory, not a file";
            case FILE_DOES_NOT_EXIST:
                return "File '" + path + "' doesn't exist";
            case FAILED_TO_GET_PARENT_DIRECTORY:
                return path + ": failed to get parent directory";
            case EXTENSION_IS_NOT_VALID:
                return "File '" + path + "' has an incorrect extension";
            case FILE_IS_EMPTY:
                return "File '" + path + "' is empty";
            case FILE_IS_TOO_BIG:
                return "File '" + path + "' is too big (>"
                        + GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES + " megabytes)";
            case FAILED_TO_READ_CONTENT:
                return path + ": failed to read the file";
            case INCORRECT_SUBTITLE_FORMAT:
                return "File '" + path + "' has an incorrect subtitle format, it can happen if the file is not UTF-8-encoded"
                        + ", you can change the encoding pressing the preview button";
            default:
                throw new IllegalStateException();
        }
    }

    private void removeExternalSubtitleFileClicked(int index, GuiFileInfo guiFileInfo) {
        generalResult.clear();
        clearLastProcessedResult();
        guiFileInfo.clearResult();
        lastProcessedFileInfo = guiFileInfo;

        FileInfo fileInfo = GuiUtils.findMatchingFileInfo(guiFileInfo, filesInfo);

        int indexOfLastStream = fileInfo.getSubtitleStreams().size() - 1;
        if (!(fileInfo.getSubtitleStreams().get(indexOfLastStream) instanceof ExternalSubtitleStream)) {
            log.error("last stream is not external, that shouldn't happen");
            throw new IllegalStateException();
        }
        fileInfo.getSubtitleStreams().remove(indexOfLastStream);

        guiFileInfo.getExternalSubtitleStreams().get(index).setFileName(null);
        guiFileInfo.getExternalSubtitleStreams().get(index).setSize(GuiSubtitleStream.UNKNOWN_SIZE);
        guiFileInfo.getExternalSubtitleStreams().get(index).setSelectedAsUpper(false);
        guiFileInfo.getExternalSubtitleStreams().get(index).setSelectedAsLower(false);

        guiFileInfo.setResultOnlySuccess("Subtitle file has been removed from the list successfully");
    }

    private void showFfmpegStreamPreview(int ffmpegIndex, GuiFileInfo guiFileInfo) {
        generalResult.clear();
        clearLastProcessedResult();

        Stage dialogStage = new Stage();

        FileInfo fileInfo = GuiUtils.findMatchingFileInfo(guiFileInfo, filesInfo);

        FfmpegSubtitleStream stream = fileInfo.getFfmpegSubtitleStreams().stream()
                .filter(currentStream -> currentStream.getFfmpegIndex() == ffmpegIndex)
                .findFirst().orElseThrow(IllegalStateException::new);

        SimpleSubtitlePreview subtitlePreviewDialog = new SimpleSubtitlePreview();

        //todo make a method for titles
        String fullTitle = stream.getLanguage() != null
                ? stream.getLanguage().toString().toUpperCase()
                : "UNKNOWN LANGUAGE";

        fullTitle += (StringUtils.isBlank(stream.getTitle()) ? "" : " " + stream.getTitle());

        fullTitle = GuiUtils.getShortenedStringIfNecessary(
                guiFileInfo.getFullPath(),
                0,
                128
        ) + ", " + fullTitle;
        subtitlePreviewDialog.getController().initializeSingleSubtitles(stream.getSubtitles(), fullTitle, dialogStage);

        dialogStage.setTitle("Subtitle preview");
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.setResizable(false);

        Scene scene = new Scene(subtitlePreviewDialog);
        scene.getStylesheets().add("/gui/style.css");
        dialogStage.setScene(scene);

        dialogStage.showAndWait();
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
                    generalResult.set(LoadSingleSubtitleTask.generateMultiPartResult(result));
                    stopProgress();
                },
                context.getFfmpeg()
        );

        prepareAndStartBackgroundTask(task);
    }

    //todo merge with the same file in subtitle files tab controller
    @AllArgsConstructor
    @Getter
    private static class ExternalSubtitleFileInfo {
        private File file;

        private File parent;

        @Setter
        private boolean isDuplicate;

        private byte[] rawData;

        @Setter
        private Subtitles subtitles;

        @Setter
        private IncorrectSubtitleFileReason incorrectFileReason;

        static IncorrectSubtitleFileReason from(FileValidator.IncorrectInputFileReason reason) {
            switch (reason) {
                case PATH_IS_TOO_LONG:
                    return IncorrectSubtitleFileReason.PATH_IS_TOO_LONG;
                case INVALID_PATH:
                    return IncorrectSubtitleFileReason.INVALID_PATH;
                case IS_A_DIRECTORY:
                    return IncorrectSubtitleFileReason.IS_A_DIRECTORY;
                case FILE_DOES_NOT_EXIST:
                    return IncorrectSubtitleFileReason.FILE_DOES_NOT_EXIST;
                case FAILED_TO_GET_PARENT_DIRECTORY:
                    return IncorrectSubtitleFileReason.FAILED_TO_GET_PARENT_DIRECTORY;
                case EXTENSION_IS_NOT_VALID:
                    return IncorrectSubtitleFileReason.EXTENSION_IS_NOT_VALID;
                case FILE_IS_EMPTY:
                    return IncorrectSubtitleFileReason.FILE_IS_EMPTY;
                case FILE_IS_TOO_BIG:
                    return IncorrectSubtitleFileReason.FILE_IS_TOO_BIG;
                case FAILED_TO_READ_CONTENT:
                    return IncorrectSubtitleFileReason.FAILED_TO_READ_CONTENT;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private enum IncorrectSubtitleFileReason {
        PATH_IS_TOO_LONG,
        INVALID_PATH,
        IS_A_DIRECTORY,
        FILE_DOES_NOT_EXIST,
        FAILED_TO_GET_PARENT_DIRECTORY,
        EXTENSION_IS_NOT_VALID,
        FILE_IS_EMPTY,
        FILE_IS_TOO_BIG,
        FAILED_TO_READ_CONTENT,
        INCORRECT_SUBTITLE_FORMAT
    }
}
