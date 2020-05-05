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
import kirill.subtitlemerger.gui.forms.videos.table.*;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.Popups;
import kirill.subtitlemerger.gui.utils.background.BackgroundCallback;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.entities.FileOrigin;
import kirill.subtitlemerger.logic.settings.SettingType;
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
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

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

    private List<Video> allVideos;

    private List<TableVideo> allTableVideos;

    /*
     * Before performing a one-video operation it is better to clean the result of the previous one-video operation, it
     * will look better. We can't just clear all videos' results because it may take a lot of time if there are many
     * videos and it's unacceptable for generally fast one-video operations. So we will keep track of the last processed
     * video to clear its result only when starting the next one-video operation.
     */
    private TableVideo lastProcessedVideo;

    public void initialize(MainFormController mainFormController, Stage stage, GuiContext context) {
        this.mainFormController = mainFormController;
        this.stage = stage;
        this.settings = context.getSettings();
        this.context = context;

        missingSettingsFormController.initialize(this, context);
        choiceFormController.initialize(this, stage, context);

        context.getMissingSettings().addListener((InvalidationListener) observable -> setActivePane());
        setActivePane();

        setFormHandlersAndBindings();
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

    private void setFormHandlersAndBindings() {
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

        table.setSelectAllHandler(this::selectAllVideos);
        table.setAddSubtitleFileHandler(this::addSubtitleFile);
        table.setRemoveSubtitleFileHandler(this::handleRemoveOption);
        setSingleSubtitleLoader(table);
        setAllFileSubtitleLoader(table);
        table.setSubtitleOptionPreviewHandler(this::handleSubtitleOptionPreview);
        setMergedSubtitlePreviewHandler(table);
        table.setChangeSortHandler(this::changeSort);

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
        lastProcessedVideo = null;

        directoryPath = path;
        if (fileOrigin == FileOrigin.FILE_CHOOSER) {
            settings.saveQuietly(new File(path), SettingType.VIDEO_DIRECTORY);
        }

        ProcessDirectoryRunner backgroundRunner = new ProcessDirectoryRunner(directoryPath, table, context);

        BackgroundCallback<ProcessDirectoryRunner.Result> callback = runnerResult -> {
            if (runnerResult.getNotValidReason() != null) {
                allVideos = null;
                allTableVideos = null;

                totalResult.setOnlyError(runnerResult.getNotValidReason());
                chosenDirectoryField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                refreshButton.setDisable(runnerResult.isDisableRefresh());
                tableAndActionsPane.setDisable(true);
            } else {
                allVideos = runnerResult.getAllVideos();
                allTableVideos = runnerResult.getAllTableVideos();

                hideUnavailableCheckbox.setSelected(runnerResult.isHideUnavailable());
                table.setData(runnerResult.getTableData(), true);
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    private void disableActionButtons(int selectedCount, int selectedUnavailableCount) {
        boolean disable = false;
        Tooltip tooltip = null;
        if (selectedCount == 0) {
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

    private void selectAllVideos(boolean selectAll) {
        BackgroundRunner<Void> backgroundRunner = backgroundManager -> {
            backgroundManager.setCancellationPossible(false);
            backgroundManager.setIndeterminateProgress();
            backgroundManager.updateMessage("Processing videos...");

            for (TableVideo video : table.getItems()) {
                boolean select;
                if (table.getMode() == TableMode.SEPARATE_VIDEOS) {
                    select = selectAll;
                } else {
                    select = selectAll && StringUtils.isBlank(video.getNotValidReason());
                }

                Platform.runLater(() -> video.setSelected(select));
            }

            return null;
        };

        runInBackground(backgroundRunner, (result) -> {});
    }

    private void addSubtitleFile(TableVideo tableVideo) {
        totalResult.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();
        lastProcessedVideo = tableVideo;

        Video video = Video.getById(tableVideo.getId(), allVideos);
        File subtitleFile = getSubtitleFile(video, stage, settings);
        if (subtitleFile == null) {
            return;
        }
        settings.saveQuietly(subtitleFile.getParentFile(), SettingType.VIDEO_SUBTITLE_DIRECTORY);

        ProcessSubtitleFileRunner backgroundRunner = new ProcessSubtitleFileRunner(subtitleFile, video);

        BackgroundCallback<ProcessSubtitleFileRunner.Result> callback = result -> {
            if (result.getNotValidReason() != null) {
                tableVideo.setOnlyError(result.getNotValidReason());
            } else {
                video.getSubtitleOptions().add(result.getSubtitleOption());

                SubtitlesAndInput subtitlesAndInput = result.getSubtitleOption().getSubtitlesAndInput();

                String notValidReason = null;
                ActionResult actionResult;
                if (!subtitlesAndInput.isCorrectFormat()) {
                    notValidReason = "The subtitles have an incorrect format";
                    actionResult = ActionResult.onlyWarn(
                            "The file has been added but subtitles can't be parsed, it can happen if the file is not "
                                    + "UTF-8-encoded, you can change the encoding after pressing the preview button"
                    );
                } else {
                    actionResult = ActionResult.onlySuccess("The file with subtitles has been added successfully");
                }

                tableVideo.addOption(
                        TableSubtitleOption.createExternal(
                                result.getSubtitleOption().getId(),
                                tableVideo,
                                notValidReason,
                                result.getSubtitleOption().getFile().getAbsolutePath(),
                                subtitlesAndInput.getSize(),
                                false,
                                false
                        ),
                        actionResult
                );
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    @Nullable
    private static File getSubtitleFile(Video fileInfo, Stage stage, Settings settings) {
        File initialDirectory = settings.getVideoSubtitleDirectory();
        if (initialDirectory == null) {
            initialDirectory = fileInfo.getFile().getParentFile();
        }

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose a file with subtitles");
        fileChooser.setInitialDirectory(initialDirectory);
        fileChooser.getExtensionFilters().add(GuiConstants.SUB_RIP_EXTENSION_FILTER);

        return fileChooser.showOpenDialog(stage);
    }

    private void changeSort(TableSortBy tableSortBy, TableSortDirection tableSortDirection) {
        SortBy sortBy = sortByFrom(tableSortBy);
        SortDirection sortDirection = sortDirectionFrom(tableSortDirection);

        settings.save(sortBy, SettingType.SORT_BY);
        settings.save(sortDirection, SettingType.SORT_DIRECTION);

        totalResult.clear();
        clearLastProcessedResult();

        BackgroundRunner<TableData> backgroundRunner = backgroundManager -> {
            List<TableVideo> sortedVideosInfo = VideosBackgroundUtils.getSortedVideos(
                    table.getItems(),
                    sortBy,
                    sortDirection,
                    backgroundManager
            );
            return VideosBackgroundUtils.getTableData(
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
        if (lastProcessedVideo != null) {
            lastProcessedVideo.clearActionResult();
        }
    }

    private void handleRemoveOption(TableSubtitleOption subtitleOption) {
        TableVideo tableVideo = subtitleOption.getVideo();
        totalResult.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();
        lastProcessedVideo = tableVideo;

        Video video = Video.getById(tableVideo.getId(), allVideos);
        video.getSubtitleOptions().removeIf(option -> Objects.equals(option.getId(), subtitleOption.getId()));

        tableVideo.removeOption(
                subtitleOption,
                ActionResult.onlySuccess("The subtitle file has been removed from the list successfully")
        );
    }

    private void setSingleSubtitleLoader(TableWithVideos tableWithFiles) {
        tableWithFiles.setSingleSubtitleLoader((tableSubtitleOption) -> {
            TableVideo tableVideoInfo = tableSubtitleOption.getVideo();
            totalResult.clear();
            clearLastProcessedResult();
            tableVideoInfo.clearActionResult();
            lastProcessedVideo = tableVideoInfo;

            Video fileInfo = Video.getById(tableVideoInfo.getId(), allVideos);
            BuiltInSubtitleOption ffmpegStream = BuiltInSubtitleOption.getById(
                    tableSubtitleOption.getId(),
                    fileInfo.getBuiltInSubtitleOptions()
            );

            LoadSingleSubtitlesRunner backgroundRunner = new LoadSingleSubtitlesRunner(
                    ffmpegStream,
                    fileInfo,
                    tableSubtitleOption,
                    context.getFfmpeg()
            );

            BackgroundCallback<ActionResult> callback = tableVideoInfo::setActionResult;

            runInBackground(backgroundRunner, callback);
        });
    }

    private void handleSubtitleOptionPreview(TableSubtitleOption tableSubtitleOption) {
        TableVideo tableVideo = tableSubtitleOption.getVideo();
        totalResult.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        Video videoInfo = Video.getById(tableVideo.getId(), allVideos);
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

            if (!StringUtils.isBlank(tableSubtitleOption.getNotValidReason()) && previewSelection.isCorrectFormat()) {
                tableSubtitleOption.markAsValid();
            }
        } else {
            log.error("unexpected subtitle option class " + subtitleOption.getClass() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private void setAllFileSubtitleLoader(TableWithVideos tableWithFiles) {
        tableWithFiles.setAllVideoSubtitleLoader(tableVideoInfo -> {
            totalResult.clear();
            clearLastProcessedResult();
            tableVideoInfo.clearActionResult();
            lastProcessedVideo = tableVideoInfo;

            Video fileInfo = Video.getById(tableVideoInfo.getId(), allVideos);

            SingleFileAllSubtitleLoader backgroundRunner = new SingleFileAllSubtitleLoader(
                    fileInfo,
                    tableVideoInfo,
                    context.getFfmpeg()
            );

            BackgroundCallback<ActionResult> callback = tableVideoInfo::setActionResult;

            runInBackground(backgroundRunner, callback);
        });
    }

    private void setMergedSubtitlePreviewHandler(TableWithVideos tableWithFiles) {
        tableWithFiles.setMergedSubtitlePreviewHandler(tableVideoInfo -> {
            totalResult.clear();
            clearLastProcessedResult();
            tableVideoInfo.clearActionResult();

            Video fileInfo = Video.getById(tableVideoInfo.getId(), allVideos);
            SubtitleOption upperOption = SubtitleOption.getById(
                    tableVideoInfo.getUpperOption().getId(),
                    fileInfo.getSubtitleOptions()
            );
            SubtitleOption lowerOption = SubtitleOption.getById(
                    tableVideoInfo.getLowerOption().getId(),
                    fileInfo.getSubtitleOptions()
            );

            MergedPreviewRunner mergedPreviewRunner = new MergedPreviewRunner(
                    upperOption,
                    lowerOption,
                    fileInfo,
                    tableVideoInfo,
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
        settings.saveQuietly(files.get(0).getParentFile(), SettingType.VIDEO_DIRECTORY);

        GuiUtils.setVisibleAndManaged(addRemoveVideosPane, true);
        hideUnavailableCheckbox.setSelected(false);
        directoryPath = null;

        LoadSeparateFilesRunner backgroundRunner = new LoadSeparateFilesRunner(files, table, context);

        BackgroundCallback<LoadSeparateFilesRunner.Result> callback = result -> {
            allVideos = result.getFilesInfo();
            allTableVideos = result.getAllTableFilesInfo();

            table.setData(result.getTableData(), true);
        };

        runInBackground(backgroundRunner, callback);
    }

    void processChosenDirectory(File directory) {
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
        lastProcessedVideo = null;

        setActivePane(ActivePane.CHOICE);
        context.setVideosInProgress(false);
    }

    @FXML
    private void refreshClicked() {
        chosenDirectoryField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        refreshButton.setDisable(false);
        totalResult.clear();
        tableAndActionsPane.setDisable(false);
        table.clearTable();
        lastProcessedVideo = null;

        ProcessDirectoryRunner backgroundRunner = new ProcessDirectoryRunner(directoryPath, table, context);

        BackgroundCallback<ProcessDirectoryRunner.Result> callback = result -> {
            if (result.getNotValidReason() != null) {
                allVideos = null;
                allTableVideos = null;

                chosenDirectoryField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                totalResult.setOnlyError(result.getNotValidReason());
                tableAndActionsPane.setDisable(true);
            } else {
                allVideos = result.getAllVideos();
                allTableVideos = result.getAllTableVideos();

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
            List<TableVideo> videosInfo;
            if (hideUnavailableCheckbox.isSelected()) {
                videosInfo = VideosBackgroundUtils.getOnlyValidVideos(allTableVideos, backgroundManager);
            } else {
                videosInfo = allTableVideos;
            }

            return VideosBackgroundUtils.getTableData(
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
        lastProcessedVideo = null;

        AutoSelectSubtitlesRunner backgroundRunner = new AutoSelectSubtitlesRunner(
                table.getItems(),
                allVideos,
                context.getFfmpeg(),
                settings
        );

        BackgroundCallback<ActionResult> callback = actionResult -> totalResult.setActionResult(actionResult);

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void loadAllSubtitlesClicked() {
        totalResult.clear();
        lastProcessedVideo = null;

        MultipleFilesAllSubtitleLoader backgroundRunner = new MultipleFilesAllSubtitleLoader(
                table.getItems(),
                allVideos,
                context.getFfmpeg()
        );

        BackgroundCallback<ActionResult> callback = actionResult -> totalResult.setActionResult(actionResult);

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void mergeClicked() {
        totalResult.clear();
        lastProcessedVideo = null;

        MergePreparationRunner preparationRunner = new MergePreparationRunner(table.getItems(), allVideos, context);

        BackgroundCallback<MergePreparationRunner.Result> callback = preparationResult -> {
            if (preparationResult.isCanceled()) {
                totalResult.setOnlyWarn("Merge has been cancelled");
                return;
            }

            if (preparationResult.getFilesWithoutSelectionCount() != 0) {
                totalResult.setActionResult(
                        getFilesWithoutSelectionResult(
                                preparationResult.getFilesWithoutSelectionCount(),
                                table.getSelectedCount()
                        )
                );
                return;
            }

            String agreementMessage = getFreeSpaceAgreementMessage(preparationResult).orElse(null);
            if (agreementMessage != null) {
                if (!Popups.askAgreement(agreementMessage, "yes", "no", stage)) {
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
                    allVideos,
                    table,
                    context.getFfprobe(),
                    context.getFfmpeg(),
                    settings
            );

            BackgroundCallback<MergeRunner.Result> mergeCallback = result -> {
                totalResult.setActionResult(result.getActionResult());

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
                            + Utils.getSizeTextual(preparationResult.getRequiredTempSpace(), false)
                            + " of free disk space during the process but only "
                            + Utils.getSizeTextual(preparationResult.getAvailableTempSpace(), false)
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

            AgreementResult agreementResult = Popups.askAgreement(
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
                allVideos,
                table.getMode(),
                allTableVideos,
                table.getItems(),
                settings
        );

        BackgroundCallback<RemoveFilesRunner.Result> callback = result -> {
            allVideos = result.getFilesInfo();
            allTableVideos = result.getAllTableFilesInfo();

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

        settings.saveQuietly(filesToAdd.get(0).getParentFile(), SettingType.VIDEO_DIRECTORY);

        AddFilesRunner backgroundRunner = new AddFilesRunner(
                allVideos,
                filesToAdd,
                allTableVideos,
                hideUnavailableCheckbox.isSelected(),
                table,
                context
        );

        BackgroundCallback<AddFilesRunner.Result> callback = result -> {
            if (!StringUtils.isBlank(result.getAddFailedReason())) {
                totalResult.setActionResult(ActionResult.onlyError(result.getAddFailedReason()));
            } else {
                allVideos = result.getFilesInfo();
                allTableVideos = result.getAllTableFilesInfo();

                table.setData(result.getTableData(), false);

                totalResult.setActionResult(AddFilesRunner.getActionResult(result));
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    private static List<File> getFiles(Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose videos");
        fileChooser.setInitialDirectory(settings.getVideoDirectory());
        fileChooser.getExtensionFilters().add(GuiConstants.VIDEO_EXTENSION_FILTER);

        return fileChooser.showOpenMultipleDialog(stage);
    }

    void openSettingsForm() {
        mainFormController.openSettingsForm();
    }
}
