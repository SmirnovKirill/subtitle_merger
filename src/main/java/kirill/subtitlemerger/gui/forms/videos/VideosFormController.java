package kirill.subtitlemerger.gui.forms.videos;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.common_controls.ActionResultPane;
import kirill.subtitlemerger.gui.forms.MainFormController;
import kirill.subtitlemerger.gui.forms.common.BackgroundTaskFormController;
import kirill.subtitlemerger.gui.forms.common.agreement_popup.AgreementResult;
import kirill.subtitlemerger.gui.forms.videos.background.*;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.*;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.Popups;
import kirill.subtitlemerger.gui.utils.background.BackgroundCallback;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.entities.FileOrigin;
import kirill.subtitlemerger.logic.settings.SettingException;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.settings.SortBy;
import kirill.subtitlemerger.logic.settings.SortDirection;
import kirill.subtitlemerger.logic.subtitles.SubRipWriter;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.ExternalSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@CommonsLog
public class VideosFormController extends BackgroundTaskFormController {
    @SuppressWarnings("unused")
    @FXML
    private MissingSettingsFormController missingSettingsFormController;

    @SuppressWarnings("unused")
    @FXML
    private ChoiceFormController choiceFormController;

    @FXML
    private Pane chosenDirectoryPane;

    @FXML
    private TextField chosenDirectoryField;

    @FXML
    private Button refreshButton;

    @FXML
    private ActionResultPane totalResult;

    @FXML
    private Pane tableAndActionsPane;

    @FXML
    private Label selectedCountLabel;

    @FXML
    private CheckBox hideUnavailableCheckbox;

    @FXML
    private Pane autoSelectButtonWrapper;

    @FXML
    private Button autoSelectButton;

    @FXML
    private Pane loadAllButtonWrapper;

    @FXML
    private Button loadAllButton;

    @FXML
    private Pane mergeButtonWrapper;

    @FXML
    private Button mergeButton;

    @FXML
    private TableWithVideos table;

    @FXML
    private Pane addRemoveVideosPane;

    @FXML
    private Button removeVideosButton;

    private MainFormController mainFormController;

    private Stage stage;

    private Settings settings;

    private GuiContext context;

    private String directoryPath;

    private List<VideoInfo> allVideosInfo;

    private List<TableVideoInfo> allTableVideosInfo;

    /*
     * Before performing a one-video operation it is better to clean the result of the previous one-video operation, it
     * will look better. We can't just clear all videos' results because it may take a lot of time if there are many
     * videos and it's unacceptable for generally fast one-video operations. So we will keep track of the last processed
     * video to clear its result only when starting the next one-video operation.
     */
    private TableVideoInfo lastProcessedVideoInfo;

    public void initialize(MainFormController mainFormController, Stage stage, GuiContext context) {
        this.mainFormController = mainFormController;
        this.stage = stage;
        this.settings = context.getSettings();
        this.context = context;

        missingSettingsFormController.initialize(this, context);
        choiceFormController.initialize(this, stage, context);

        context.getMissingSettings().addListener((InvalidationListener) observable -> setActivePane());
        setActivePane();

        setFormListenersAndBindings();
    }

    private void setActivePane() {
        boolean noMissingSettings = CollectionUtils.isEmpty(context.getMissingSettings());
        setActivePane(noMissingSettings ? ActivePane.CHOICE : ActivePane.MISSING_SETTINGS);
    }

    void setActivePane(ActivePane activePane) {
        if (activePane == ActivePane.MISSING_SETTINGS) {
            missingSettingsFormController.show();
            choiceFormController.hide();
            hide();
        } else if (activePane == ActivePane.CHOICE) {
            missingSettingsFormController.hide();
            choiceFormController.show();
            hide();
        } else if (activePane == ActivePane.MAIN) {
            missingSettingsFormController.hide();
            choiceFormController.hide();
            show();
        } else {
            log.error("unexpected active pane " + activePane + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private void hide() {
        mainPane.setVisible(false);
    }

    private void show() {
        mainPane.setVisible(true);
    }

    private void setFormListenersAndBindings() {
        GuiUtils.setTextEnteredHandler(
                chosenDirectoryField,
                (path) -> processDirectoryPath(path, FileOrigin.TEXT_FIELD)
        );

        selectedCountLabel.textProperty().bind(
                GuiUtils.getTextDependingOnCount(
                        table.selectedCountProperty(),
                        "1 file selected",
                        "%d files selected"
                )
        );

        table.selectedCountProperty().addListener(
                observable -> disableActionButtons(table.getSelectedCount(), table.getSelectedUnavailableCount())
        );
        disableActionButtons(table.getSelectedCount(), table.getSelectedUnavailableCount());

        table.setSelectAllHandler(this::handleSelectAll);
        table.setSortChangeHandler(this::handleSortChange);
        table.setRemoveSubtitleOptionHandler(this::handleRemoveOption);
        setSingleSubtitleLoader(table);
        table.setSubtitleOptionPreviewHandler(this::handleSubtitleOptionPreview);
        setAddFileWithSubtitlesHandler(table);
        setAllFileSubtitleLoader(table);
        setMergedSubtitlePreviewHandler(table);

        removeVideosButton.disableProperty().bind(table.selectedCountProperty().isEqualTo(0));
    }

    private void processDirectoryPath(String path, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && path.equals(ObjectUtils.firstNonNull(directoryPath, ""))) {
            return;
        }

        totalResult.clear();
        chosenDirectoryField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        if (fileOrigin == FileOrigin.FILE_CHOOSER) {
            chosenDirectoryField.setText(path);
        }
        refreshButton.setDisable(false);
        tableAndActionsPane.setDisable(false);
        table.clearTable();
        lastProcessedVideoInfo = null;

        directoryPath = path;

        try {
            if (fileOrigin == FileOrigin.FILE_CHOOSER) {
                settings.saveVideosDirectory(path);
            }
        } catch (SettingException e) {
            log.warn("failed to save last directory with videos: " + ExceptionUtils.getStackTrace(e));
        }

        ProcessDirectoryRunner backgroundRunner = new ProcessDirectoryRunner(directoryPath, context);

        BackgroundCallback<ProcessDirectoryRunner.Result> callback = result -> {
            if (result.getUnavailabilityReason() != null) {
                totalResult.setOnlyError(result.getUnavailabilityReason());
                chosenDirectoryField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                refreshButton.setDisable(result.isDisableRefresh());
                tableAndActionsPane.setDisable(true);

                allVideosInfo = null;
                allTableVideosInfo = null;
            } else {
                hideUnavailableCheckbox.setSelected(result.isHideUnavailable());
                table.setData(result.getTableData(), true);

                allVideosInfo = result.getFilesInfo();
                allTableVideosInfo = result.getAllTableFilesInfo();
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    private void disableActionButtons(int allSelectedCount, int selectedUnavailableCount) {
        boolean disable = false;
        Tooltip tooltip = null;
        if (allSelectedCount == 0) {
            disable = true;
            tooltip = GuiUtils.getTooltip("Please select at least one file");
        } else if (selectedUnavailableCount > 0) {
            disable = true;
            tooltip = GuiUtils.getTooltip("Please select only available files");
        }

        autoSelectButton.setDisable(disable);
        Tooltip.install(autoSelectButtonWrapper, tooltip);

        loadAllButton.setDisable(disable);
        Tooltip.install(loadAllButtonWrapper, tooltip);

        mergeButton.setDisable(disable);
        Tooltip.install(mergeButtonWrapper, tooltip);
    }

    private void handleSelectAll(boolean allSelected) {
        BackgroundRunner<Void> backgroundRunner = backgroundManager -> {
            backgroundManager.setCancellationPossible(false);
            backgroundManager.setIndeterminateProgress();
            backgroundManager.updateMessage("Processing videos...");

            for (TableVideoInfo fileInfo : table.getItems()) {
                boolean selected;
                if (table.getMode() == TableWithVideos.Mode.SEPARATE_FILES) {
                    selected = allSelected;
                } else {
                    selected = allSelected && StringUtils.isBlank(fileInfo.getNotValidReason());
                }

                Platform.runLater(() -> table.setSelected(selected, fileInfo));
            }

            return null;
        };

        runInBackground(backgroundRunner, (result) -> {});
    }

    private void handleSortChange(TableSortBy tableSortBy, TableSortDirection tableSortDirection) {
        SortBy sortBy = sortByFrom(tableSortBy);
        SortDirection sortDirection = sortDirectionFrom(tableSortDirection);

        try {
            settings.saveSortBy(sortBy.toString());
            settings.saveSortDirection(sortDirection.toString());
        } catch (SettingException e) {
            log.error("failed to save sort settings, most likely a bug: " + ExceptionUtils.getStackTrace(e));
        }

        totalResult.clear();
        clearLastProcessedResult();

        BackgroundRunner<TableData> backgroundRunner = backgroundManager -> {
            List<TableVideoInfo> sortedVideosInfo = VideoBackgroundUtils.getSortedVideosInfo(
                    table.getItems(),
                    sortBy,
                    sortDirection,
                    backgroundManager
            );
            return VideoBackgroundUtils.getTableData(
                    table.getMode(),
                    sortedVideosInfo,
                    sortBy,
                    sortDirection,
                    backgroundManager
            );
        };

        BackgroundCallback<TableData> callback = tableData -> table.setData(tableData, false);

        runInBackground(backgroundRunner, callback);
    }

    private static SortBy sortByFrom(TableSortBy tableSortBy) {
        switch (tableSortBy) {
            case NAME:
                return SortBy.NAME;
            case MODIFICATION_TIME:
                return SortBy.MODIFICATION_TIME;
            case SIZE:
                return SortBy.SIZE;
            default:
                log.error("unexpected sort by: " + tableSortBy + ", most likely a bug");
                throw new IllegalStateException();
        }
    }

    private static SortDirection sortDirectionFrom(TableSortDirection tableSortDirection) {
        switch (tableSortDirection) {
            case ASCENDING:
                return SortDirection.ASCENDING;
            case DESCENDING:
                return SortDirection.DESCENDING;
            default:
                log.error("unexpected sort direction: " + tableSortDirection + ", most likely a bug");
                throw new IllegalStateException();
        }
    }

    private void clearLastProcessedResult() {
        if (lastProcessedVideoInfo != null) {
            table.clearActionResult(lastProcessedVideoInfo);
        }
    }

    private void handleRemoveOption(TableSubtitleOption subtitleOption, TableVideoInfo tableVideoInfo) {
        totalResult.clear();
        clearLastProcessedResult();
        table.clearActionResult(tableVideoInfo);
        lastProcessedVideoInfo = tableVideoInfo;

        VideoInfo video = VideoInfo.getById(tableVideoInfo.getId(), allVideosInfo);
        video.getSubtitleOptions().removeIf(option -> Objects.equals(option.getId(), subtitleOption.getId()));

        table.removeFileWithSubtitles(subtitleOption, tableVideoInfo);
    }

    private void setSingleSubtitleLoader(TableWithVideos tableWithFiles) {
        tableWithFiles.setSingleSubtitleLoader((tableSubtitleOption, tableFileInfo) -> {
            totalResult.clear();
            clearLastProcessedResult();
            tableWithFiles.clearActionResult(tableFileInfo);
            lastProcessedVideoInfo = tableFileInfo;

            VideoInfo fileInfo = VideoInfo.getById(tableFileInfo.getId(), allVideosInfo);
            BuiltInSubtitleOption ffmpegStream = BuiltInSubtitleOption.getById(
                    tableSubtitleOption.getId(),
                    fileInfo.getBuiltInSubtitleOptions()
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

    private void handleSubtitleOptionPreview(TableSubtitleOption tableSubtitleOption, TableVideoInfo tableVideoInfo) {
        totalResult.clear();
        clearLastProcessedResult();
        table.clearActionResult(tableVideoInfo);

        VideoInfo videoInfo = VideoInfo.getById(tableVideoInfo.getId(), allVideosInfo);
        SubtitleOption subtitleOption = SubtitleOption.getById(
                tableSubtitleOption.getId(),
                videoInfo.getSubtitleOptions()
        );

        if (subtitleOption instanceof BuiltInSubtitleOption) {
            BuiltInSubtitleOption builtInOption = (BuiltInSubtitleOption) subtitleOption;

            Subtitles subtitles = builtInOption.getSubtitles();
            if (subtitles == null) {
                log.error("subtitles are null before the preview, most likely a bug");
                throw new IllegalStateException();
            }

            String title = videoInfo.getFile().getName()
                    + ", " + Utils.languageToString(builtInOption.getLanguage()).toUpperCase()
                    + (!StringUtils.isBlank(builtInOption.getTitle()) ? " " + builtInOption.getTitle() : "");

            Popups.showSimpleSubtitlesPreview(title, SubRipWriter.toText(subtitles, false), stage);
        } else if (subtitleOption instanceof ExternalSubtitleOption) {
            ExternalSubtitleOption externalOption = (ExternalSubtitleOption) subtitleOption;

            SubtitlesAndInput previewSelection = Popups.showEncodingPreview(
                    videoInfo.getFile().getAbsolutePath(),
                    externalOption.getSubtitlesAndInput(),
                    stage
            );
            externalOption.setSubtitlesAndInput(previewSelection);

            table.subtitleOptionPreviewClosed(
                    previewSelection.isCorrectFormat() ? null : "The subtitles have an incorrect format",
                    tableSubtitleOption);
        } else {
            log.error("unexpected subtitle option class " + subtitleOption.getClass() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private void setAddFileWithSubtitlesHandler(TableWithVideos tableWithFiles) {
        tableWithFiles.setAddFileWithSubtitlesHandler(tableFileInfo -> {
            totalResult.clear();
            clearLastProcessedResult();
            tableWithFiles.clearActionResult(tableFileInfo);
            lastProcessedVideoInfo = tableFileInfo;

            VideoInfo videoFileInfo = VideoInfo.getById(tableFileInfo.getId(), allVideosInfo);

            File fileWithSubtitlesToAdd = getFile(videoFileInfo, stage, settings).orElse(null);
            if (fileWithSubtitlesToAdd == null) {
                return;
            }

            try {
                if (fileWithSubtitlesToAdd.getParent() != null) {
                    settings.saveExternalDirectory(fileWithSubtitlesToAdd.getParent());
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

    private Optional<File> getFile(VideoInfo fileInfo, Stage stage, Settings settings) {
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

    private void setAllFileSubtitleLoader(TableWithVideos tableWithFiles) {
        tableWithFiles.setAllFileSubtitleLoader(tableFileInfo -> {
            totalResult.clear();
            clearLastProcessedResult();
            tableWithFiles.clearActionResult(tableFileInfo);
            lastProcessedVideoInfo = tableFileInfo;

            VideoInfo fileInfo = VideoInfo.getById(tableFileInfo.getId(), allVideosInfo);

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

    private void setMergedSubtitlePreviewHandler(TableWithVideos tableWithFiles) {
        tableWithFiles.setMergedSubtitlePreviewHandler(tableFileInfo -> {
            totalResult.clear();
            clearLastProcessedResult();
            tableWithFiles.clearActionResult(tableFileInfo);

            VideoInfo fileInfo = VideoInfo.getById(tableFileInfo.getId(), allVideosInfo);
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
                    context
            );

            BackgroundCallback<MergedPreviewRunner.Result> callback = result -> {
                if (result.isCanceled()) {
                    totalResult.setOnlyWarn("Merge has been cancelled");
                    return;
                }

                fileInfo.setMergedSubtitleInfo(result.getMergedSubtitleInfo());

                Popups.showMergedSubtitlesPreview(
                        getOptionTitleForPreview(upperOption),
                        getOptionTitleForPreview(lowerOption),
                        fileInfo.getMergedSubtitleInfo().getSubtitlesAndOutput().getText(),
                        stage
                );
            };

            runInBackground(mergedPreviewRunner, callback);
        });
    }

    private static String getOptionTitleForPreview(SubtitleOption option) {
        if (option instanceof BuiltInSubtitleOption) {
            BuiltInSubtitleOption ffmpegStream = (BuiltInSubtitleOption) option;

            return Utils.languageToString(ffmpegStream.getLanguage()).toUpperCase()
                    + (!StringUtils.isBlank(ffmpegStream.getTitle()) ? " " + ffmpegStream.getTitle() : "");
        } else if (option instanceof ExternalSubtitleOption) {
            ExternalSubtitleOption fileWithSubtitles = (ExternalSubtitleOption) option;

            return fileWithSubtitles.getFile().getAbsolutePath();
        } else {
            throw new IllegalStateException();
        }
    }

    void processChosenFiles(List<File> files) {
        try {
            settings.saveVideosDirectory(files.get(0).getParent());
        } catch (SettingException e) {
            log.warn("failed to save last directory with videos: " + ExceptionUtils.getStackTrace(e));
        }

        context.setVideosInProgress(true);
        GuiUtils.setVisibleAndManaged(addRemoveVideosPane, true);
        hideUnavailableCheckbox.setSelected(false);
        directoryPath = null;

        LoadSeparateFilesRunner backgroundRunner = new LoadSeparateFilesRunner(files, context);

        BackgroundCallback<LoadSeparateFilesRunner.Result> callback = result -> {
            allVideosInfo = result.getFilesInfo();
            allTableVideosInfo = result.getAllTableFilesInfo();

            table.setData(result.getTableData(), true);
        };

        runInBackground(backgroundRunner, callback);
    }

    void processChosenDirectory(File directory) {
        context.setVideosInProgress(true);
        GuiUtils.setVisibleAndManaged(chosenDirectoryPane, true);

        processDirectoryPath(directory.getAbsolutePath(), FileOrigin.FILE_CHOOSER);
    }

    @FXML
    private void backToSelectionClicked() {
        GuiUtils.setVisibleAndManaged(chosenDirectoryPane, false);
        chosenDirectoryField.setText(null);
        GuiUtils.setVisibleAndManaged(addRemoveVideosPane, false);

        totalResult.clear();
        tableAndActionsPane.setDisable(false);
        table.clearTable();
        lastProcessedVideoInfo = null;

        context.setVideosInProgress(false);
        setActivePane(ActivePane.CHOICE);
    }

    @FXML
    private void refreshClicked() {
        chosenDirectoryField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        refreshButton.setDisable(false);
        totalResult.clear();
        tableAndActionsPane.setDisable(false);
        table.clearTable();
        lastProcessedVideoInfo = null;

        ProcessDirectoryRunner backgroundRunner = new ProcessDirectoryRunner(directoryPath, context);

        BackgroundCallback<ProcessDirectoryRunner.Result> callback = result -> {
            if (result.getUnavailabilityReason() != null) {
                allVideosInfo = null;
                allTableVideosInfo = null;

                chosenDirectoryField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                totalResult.setOnlyError(result.getUnavailabilityReason());
                tableAndActionsPane.setDisable(true);
            } else {
                allVideosInfo = result.getFilesInfo();
                allTableVideosInfo = result.getAllTableFilesInfo();

                hideUnavailableCheckbox.setSelected(result.isHideUnavailable());
                table.setData(result.getTableData(), true);

                /* See the huge comment in the hideUnavailableClicked() method. */
                table.scrollTo(0);
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void hideUnavailableClicked() {
        totalResult.clear();
        clearLastProcessedResult();

        BackgroundRunner<TableData> backgroundRunner = backgroundManager -> {
            List<TableVideoInfo> videosInfo;
            if (hideUnavailableCheckbox.isSelected()) {
                videosInfo = VideoBackgroundUtils.getOnlyAvailableFilesInfo(allTableVideosInfo, backgroundManager);
            } else {
                videosInfo = allTableVideosInfo;
            }

            return VideoBackgroundUtils.getTableData(
                    table.getMode(),
                    videosInfo,
                    settings.getSortBy(),
                    settings.getSortDirection(),
                    backgroundManager
            );
        };

        BackgroundCallback<TableData> callback = tableData -> {
            table.setData(tableData, false);

            /*
             * There is a strange bug with TableView - when the list is shrunk in size (because for example
             * "hide unavailable" checkbox is checked but it can also happen when refresh is clicked I suppose) and both
             * big list and shrunk list have vertical scrollbars table isn't shrunk unless you move the scrollbar.
             * I've tried many workarounds but this one seems the best so far - just show the beginning of the table.
             * I couldn't find a bug with precise description but these ones fit quite well -
             * https://bugs.openjdk.java.net/browse/JDK-8095384, https://bugs.openjdk.java.net/browse/JDK-8087833.
             */
            table.scrollTo(0);
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void autoSelectClicked() {
        totalResult.clear();
        lastProcessedVideoInfo = null;

        AutoSelectSubtitlesRunner backgroundRunner = new AutoSelectSubtitlesRunner(
                table.getItems(),
                allVideosInfo,
                table,
                context.getFfmpeg(),
                settings
        );

        BackgroundCallback<ActionResult> callback = actionResult -> totalResult.set(actionResult);

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void loadAllSubtitlesClicked() {
        totalResult.clear();
        lastProcessedVideoInfo = null;

        MultipleFilesAllSubtitleLoader backgroundRunner = new MultipleFilesAllSubtitleLoader(
                table.getItems(),
                allVideosInfo,
                table,
                context.getFfmpeg()
        );

        BackgroundCallback<ActionResult> callback = actionResult -> totalResult.set(actionResult);

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void mergeClicked() {
        totalResult.clear();
        lastProcessedVideoInfo = null;

        MergePreparationRunner preparationRunner = new MergePreparationRunner(
                table.getItems(),
                allVideosInfo,
                table,
                context
        );

        BackgroundCallback<MergePreparationRunner.Result> callback = preparationResult -> {
            if (preparationResult.isCanceled()) {
                totalResult.setOnlyWarn("Merge has been cancelled");
                return;
            }

            if (preparationResult.getFilesWithoutSelectionCount() != 0) {
                totalResult.set(
                        getFilesWithoutSelectionResult(
                                preparationResult.getFilesWithoutSelectionCount(),
                                table.getSelectedCount()
                        )
                );
                return;
            }

            String agreementMessage = getFreeSpaceAgreementMessage(preparationResult).orElse(null);
            if (agreementMessage != null) {
                if (!Popups.showAgreementPopup(agreementMessage, "yes", "no", stage)) {
                    totalResult.setOnlyWarn("Merge has been cancelled");
                    return;
                }
            }

            List<File> confirmedFilesToOverwrite = new ArrayList<>();
            if (!CollectionUtils.isEmpty(preparationResult.getFilesToOverwrite())) {
                confirmedFilesToOverwrite = getConfirmedFilesToOverwrite(preparationResult, stage).orElse(null);
                if (confirmedFilesToOverwrite == null) {
                    totalResult.setOnlyWarn("Merge has been cancelled");
                    return;
                }
            }

            MergeRunner mergeRunner = new MergeRunner(
                    preparationResult.getFilesMergeInfo(),
                    confirmedFilesToOverwrite,
                    preparationResult.getDirectoryForTempFile(),
                    table.getItems(),
                    allVideosInfo,
                    table,
                    context.getFfprobe(),
                    context.getFfmpeg(),
                    settings
            );

            BackgroundCallback<MergeRunner.Result> mergeCallback = result -> {
                totalResult.set(result.getActionResult());

                table.setData(result.getTableData(), true);
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
                            + Utils.getFileSizeTextual(preparationResult.getRequiredTempSpace(), false)
                            + " of free disk space during the process but only "
                            + Utils.getFileSizeTextual(preparationResult.getAvailableTempSpace(), false)
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
            String fileName = Utils.getShortenedString(
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

            AgreementResult agreementResult = Popups.showAgreementPopup(
                    "File '" + fileName + "' already exists. Do you want to overwrite it?",
                    applyToAllText,
                    "Yes",
                    "No",
                    stage
            );
            if (agreementResult == AgreementResult.CANCELED) {
                return Optional.empty();
            } else if (agreementResult == AgreementResult.YES) {
                result.add(fileToOverwrite);
            } else if (agreementResult == AgreementResult.YES_TO_ALL) {
                List<File> filesLeft = mergePreparationResult.getFilesToOverwrite().subList(
                        mergePreparationResult.getFilesToOverwrite().size() - filesToOverwriteLeft,
                        mergePreparationResult.getFilesToOverwrite().size()
                );
                result.addAll(filesLeft);
                return Optional.of(result);
            } else if (agreementResult == AgreementResult.NO_TO_ALL) {
                return Optional.of(result);
            }

            filesToOverwriteLeft--;
        }

        return Optional.of(result);
    }

    @FXML
    private void removeVideosClicked() {
        totalResult.clear();
        clearLastProcessedResult();

        RemoveFilesRunner backgroundRunner = new RemoveFilesRunner(
                allVideosInfo,
                table.getMode(),
                allTableVideosInfo,
                table.getItems(),
                settings
        );

        BackgroundCallback<RemoveFilesRunner.Result> callback = result -> {
            allVideosInfo = result.getFilesInfo();
            allTableVideosInfo = result.getAllTableFilesInfo();

            table.setData(result.getTableData(), false);

            if (result.getRemovedCount() == 0) {
                log.error("nothing has been removed, most likely a bug");
                throw new IllegalStateException();
            } else if (result.getRemovedCount() == 1) {
                totalResult.setOnlySuccess("File has been removed from the list successfully");
            } else {
                totalResult.setOnlySuccess(
                        result.getRemovedCount() + " files have been removed from the list successfully"
                );
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void addVideoClicked() {
        totalResult.clear();
        clearLastProcessedResult();

        List<File> filesToAdd = getFiles(stage, settings);
        if (CollectionUtils.isEmpty(filesToAdd)) {
            return;
        }

        try {
            settings.saveVideosDirectory(filesToAdd.get(0).getParent());
        } catch (SettingException e) {
            log.warn("failed to save last directory with videos: " + ExceptionUtils.getStackTrace(e));
        }

        AddFilesRunner backgroundRunner = new AddFilesRunner(
                allVideosInfo,
                filesToAdd,
                allTableVideosInfo,
                hideUnavailableCheckbox.isSelected(),
                context
        );

        BackgroundCallback<AddFilesRunner.Result> callback = result -> {
            if (!StringUtils.isBlank(result.getAddFailedReason())) {
                totalResult.set(ActionResult.onlyError(result.getAddFailedReason()));
            } else {
                allVideosInfo = result.getFilesInfo();
                allTableVideosInfo = result.getAllTableFilesInfo();

                table.setData(result.getTableData(), false);

                totalResult.set(AddFilesRunner.getActionResult(result));
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    private static List<File> getFiles(Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose videos");
        fileChooser.setInitialDirectory(settings.getVideosDirectory());
        fileChooser.getExtensionFilters().add(GuiConstants.VIDEO_EXTENSION_FILTER);

        return fileChooser.showOpenMultipleDialog(stage);
    }

    void openSettingsForm() {
        mainFormController.openSettingsForm();
    }
}
