package kirill.subtitlemerger.gui.tabs.videos;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.utils.entities.AbstractController;
import kirill.subtitlemerger.gui.utils.forms_and_controls.SubtitlePreviewController;
import kirill.subtitlemerger.gui.tabs.videos.background.*;
import kirill.subtitlemerger.gui.tabs.videos.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.table_with_files.TableSubtitleOption;
import kirill.subtitlemerger.gui.tabs.videos.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundCallback;
import kirill.subtitlemerger.gui.utils.forms_and_controls.ActionResultLabels;
import kirill.subtitlemerger.gui.utils.forms_and_controls.AgreementPopupController;
import kirill.subtitlemerger.gui.utils.entities.ActionResult;
import kirill.subtitlemerger.gui.utils.entities.FileOrigin;
import kirill.subtitlemerger.gui.utils.entities.NodeInfo;
import kirill.subtitlemerger.logic.files.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.files.entities.FileInfo;
import kirill.subtitlemerger.logic.files.entities.FileWithSubtitles;
import kirill.subtitlemerger.logic.files.entities.SubtitleOption;
import kirill.subtitlemerger.logic.settings.SettingException;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.settings.SortBy;
import kirill.subtitlemerger.logic.settings.SortDirection;
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
    private Pane mergeButtonWrapper;

    @FXML
    private Button mergeButton;

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

        mergeButton.setDisable(disable);
        Tooltip.install(mergeButtonWrapper, tooltip);
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
            BackgroundRunner<Void> backgroundRunner = backgroundManager -> {
                backgroundManager.setIndeterminateProgress();
                backgroundManager.updateMessage("Processing files...");

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
            } catch (SettingException e) {
                log.error("failed to save sort by, should not happen: " + ExceptionUtils.getStackTrace(e));
            }

            SortHideUnavailableRunner backgroundRunner = new SortHideUnavailableRunner(
                    allTableFilesInfo,
                    tableWithFiles.getMode(),
                    hideUnavailableCheckbox.isSelected(),
                    context.getSettings().getSortBy(),
                    context.getSettings().getSortDirection()
            );

            BackgroundCallback<TableFilesToShowInfo> callback = result ->
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

    private static SortBy sortByFrom(TableWithFiles.SortBy tableSortBy) {
        return EnumUtils.getEnum(SortBy.class, tableSortBy.toString());
    }

    private static TableWithFiles.SortBy getTableSortBy(Settings settings) {
        return EnumUtils.getEnum(TableWithFiles.SortBy.class, settings.getSortBy().toString());
    }

    private static TableWithFiles.SortDirection getTableSortDirection(Settings settings) {
        return EnumUtils.getEnum(TableWithFiles.SortDirection.class, settings.getSortDirection().toString());
    }

    private void setSortDirectionChangeHandler(TableWithFiles tableWithFiles) {
        tableWithFiles.setSortDirectionChangeHandler(tableSortDirection -> {
            generalResult.clear();
            clearLastProcessedResult();

            try {
                context.getSettings().saveSortDirection(sortDirectionFrom(tableSortDirection).toString());
            } catch (SettingException e) {
                log.error("failed to save sort direction, should not happen: " + ExceptionUtils.getStackTrace(e));
            }

            SortHideUnavailableRunner backgroundRunner = new SortHideUnavailableRunner(
                    allTableFilesInfo,
                    tableWithFiles.getMode(),
                    hideUnavailableCheckbox.isSelected(),
                    context.getSettings().getSortBy(),
                    context.getSettings().getSortDirection()
            );

            BackgroundCallback<TableFilesToShowInfo> callback = result ->
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

    private static SortDirection sortDirectionFrom(TableWithFiles.SortDirection tableSortDirection) {
        return EnumUtils.getEnum(SortDirection.class, tableSortDirection.toString());
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

            BackgroundCallback<ActionResult> callback = actionResult ->
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

            NodeInfo nodeInfo = GuiUtils.loadNode("/gui/javafx/forms_and_controls/subtitle_preview.fxml");

            Stage previewStage = GuiUtils.createPopupStage("Subtitle preview", nodeInfo.getNode(), stage);
            SubtitlePreviewController controller = getInitializedOptionPreviewController(
                    nodeInfo,
                    subtitleOption,
                    fileInfo,
                    previewStage
            );

            previewStage.showAndWait();

            if (subtitleOption instanceof FileWithSubtitles) {
                FileWithSubtitles fileWithSubtitles = (FileWithSubtitles) subtitleOption;

                fileWithSubtitles.changeEncoding(
                        controller.getUserSelection().getEncoding(),
                        controller.getUserSelection().getSubtitles()
                );

                tableWithFiles.subtitleOptionPreviewClosed(
                        controller.getUserSelection().getSubtitles() != null
                                ? null
                                : TableSubtitleOption.UnavailabilityReason.INCORRECT_FORMAT,
                        tableSubtitleOption);
            }
        });
    }

    private SubtitlePreviewController getInitializedOptionPreviewController(
            NodeInfo nodeInfo,
            SubtitleOption subtitleOption,
            FileInfo fileInfo,
            Stage dialogStage
    ) {
        SubtitlePreviewController result = nodeInfo.getController();

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
                    context.getSettings().saveExternalDirectory(fileWithSubtitlesToAdd.getParent());
                }
            } catch (SettingException e) {
                log.warn("failed to save last directory:" + ExceptionUtils.getStackTrace(e));
            }

            AddFileWithSubtitlesRunner backgroundRunner = new AddFileWithSubtitlesRunner(
                    fileWithSubtitlesToAdd,
                    videoFileInfo
            );

            BackgroundCallback<AddFileWithSubtitlesRunner.Result> callback = result -> {
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

    private Optional<File> getFile(FileInfo fileInfo, Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose a file with the subtitles");

        File initialDirectory = settings.getExternalDirectory();
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

            BackgroundCallback<ActionResult> callback = actionResult ->
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

            BackgroundCallback<MergedPreviewRunner.Result> callback = result -> {
                if (result.isCanceled()) {
                    generalResult.setOnlyWarn("Merge has been cancelled");
                    return;
                }

                fileInfo.setMergedSubtitleInfo(result.getMergedSubtitleInfo());

                NodeInfo nodeInfo = GuiUtils.loadNode("/gui/javafx/forms_and_controls/subtitle_preview.fxml");

                Stage previewStage = GuiUtils.createPopupStage("Subtitle preview", nodeInfo.getNode(), stage);
                SubtitlePreviewController controller = nodeInfo.getController();
                controller.initializeMerged(
                        fileInfo.getMergedSubtitleInfo().getSubtitles(),
                        getOptionTitleForPreview(upperOption),
                        getOptionTitleForPreview(lowerOption),
                        context.getSettings().isPlainTextSubtitles(),
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
            context.getSettings().saveVideosDirectory(files.get(0).getParent());
        } catch (SettingException e) {
            log.warn("failed to save last directory with videos: " + ExceptionUtils.getStackTrace(e));
        }

        context.setVideosInProgress(true);
        GuiUtils.setVisibleAndManaged(addRemoveFilesPane, true);
        hideUnavailableCheckbox.setSelected(false);
        directoryPath = null;

        LoadSeparateFilesRunner backgroundRunner = new LoadSeparateFilesRunner(
                files,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context
        );

        BackgroundCallback<LoadSeparateFilesRunner.Result> callback = result -> {
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
        context.setVideosInProgress(true);
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
                context.getSettings().saveVideosDirectory(path);
            }
        } catch (SettingException e) {
            log.warn("failed to save last directory with videos: " + ExceptionUtils.getStackTrace(e));
        }

        LoadDirectoryRunner backgroundRunner = new LoadDirectoryRunner(
                directoryPath,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                context
        );

        BackgroundCallback<LoadDirectoryRunner.Result> callback = result -> {
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

        context.setVideosInProgress(false);
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

        BackgroundCallback<LoadDirectoryRunner.Result> callback = result -> {
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

        BackgroundCallback<TableFilesToShowInfo> callback = result -> {
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

        BackgroundCallback<ActionResult> callback = actionResult -> generalResult.set(actionResult);

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

        BackgroundCallback<ActionResult> callback = actionResult -> generalResult.set(actionResult);

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void mergeButtonClicked() {
        generalResult.clear();
        lastProcessedFileInfo = null;

        MergePreparationRunner preparationRunner = new MergePreparationRunner(
                tableWithFiles.getItems(),
                filesInfo,
                tableWithFiles,
                context
        );

        BackgroundCallback<MergePreparationRunner.Result> callback = preparationResult -> {
            if (preparationResult.isCanceled()) {
                generalResult.setOnlyWarn("Merge has been cancelled");
                return;
            }

            if (preparationResult.getFilesWithoutSelectionCount() != 0) {
                generalResult.set(
                        getFilesWithoutSelectionResult(
                                preparationResult.getFilesWithoutSelectionCount(),
                                tableWithFiles.getAllSelectedCount()
                        )
                );
                return;
            }

            String agreementMessage = getFreeSpaceAgreementMessage(preparationResult).orElse(null);
            if (agreementMessage != null) {
                if (!GuiUtils.showAgreementPopup(agreementMessage, "yes", "no", stage)) {
                    generalResult.setOnlyWarn("Merge has been cancelled");
                    return;
                }
            }

            List<File> confirmedFilesToOverwrite = new ArrayList<>();
            if (!CollectionUtils.isEmpty(preparationResult.getFilesToOverwrite())) {
                confirmedFilesToOverwrite = getConfirmedFilesToOverwrite(preparationResult, stage).orElse(null);
                if (confirmedFilesToOverwrite == null) {
                    generalResult.setOnlyWarn("Merge has been cancelled");
                    return;
                }
            }

            MergeRunner mergeRunner = new MergeRunner(
                    preparationResult.getFilesMergeInfo(),
                    confirmedFilesToOverwrite,
                    preparationResult.getDirectoryForTempFile(),
                    tableWithFiles.getItems(),
                    filesInfo,
                    tableWithFiles,
                    context.getFfprobe(),
                    context.getFfmpeg(),
                    context.getSettings()
            );

            BackgroundCallback<MergeRunner.Result> mergeCallback = result -> {
                generalResult.set(result.getActionResult());

                tableWithFiles.setFilesInfo(
                        result.getTableFilesToShowInfo().getFilesInfo(),
                        getTableSortBy(context.getSettings()),
                        getTableSortDirection(context.getSettings()),
                        result.getTableFilesToShowInfo().getAllSelectableCount(),
                        result.getTableFilesToShowInfo().getSelectedAvailableCount(),
                        result.getTableFilesToShowInfo().getSelectedUnavailableCount(),
                        tableWithFiles.getMode(),
                        true
                );
            };

            runInBackground(mergeRunner, mergeCallback);
        };

        runInBackground(preparationRunner, callback);
    }

    private static ActionResult getFilesWithoutSelectionResult(
            int filesWithoutSelectionCount,
            int selectedFileCount
    ) {
        String message;
        if (selectedFileCount == 1) {
            message = "Merge for the file is unavailable because you have to select upper and lower subtitles first";
        } else {
            message = "Merge is unavailable because you have to select upper and lower subtitles for all the selected "
                    + "files (%d left)";
        }

        return ActionResult.onlyError(String.format(message, filesWithoutSelectionCount));
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
      } else {
          return Optional.empty();
      }
    }

    private static Optional<List<File>> getConfirmedFilesToOverwrite(
            MergePreparationRunner.Result mergePreparationResult,
            Stage stage
    ) {
        if (CollectionUtils.isEmpty(mergePreparationResult.getFilesToOverwrite())) {
            return Optional.of(new ArrayList<>());
        }

        List<File> result = new ArrayList<>();

        int filesToOverwriteLeft = mergePreparationResult.getFilesToOverwrite().size();
        for (File fileToOverwrite : mergePreparationResult.getFilesToOverwrite()) {
            String fileName = GuiUtils.getShortenedString(
                    fileToOverwrite.getName(),
                    0,
                    32
            );

            String applyToAllText;
            if (filesToOverwriteLeft == 1) {
                applyToAllText = null;
            } else {
                applyToAllText = String.format("Apply to all (%d left)", filesToOverwriteLeft);
            }

            AgreementPopupController.Result agreementResult = GuiUtils.showAgreementPopup(
                    "File '" + fileName + "' already exists. Do you want to overwrite it?",
                    "Yes",
                    "No",
                    applyToAllText,
                    stage
            );
            if (agreementResult == AgreementPopupController.Result.CANCELED) {
                return Optional.empty();
            } else if (agreementResult == AgreementPopupController.Result.YES) {
                result.add(fileToOverwrite);
            } else if (agreementResult == AgreementPopupController.Result.YES_TO_ALL) {
                List<File> filesLeft = mergePreparationResult.getFilesToOverwrite().subList(
                        mergePreparationResult.getFilesToOverwrite().size() - filesToOverwriteLeft,
                        mergePreparationResult.getFilesToOverwrite().size()
                );
                result.addAll(filesLeft);
                return Optional.of(result);
            } else if (agreementResult == AgreementPopupController.Result.NO_TO_ALL) {
                return Optional.of(result);
            }

            filesToOverwriteLeft--;
        }

        return Optional.of(result);
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

        BackgroundCallback<RemoveFilesRunner.Result> callback = result -> {
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
            context.getSettings().saveVideosDirectory(filesToAdd.get(0).getParent());
        } catch (SettingException e) {
            log.warn("failed to save last directory with videos: " + ExceptionUtils.getStackTrace(e));
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

        BackgroundCallback<AddFilesRunner.Result> callback = result -> {
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

    private static List<File> getFiles(Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("choose videos");
        fileChooser.setInitialDirectory(settings.getVideosDirectory());
        fileChooser.getExtensionFilters().add(GuiConstants.VIDEO_EXTENSION_FILTER);

        return fileChooser.showOpenMultipleDialog(stage);
    }
}
