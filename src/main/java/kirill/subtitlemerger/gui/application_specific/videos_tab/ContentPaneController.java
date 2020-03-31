package kirill.subtitlemerger.gui.application_specific.videos_tab;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.AbstractController;
import kirill.subtitlemerger.gui.application_specific.SubtitlePreviewController;
import kirill.subtitlemerger.gui.application_specific.videos_tab.background.*;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.GuiUtils;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerCallback;
import kirill.subtitlemerger.gui.util.custom_controls.ActionResultLabels;
import kirill.subtitlemerger.gui.util.entities.ActionResult;
import kirill.subtitlemerger.gui.util.entities.FileOrigin;
import kirill.subtitlemerger.gui.util.entities.NodeAndController;
import kirill.subtitlemerger.logic.work_with_files.entities.*;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@CommonsLog
public class ContentPaneController extends AbstractController {
    @FXML
    private Pane contentPane;

    @FXML
    private Pane chosenDirectoryPane;

    @FXML
    private TextField chosenDirectoryField;

    @FXML
    private Button refreshButton;

    @FXML
    private ActionResultLabels generalResult;

    @FXML
    private Pane tableAndActionsPane;

    @FXML
    private Label selectedLabel;

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

    private String directoryPath;

    private List<FileInfo> filesInfo;

    private List<TableFileInfo> allTableFilesInfo;

    /*
     * Before performing a one-file operation it is better to clean the result of the previous one-file operation, it
     * will look better. We can't just clear all files' results because it may take a lot of time if there are many
     * files and it's unacceptable for generally fast one-file operations. So we will keep track of the last processed
     * file to clear its result very fast when starting next one-file operation.
     */
    private TableFileInfo lastProcessedFileInfo;

    private VideosTabController videosTabController;

    private Stage stage;

    private GuiContext context;

    public void initialize(VideosTabController videosTabController, Stage stage, GuiContext context) {
        this.videosTabController = videosTabController;
        this.stage = stage;
        this.context = context;

        GuiUtils.setTextFieldChangeListeners(
                chosenDirectoryField,
                (path) -> processDirectoryPath(path, FileOrigin.TEXT_FIELD)
        );

        updateSelectedLabelText(tableWithFiles.getAllSelectedCount());
        updateActionButtons(tableWithFiles.getAllSelectedCount(), tableWithFiles.getSelectedUnavailableCount());
        tableWithFiles.allSelectedCountProperty().addListener(this::selectedCountChanged);

        setTableLoadersAndHandlers(tableWithFiles);

        removeSelectedButton.disableProperty().bind(tableWithFiles.allSelectedCountProperty().isEqualTo(0));
    }

    private void updateSelectedLabelText(int selected) {
        selectedLabel.setText(
                GuiUtils.getTextDependingOnTheCount(
                        selected,
                        "1 file selected",
                        "%d files selected"
                )
        );
    }

    private void updateActionButtons(int allSelectedCount, int selectedUnavailableCount) {
        boolean disable = false;
        Tooltip tooltip = null;
        if (allSelectedCount == 0) {
            disable = true;
            tooltip = GuiUtils.generateTooltip("Please select at least one file");
        } else if (selectedUnavailableCount > 0) {
            disable = true;
            tooltip = GuiUtils.generateTooltip("Please select only available files");
        }

        autoSelectButton.setDisable(disable);
        Tooltip.install(autoSelectButtonWrapper, tooltip);

        loadAllSubtitlesButton.setDisable(disable);
        Tooltip.install(loadAllSubtitlesButtonWrapper, tooltip);

        goButton.setDisable(disable);
        Tooltip.install(goButtonWrapper, tooltip);
    }

    private void selectedCountChanged(Observable observable) {
        updateSelectedLabelText(tableWithFiles.getAllSelectedCount());
        updateActionButtons(tableWithFiles.getAllSelectedCount(), tableWithFiles.getSelectedUnavailableCount());
    }

    private void setTableLoadersAndHandlers(TableWithFiles table) {
        setAllSelectedHandler(table);
        setSortByChangeHandler(table);
        setSortDirectionChangeHandler(table);

        setRemoveSubtitleOptionHandler(table);
        setSingleSubtitleLoader(table);
        setSubtitleOptionPreviewHandler(table);
        setAddFileWithSubtitlesHandler(table);
        setAllFileSubtitleLoader(table);
        setMergedSubtitlePreviewHandler(table);
    }

    private void setAllSelectedHandler(TableWithFiles tableWithFiles) {
        tableWithFiles.setAllSelectedHandler(allSelected -> {
            BackgroundRunner<Void> backgroundRunner = runnerManager -> {
                runnerManager.setIndeterminateProgress();
                runnerManager.updateMessage("processing files...");

                for (TableFileInfo fileInfo : tableWithFiles.getItems()) {
                    boolean selected;
                    if (tableWithFiles.getMode() == TableWithFiles.Mode.SEPARATE_FILES) {
                        selected = allSelected;
                    } else {
                        selected = allSelected && fileInfo.getUnavailabilityReason() == null;
                    }

                    Platform.runLater(() -> tableWithFiles.setSelected(selected, fileInfo));
                }

                return null;
            };

            runInBackground(backgroundRunner, (result) -> {});
        });
    }

    private void setSortByChangeHandler(TableWithFiles tableWithFiles) {
        tableWithFiles.setSortByChangeHandler(tableSortBy -> {
            generalResult.clear();
            clearLastProcessedResult();

            try {
                context.getSettings().saveSortBy(sortByFrom(tableSortBy).toString());
            } catch (GuiSettings.ConfigException e) {
                log.error("failed to save sort by, should not happen: " + ExceptionUtils.getStackTrace(e));
            }

            SortHideUnavailableRunner backgroundRunner = new SortHideUnavailableRunner(
                    allTableFilesInfo,
                    tableWithFiles.getMode(),
                    hideUnavailableCheckbox.isSelected(),
                    context.getSettings().getSortBy(),
                    context.getSettings().getSortDirection()
            );

            BackgroundRunnerCallback<TableFilesToShowInfo> callback = result ->
                    tableWithFiles.setFilesInfo(
                            result.getFilesInfo(),
                            getTableSortBy(context.getSettings()),
                            getTableSortDirection(context.getSettings()),
                            result.getAllSelectableCount(),
                            result.getSelectedAvailableCount(),
                            result.getSelectedUnavailableCount(),
                            tableWithFiles.getMode(),
                            false
                    );

            runInBackground(backgroundRunner, callback);
        });
    }

    private void clearLastProcessedResult() {
        if (lastProcessedFileInfo != null) {
            tableWithFiles.clearActionResult(lastProcessedFileInfo);
        }
    }

    private static GuiSettings.SortBy sortByFrom(TableWithFiles.SortBy tableSortBy) {
        return EnumUtils.getEnum(GuiSettings.SortBy.class, tableSortBy.toString());
    }

    private static TableWithFiles.SortBy getTableSortBy(GuiSettings settings) {
        return EnumUtils.getEnum(TableWithFiles.SortBy.class, settings.getSortBy().toString());
    }

    private static TableWithFiles.SortDirection getTableSortDirection(GuiSettings settings) {
        return EnumUtils.getEnum(TableWithFiles.SortDirection.class, settings.getSortDirection().toString());
    }

    private void setSortDirectionChangeHandler(TableWithFiles tableWithFiles) {
        tableWithFiles.setSortDirectionChangeHandler(tableSortDirection -> {
            generalResult.clear();
            clearLastProcessedResult();

            try {
                context.getSettings().saveSortDirection(sortDirectionFrom(tableSortDirection).toString());
            } catch (GuiSettings.ConfigException e) {
                log.error("failed to save sort direction, should not happen: " + ExceptionUtils.getStackTrace(e));
            }

            SortHideUnavailableRunner backgroundRunner = new SortHideUnavailableRunner(
                    allTableFilesInfo,
                    tableWithFiles.getMode(),
                    hideUnavailableCheckbox.isSelected(),
                    context.getSettings().getSortBy(),
                    context.getSettings().getSortDirection()
            );

            BackgroundRunnerCallback<TableFilesToShowInfo> callback = result ->
                    tableWithFiles.setFilesInfo(
                            result.getFilesInfo(),
                            getTableSortBy(context.getSettings()),
                            getTableSortDirection(context.getSettings()),
                            result.getAllSelectableCount(),
                            result.getSelectedAvailableCount(),
                            result.getSelectedUnavailableCount(),
                            tableWithFiles.getMode(),
                            false
                    );

            runInBackground(backgroundRunner, callback);
        });
    }

    private static GuiSettings.SortDirection sortDirectionFrom(TableWithFiles.SortDirection tableSortDirection) {
        return EnumUtils.getEnum(GuiSettings.SortDirection.class, tableSortDirection.toString());
    }

    private void setRemoveSubtitleOptionHandler(TableWithFiles tableWithFiles) {
        tableWithFiles.setRemoveSubtitleOptionHandler((tableSubtitleOption, tableFileInfo) -> {
            generalResult.clear();
            clearLastProcessedResult();
            tableWithFiles.clearActionResult(tableFileInfo);
            lastProcessedFileInfo = tableFileInfo;

            FileInfo fileInfo = FileInfo.getById(tableFileInfo.getId(), filesInfo);
            fileInfo.getSubtitleOptions().removeIf(
                    option -> Objects.equals(option.getId(), tableSubtitleOption.getId())
            );

            tableWithFiles.removeFileWithSubtitles(tableSubtitleOption, tableFileInfo);
        });
    }

    private void setSingleSubtitleLoader(TableWithFiles tableWithFiles) {
        tableWithFiles.setSingleSubtitleLoader((tableSubtitleOption, tableFileInfo) -> {
            generalResult.clear();
            clearLastProcessedResult();
            tableWithFiles.clearActionResult(tableFileInfo);
            lastProcessedFileInfo = tableFileInfo;

            FileInfo fileInfo = FileInfo.getById(tableFileInfo.getId(), filesInfo);
            FfmpegSubtitleStream ffmpegStream = FfmpegSubtitleStream.getById(
                    tableSubtitleOption.getId(),
                    fileInfo.getFfmpegSubtitleStreams()
            );

            LoadSingleSubtitlesRunner backgroundRunner = new LoadSingleSubtitlesRunner(
                    ffmpegStream,
                    fileInfo,
                    tableSubtitleOption,
                    tableFileInfo,
                    tableWithFiles,
                    context.getFfmpeg()
            );

            BackgroundRunnerCallback<ActionResult> callback = actionResult ->
                    tableWithFiles.setActionResult(actionResult, tableFileInfo);

            runInBackground(backgroundRunner, callback);
        });
    }

    private void setSubtitleOptionPreviewHandler(TableWithFiles tableWithFiles) {
        tableWithFiles.setSubtitleOptionPreviewHandler((tableSubtitleOption, tableFileInfo) -> {
            generalResult.clear();
            clearLastProcessedResult();
            tableWithFiles.clearActionResult(tableFileInfo);

            FileInfo fileInfo = FileInfo.getById(tableFileInfo.getId(), filesInfo);
            SubtitleOption subtitleOption = SubtitleOption.getById(
                    tableSubtitleOption.getId(),
                    fileInfo.getSubtitleOptions()
            );

            NodeAndController nodeAndController = GuiUtils.loadNodeAndController(
                    "/gui/application_specific/subtitlePreview.fxml"
            );

            Stage previewStage = GuiUtils.createPopupStage(
                    "Subtitle preview",
                    nodeAndController.getNode(),
                    stage
            );
            SubtitlePreviewController controller = getInitializedOptionPreviewController(
                    nodeAndController,
                    subtitleOption,
                    fileInfo,
                    previewStage
            );

            previewStage.showAndWait();

            if (subtitleOption instanceof FileWithSubtitles) {
                FileWithSubtitles fileWithSubtitles = (FileWithSubtitles) subtitleOption;

                fileWithSubtitles.setEncoding(controller.getUserSelection().getEncoding());
                fileWithSubtitles.setSubtitles(controller.getUserSelection().getSubtitles());

                tableWithFiles.subtitleOptionPreviewClosed(
                        controller.getUserSelection().getSubtitles() != null
                                ? null
                                : TableSubtitleOption.UnavailabilityReason.INCORRECT_FORMAT,
                        tableSubtitleOption);
            }
        });
    }

    private SubtitlePreviewController getInitializedOptionPreviewController(
            NodeAndController nodeAndController,
            SubtitleOption subtitleOption,
            FileInfo fileInfo,
            Stage dialogStage
    ) {
        SubtitlePreviewController result = nodeAndController.getController();

        if (subtitleOption instanceof FfmpegSubtitleStream) {
            FfmpegSubtitleStream ffmpegStream = (FfmpegSubtitleStream) subtitleOption;

            String title = fileInfo.getFile().getName()
                    + ", " + GuiUtils.languageToString(ffmpegStream.getLanguage()).toUpperCase()
                    + (!StringUtils.isBlank(ffmpegStream.getTitle()) ? " " + ffmpegStream.getTitle() : "");

            result.initializeSimple(
                    subtitleOption.getSubtitles(),
                    title,
                    dialogStage
            );
        } else if (subtitleOption instanceof FileWithSubtitles) {
            FileWithSubtitles fileWithSubtitles = (FileWithSubtitles) subtitleOption;

            result.initializeWithEncoding(
                    fileWithSubtitles.getRawData(),
                    fileWithSubtitles.getEncoding(),
                    fileInfo.getFile().getAbsolutePath(),
                    dialogStage
            );
        } else {
            throw new IllegalStateException();
        }

        return result;
    }

    private void setAddFileWithSubtitlesHandler(TableWithFiles tableWithFiles) {
        tableWithFiles.setAddFileWithSubtitlesHandler(tableFileInfo -> {
            generalResult.clear();
            clearLastProcessedResult();
            tableWithFiles.clearActionResult(tableFileInfo);
            lastProcessedFileInfo = tableFileInfo;

            FileInfo videoFileInfo = FileInfo.getById(tableFileInfo.getId(), filesInfo);

            File fileWithSubtitlesToAdd = getFile(videoFileInfo, stage, context.getSettings()).orElse(null);
            if (fileWithSubtitlesToAdd == null) {
                return;
            }

            try {
                if (fileWithSubtitlesToAdd.getParent() != null) {
                    context.getSettings().saveLastDirectoryWithExternalSubtitles(fileWithSubtitlesToAdd.getParent());
                }
            } catch (GuiSettings.ConfigException e) {
                log.error(
                        "failed to save last directory , file " + fileWithSubtitlesToAdd.getAbsolutePath() + ": "
                                + ExceptionUtils.getStackTrace(e)
                );
            }

            AddFileWithSubtitlesRunner backgroundRunner = new AddFileWithSubtitlesRunner(
                    fileWithSubtitlesToAdd,
                    videoFileInfo
            );

            BackgroundRunnerCallback<AddFileWithSubtitlesRunner.Result> callback = result -> {
                if (result.getUnavailabilityReason() != null) {
                    tableWithFiles.failedToAddFileWithSubtitles(result.getUnavailabilityReason(), tableFileInfo);
                } else {
                    videoFileInfo.getSubtitleOptions().add(result.getFileWithSubtitles());

                    tableWithFiles.addFileWithSubtitles(
                            result.getFileWithSubtitles().getId(),
                            result.getFileWithSubtitles().getFile().getAbsolutePath(),
                            result.getFileWithSubtitles().getSubtitles() == null,
                            result.getSize(),
                            tableFileInfo
                    );
                }
            };

            runInBackground(backgroundRunner, callback);
        });
    }

    private Optional<File> getFile(FileInfo fileInfo, Stage stage, GuiSettings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose a file with the subtitles");

        File initialDirectory = settings.getLastDirectoryWithExternalSubtitles();
        if (initialDirectory == null) {
            File directoryWithFile = fileInfo.getFile().getParentFile();
            if (directoryWithFile != null && directoryWithFile.isDirectory()) {
                initialDirectory = directoryWithFile;
            }
        }

        fileChooser.setInitialDirectory(initialDirectory);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("subrip files (*.srt)", "*.srt")
        );

        return Optional.ofNullable(fileChooser.showOpenDialog(stage));
    }

    private void setAllFileSubtitleLoader(TableWithFiles tableWithFiles) {
        tableWithFiles.setAllFileSubtitleLoader(tableFileInfo -> {
            generalResult.clear();
            clearLastProcessedResult();
            tableWithFiles.clearActionResult(tableFileInfo);
            lastProcessedFileInfo = tableFileInfo;

            FileInfo fileInfo = FileInfo.getById(tableFileInfo.getId(), filesInfo);

            SingleFileAllSubtitleLoader backgroundRunner = new SingleFileAllSubtitleLoader(
                    fileInfo,
                    tableFileInfo,
                    tableWithFiles,
                    context.getFfmpeg()
            );

            BackgroundRunnerCallback<ActionResult> callback = actionResult ->
                    tableWithFiles.setActionResult(actionResult, tableFileInfo);

            runInBackground(backgroundRunner, callback);
        });
    }

    private void setMergedSubtitlePreviewHandler(TableWithFiles tableWithFiles) {
        tableWithFiles.setMergedSubtitlePreviewHandler(tableFileInfo -> {
            generalResult.clear();
            clearLastProcessedResult();
            tableWithFiles.clearActionResult(tableFileInfo);

            FileInfo fileInfo = FileInfo.getById(tableFileInfo.getId(), filesInfo);
            SubtitleOption upperOption = SubtitleOption.getById(
                    tableFileInfo.getUpperOption().getId(),
                    fileInfo.getSubtitleOptions()
            );
            SubtitleOption lowerOption = SubtitleOption.getById(
                    tableFileInfo.getLowerOption().getId(),
                    fileInfo.getSubtitleOptions()
            );

            MergedPreviewRunner mergedPreviewRunner = new MergedPreviewRunner(
                    upperOption,
                    lowerOption,
                    fileInfo,
                    tableFileInfo,
                    tableWithFiles,
                    context.getFfmpeg()
            );

            BackgroundRunnerCallback<MergedPreviewRunner.Result> callback = result -> {
                fileInfo.setMergedSubtitleInfo(result.getMergedSubtitleInfo());

                NodeAndController nodeAndController = GuiUtils.loadNodeAndController(
                        "/gui/application_specific/subtitlePreview.fxml"
                );

                Stage previewStage = GuiUtils.createPopupStage(
                        "Subtitle preview",
                        nodeAndController.getNode(),
                        stage
                );
                SubtitlePreviewController controller = nodeAndController.getController();
                controller.initializeMerged(
                        fileInfo.getMergedSubtitleInfo().getSubtitles(),
                        getOptionTitleForPreview(upperOption),
                        getOptionTitleForPreview(lowerOption),
                        previewStage
                );

                previewStage.showAndWait();
            };

            runInBackground(mergedPreviewRunner, callback);
        });
    }

    private static String getOptionTitleForPreview(SubtitleOption option) {
        if (option instanceof FfmpegSubtitleStream) {
            FfmpegSubtitleStream ffmpegStream = (FfmpegSubtitleStream) option;

            return GuiUtils.languageToString(ffmpegStream.getLanguage()).toUpperCase()
                    + (!StringUtils.isBlank(ffmpegStream.getTitle()) ? " " + ffmpegStream.getTitle() : "");
        } else if (option instanceof FileWithSubtitles) {
            FileWithSubtitles fileWithSubtitles = (FileWithSubtitles) option;

            return fileWithSubtitles.getFile().getAbsolutePath();
        } else {
            throw new IllegalStateException();
        }
    }

    void show() {
        contentPane.setVisible(true);
    }

    void hide() {
        contentPane.setVisible(false);
    }

    void handleChosenFiles(List<File> files) {
        try {
            context.getSettings().saveLastDirectoryWithVideos(files.get(0).getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error(
                    "failed to save last directory with videos, shouldn't happen: "
                            + ExceptionUtils.getStackTrace(e)
            );
        }

        context.setWorkWithVideosInProgress(true);
        GuiUtils.setVisibleAndManaged(addRemoveFilesPane, true);
        hideUnavailableCheckbox.setSelected(false);
        directoryPath = null;

        LoadSeparateFilesRunner backgroundRunner = new LoadSeparateFilesRunner(
                files,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context
        );

        BackgroundRunnerCallback<LoadSeparateFilesRunner.Result> callback = result -> {
            filesInfo = result.getFilesInfo();
            allTableFilesInfo = result.getAllTableFilesInfo();

            tableWithFiles.setFilesInfo(
                    result.getTableFilesToShowInfo().getFilesInfo(),
                    getTableSortBy(context.getSettings()),
                    getTableSortDirection(context.getSettings()),
                    result.getTableFilesToShowInfo().getAllSelectableCount(),
                    result.getTableFilesToShowInfo().getSelectedAvailableCount(),
                    result.getTableFilesToShowInfo().getSelectedUnavailableCount(),
                    TableWithFiles.Mode.SEPARATE_FILES,
                    true
            );
        };

        runInBackground(backgroundRunner, callback);
    }

    void handleChosenDirectory(File directory) {
        context.setWorkWithVideosInProgress(true);
        GuiUtils.setVisibleAndManaged(chosenDirectoryPane, true);

        processDirectoryPath(directory.getAbsolutePath(), FileOrigin.FILE_CHOOSER);
    }

    private void processDirectoryPath(String path, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && path.equals(ObjectUtils.firstNonNull(directoryPath, ""))) {
            return;
        }

        directoryPath = path;

        chosenDirectoryField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        if (fileOrigin == FileOrigin.FILE_CHOOSER) {
            chosenDirectoryField.setText(path);
        }
        refreshButton.setDisable(false);
        generalResult.clear();
        tableAndActionsPane.setDisable(false);
        tableWithFiles.clearTable();
        lastProcessedFileInfo = null;

        try {
            if (fileOrigin == FileOrigin.FILE_CHOOSER) {
                context.getSettings().saveLastDirectoryWithVideos(path);
            }
        } catch (GuiSettings.ConfigException e) {
            log.error(
                    "failed to save last directory with videos, shouldn't happen: "
                            + ExceptionUtils.getStackTrace(e)
            );
        }

        LoadDirectoryRunner backgroundRunner = new LoadDirectoryRunner(
                directoryPath,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context
        );

        BackgroundRunnerCallback<LoadDirectoryRunner.Result> callback = result -> {
            if (result.getUnavailabilityReason() != null) {
                filesInfo = null;
                allTableFilesInfo = null;

                chosenDirectoryField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                refreshButton.setDisable(result.isDisableRefresh());
                generalResult.setOnlyError(result.getUnavailabilityReason());
                tableAndActionsPane.setDisable(true);
            } else {
                filesInfo = result.getFilesInfo();
                allTableFilesInfo = result.getAllTableFilesInfo();

                hideUnavailableCheckbox.setSelected(result.isHideUnavailable());
                tableWithFiles.setFilesInfo(
                        result.getTableFilesToShowInfo().getFilesInfo(),
                        getTableSortBy(context.getSettings()),
                        getTableSortDirection(context.getSettings()),
                        result.getTableFilesToShowInfo().getAllSelectableCount(),
                        result.getTableFilesToShowInfo().getSelectedAvailableCount(),
                        result.getTableFilesToShowInfo().getSelectedUnavailableCount(),
                        TableWithFiles.Mode.DIRECTORY,
                        true
                );
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void backToSelectionClicked() {
        GuiUtils.setVisibleAndManaged(chosenDirectoryPane, false);
        chosenDirectoryField.setText(null);
        GuiUtils.setVisibleAndManaged(addRemoveFilesPane, false);

        generalResult.clear();
        tableAndActionsPane.setDisable(false);
        tableWithFiles.clearTable();
        lastProcessedFileInfo = null;

        context.setWorkWithVideosInProgress(false);
        videosTabController.setActivePane(VideosTabController.ActivePane.CHOICE);
    }

    @FXML
    private void refreshButtonClicked() {
        chosenDirectoryField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        refreshButton.setDisable(false);
        generalResult.clear();
        tableAndActionsPane.setDisable(false);
        tableWithFiles.clearTable();
        lastProcessedFileInfo = null;

        LoadDirectoryRunner backgroundRunner = new LoadDirectoryRunner(
                directoryPath,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context
        );

        BackgroundRunnerCallback<LoadDirectoryRunner.Result> callback = result -> {
            if (result.getUnavailabilityReason() != null) {
                filesInfo = null;
                allTableFilesInfo = null;

                chosenDirectoryField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                generalResult.setOnlyError(result.getUnavailabilityReason());
                tableAndActionsPane.setDisable(true);
            } else {
                filesInfo = result.getFilesInfo();
                allTableFilesInfo = result.getAllTableFilesInfo();

                hideUnavailableCheckbox.setSelected(result.isHideUnavailable());
                tableWithFiles.setFilesInfo(
                        result.getTableFilesToShowInfo().getFilesInfo(),
                        getTableSortBy(context.getSettings()),
                        getTableSortDirection(context.getSettings()),
                        result.getTableFilesToShowInfo().getAllSelectableCount(),
                        result.getTableFilesToShowInfo().getSelectedAvailableCount(),
                        result.getTableFilesToShowInfo().getSelectedUnavailableCount(),
                        TableWithFiles.Mode.DIRECTORY,
                        true
                );

                /* See the huge comment in the hideUnavailableClicked() method. */
                tableWithFiles.scrollTo(0);
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void hideUnavailableClicked() {
        generalResult.clear();
        clearLastProcessedResult();

        SortHideUnavailableRunner backgroundRunner = new SortHideUnavailableRunner(
                allTableFilesInfo,
                tableWithFiles.getMode(),
                hideUnavailableCheckbox.isSelected(),
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection()
        );

        BackgroundRunnerCallback<TableFilesToShowInfo> callback = result -> {
            tableWithFiles.setFilesInfo(
                    result.getFilesInfo(),
                    getTableSortBy(context.getSettings()),
                    getTableSortDirection(context.getSettings()),
                    result.getAllSelectableCount(),
                    result.getSelectedAvailableCount(),
                    result.getSelectedUnavailableCount(),
                    tableWithFiles.getMode(),
                    false
            );

            /*
             * There is a strange bug with TableView - when the list is shrunk in size (because for example
             * "hide unavailable" checkbox is checked but it can also happen when refresh is clicked I suppose) and both
             * big list and shrunk list have vertical scrollbars table isn't shrunk unless you move the scrollbar.
             * I've tried many workaround but this one seems the best so far - just show the beginning of the table.
             * I couldn't find a bug with precise description but these ones fit quite well -
             * https://bugs.openjdk.java.net/browse/JDK-8095384, https://bugs.openjdk.java.net/browse/JDK-8087833.
             */
            tableWithFiles.scrollTo(0);
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void autoSelectButtonClicked() {
        generalResult.clear();
        lastProcessedFileInfo = null;

        AutoSelectSubtitlesRunner backgroundRunner = new AutoSelectSubtitlesRunner(
                tableWithFiles.getItems(),
                filesInfo,
                tableWithFiles,
                context.getFfmpeg(),
                context.getSettings()
        );

        BackgroundRunnerCallback<ActionResult> callback = actionResult -> generalResult.set(actionResult);

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void loadAllSubtitlesClicked() {
        generalResult.clear();
        lastProcessedFileInfo = null;

        MultipleFilesAllSubtitleLoader backgroundRunner = new MultipleFilesAllSubtitleLoader(
                tableWithFiles.getItems(),
                filesInfo,
                tableWithFiles,
                context.getFfmpeg()
        );

        BackgroundRunnerCallback<ActionResult> callback = actionResult -> generalResult.set(actionResult);

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void goButtonClicked() {
        generalResult.clear();
        lastProcessedFileInfo = null;

        MergePreparationRunner preparationRunner = new MergePreparationRunner(
                tableWithFiles.getItems(),
                filesInfo,
                tableWithFiles,
                context.getSettings()
        );

        BackgroundRunnerCallback<MergePreparationRunner.Result> callback = preparationResult -> {
            String agreementMessage = getFreeSpaceAgreementMessage(preparationResult).orElse(null);
            if (agreementMessage != null) {
                if (!GuiUtils.showAgreementPopup(agreementMessage, "yes", "no", stage)) {
                    return;
                }
            }

            List<File> confirmedFilesToOverwrite = null;
            if (!CollectionUtils.isEmpty(preparationResult.getFilesToOverwrite())) {
                confirmedFilesToOverwrite = getConfirmedFilesToOverwrite(preparationResult.getFilesToOverwrite());
            }
        };

        runInBackground(preparationRunner, callback);
    }

    private static Optional<String> getFreeSpaceAgreementMessage(MergePreparationRunner.Result preparationResult) {
      if (preparationResult.getRequiredTempSpace() != null) {
          if (preparationResult.getRequiredTempSpace() <= preparationResult.getAvailableTempSpace()) {
              return Optional.empty();
          }

          return Optional.of(
                  "Merge requires approximately "
                          + GuiUtils.getFileSizeTextual(preparationResult.getRequiredTempSpace(), false)
                          + " of free disk space during the process but only "
                          + GuiUtils.getFileSizeTextual(preparationResult.getAvailableTempSpace(), false)
                          + " is available, proceed anyway?"
          );
      } else if (preparationResult.getRequiredPermanentSpace() != null) {
          return Optional.of(
                  "Approximately "
                          + GuiUtils.getFileSizeTextual(preparationResult.getRequiredTempSpace(), false)
                          + " of free disk space will be used, do you want to proceed?"
          );
      } else {
          return Optional.empty();
      }
    }

    private static List<File> getConfirmedFilesToOverwrite(List<File> allFilesToOverwrite) {
        for (File file : allFilesToOverwrite) {

        }

        return new ArrayList<>();
    }

    @FXML
    private void removeButtonClicked() {
        generalResult.clear();
        clearLastProcessedResult();

        RemoveFilesRunner backgroundRunner = new RemoveFilesRunner(
                filesInfo,
                tableWithFiles.getMode(),
                allTableFilesInfo,
                tableWithFiles.getItems()
        );

        BackgroundRunnerCallback<RemoveFilesRunner.Result> callback = result -> {
            filesInfo = result.getFilesInfo();
            allTableFilesInfo = result.getAllTableFilesInfo();

            tableWithFiles.setFilesInfo(
                    result.getTableFilesToShowInfo().getFilesInfo(),
                    getTableSortBy(context.getSettings()),
                    getTableSortDirection(context.getSettings()),
                    result.getTableFilesToShowInfo().getAllSelectableCount(),
                    result.getTableFilesToShowInfo().getSelectedAvailableCount(),
                    result.getTableFilesToShowInfo().getSelectedUnavailableCount(),
                    tableWithFiles.getMode(),
                    false
            );

            if (result.getRemovedCount() == 0) {
                log.error("nothing has been removed, that shouldn't happen");
                throw new IllegalStateException();
            } else if (result.getRemovedCount() == 1) {
                generalResult.setOnlySuccess("File has been removed from the list successfully");
            } else {
                generalResult.setOnlySuccess(
                        result.getRemovedCount() + " files have been removed from the list successfully"
                );
            }
        };

        runInBackground(backgroundRunner, callback);
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
            log.error(
                    "failed to save last directory with videos, that shouldn't happen: "
                            + ExceptionUtils.getStackTrace(e)
            );
        }

        AddFilesRunner backgroundRunner = new AddFilesRunner(
                filesInfo,
                filesToAdd,
                allTableFilesInfo,
                tableWithFiles.getMode(),
                hideUnavailableCheckbox.isSelected(),
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context
        );

        BackgroundRunnerCallback<AddFilesRunner.Result> callback = result -> {
            if (!StringUtils.isBlank(result.getAddFailedReason())) {
                generalResult.set(ActionResult.onlyError(result.getAddFailedReason()));
            } else {
                filesInfo = result.getFilesInfo();
                allTableFilesInfo = result.getAllTableFilesInfo();

                tableWithFiles.setFilesInfo(
                        result.getTableFilesToShowInfo().getFilesInfo(),
                        getTableSortBy(context.getSettings()),
                        getTableSortDirection(context.getSettings()),
                        result.getTableFilesToShowInfo().getAllSelectableCount(),
                        result.getTableFilesToShowInfo().getSelectedAvailableCount(),
                        result.getTableFilesToShowInfo().getSelectedUnavailableCount(),
                        tableWithFiles.getMode(),
                        false
                );

                generalResult.set(AddFilesRunner.generateActionResult(result));
            }
        };

        runInBackground(backgroundRunner, callback);
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
}
