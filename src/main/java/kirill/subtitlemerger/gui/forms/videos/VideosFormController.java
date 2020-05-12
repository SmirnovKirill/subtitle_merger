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
import kirill.subtitlemerger.gui.forms.common.agreement.AgreementResult;
import kirill.subtitlemerger.gui.forms.videos.background.*;
import kirill.subtitlemerger.gui.forms.videos.table.*;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.Popups;
import kirill.subtitlemerger.gui.utils.background.BackgroundCallback;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.entities.FileOrigin;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.settings.SettingType;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.settings.SortBy;
import kirill.subtitlemerger.logic.settings.SortDirection;
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

import static kirill.subtitlemerger.gui.forms.videos.background.VideosBackgroundUtils.*;

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

    private Ffprobe ffprobe;

    private Ffmpeg ffmpeg;

    private Settings settings;

    private GuiContext context;

    private String directoryPath;

    private List<Video> allVideos;

    /*
     * This variable stores all (meaning both available and unavailable) videos and the videos have to be sorted. This
     * variable is necessary and the table's getRows() method isn't enough because if the user presses the "hide
     * unavailable" button the information about the unavailable videos will be removed from the table.
     */
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
        this.ffprobe = context.getFfprobe();
        this.ffmpeg = context.getFfmpeg();
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
        table.setRemoveSubtitleFileHandler(this::removeSubtitleFile);
        table.setSingleSubtitleLoader(this::loadSingleSubtitles);
        table.setAllVideoSubtitleLoader(this::loadAllVideoSubtitles);
        table.setSubtitleOptionPreviewHandler(this::handleSubtitleOptionPreview);
        table.setMergedSubtitlePreviewHandler(this::handleMergedSubtitlePreview);
        table.setChangeSortHandler(this::handleSortChange);

        removeVideosButton.disableProperty().bind(table.selectedCountProperty().isEqualTo(0));
    }

    private void processDirectoryPath(String path, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && path.equals(ObjectUtils.firstNonNull(directoryPath, ""))) {
            return;
        }

        chosenDirectoryField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        if (fileOrigin == FileOrigin.FILE_CHOOSER) {
            chosenDirectoryField.setText(path);
        }
        refreshButton.setDisable(false);
        totalResult.clear();
        tableAndActionsPane.setDisable(false);
        table.clearTable();

        directoryPath = path;
        if (fileOrigin == FileOrigin.FILE_CHOOSER) {
            settings.saveQuietly(new File(path), SettingType.VIDEO_DIRECTORY);
        }
        lastProcessedVideo = null;

        ProcessDirectoryRunner backgroundRunner = new ProcessDirectoryRunner(directoryPath, table, ffprobe, settings);

        BackgroundCallback<ProcessDirectoryRunner.Result> callback = runnerResult -> {
            if (runnerResult.getNotValidReason() != null) {
                chosenDirectoryField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                refreshButton.setDisable(runnerResult.isDisableRefresh());
                totalResult.setOnlyError(runnerResult.getNotValidReason());
                tableAndActionsPane.setDisable(true);

                allVideos = null;
                allTableVideos = null;
            } else {
                hideUnavailableCheckbox.setSelected(runnerResult.isHideUnavailable());
                table.setData(runnerResult.getTableData(), true);

                allVideos = runnerResult.getAllVideos();
                allTableVideos = runnerResult.getAllTableVideos();
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
            backgroundManager.setCancelPossible(false);
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

        runInBackground(backgroundRunner, (runnerResult) -> {});
    }

    private void addSubtitleFile(TableVideo tableVideo) {
        totalResult.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        Video video = Video.getById(tableVideo.getId(), allVideos);
        File subtitleFile = getSubtitleFile(video, stage, settings);
        if (subtitleFile == null) {
            return;
        }
        settings.saveQuietly(subtitleFile.getParentFile(), SettingType.VIDEO_SUBTITLE_DIRECTORY);

        ProcessSubtitleFileRunner backgroundRunner = new ProcessSubtitleFileRunner(subtitleFile, video, tableVideo);

        BackgroundCallback<ProcessSubtitleFileRunner.Result> callback = runnerResult -> {
            tableVideo.setActionResult(runnerResult.getActionResult());
            if (runnerResult.getOption() != null && runnerResult.getTableOption() != null) {
                video.getOptions().add(runnerResult.getOption());
                tableVideo.addOption(runnerResult.getTableOption());
            }
        };

        runInBackground(backgroundRunner, callback);

        lastProcessedVideo = tableVideo;
    }

    private void clearLastProcessedResult() {
        if (lastProcessedVideo != null) {
            lastProcessedVideo.clearActionResult();
        }
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

    private void removeSubtitleFile(TableSubtitleOption tableOption) {
        TableVideo tableVideo = tableOption.getVideo();

        totalResult.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        Video video = Video.getById(tableVideo.getId(), allVideos);
        SubtitleOption option = video.getOption(tableOption.getId());
        video.getOptions().remove(option);

        String successMessage = "The file file with subtitles has been removed successfully";
        tableVideo.removeOption(tableOption, ActionResult.onlySuccess(successMessage));
        lastProcessedVideo = tableVideo;
    }

    private void loadSingleSubtitles(TableSubtitleOption tableOption) {
        TableVideo tableVideo = tableOption.getVideo();

        totalResult.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        BackgroundRunner<ActionResult> backgroundRunner = backgroundManager -> {
            backgroundManager.setCancelPossible(true);
            backgroundManager.setCancelDescription("Please be patient, this may take a while depending on the video.");
            backgroundManager.setIndeterminateProgress();

            Video video = Video.getById(tableVideo.getId(), allVideos);
            String action = "Loading " + tableOption.getTitle() + " in " + video.getFile().getName() + "...";
            backgroundManager.updateMessage(action);
            try {
                BuiltInSubtitleOption option = video.getBuiltInOption(tableOption.getId());
                LoadSubtitlesResult loadResult = loadSubtitles(option, video, tableOption, ffmpeg);
                if (loadResult == LoadSubtitlesResult.SUCCESS) {
                    return ActionResult.onlySuccess("The subtitles have been loaded successfully");
                } else if (loadResult == LoadSubtitlesResult.FAILED) {
                    return ActionResult.onlyError("Failed to load the subtitles");
                } else if (loadResult == LoadSubtitlesResult.INCORRECT_FORMAT) {
                    return ActionResult.onlyError("The subtitles have an incorrect format");
                } else {
                    log.error("unexpected load result: " + loadResult + ", most likely a bug");
                    throw new IllegalStateException();
                }
            } catch (InterruptedException e) {
                return ActionResult.onlyWarn("The task has been cancelled");
            }
        };

        BackgroundCallback<ActionResult> callback = tableVideo::setActionResult;

        runInBackground(backgroundRunner, callback);

        lastProcessedVideo = tableVideo;
    }

    private void loadAllVideoSubtitles(TableVideo tableVideo) {
        totalResult.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        BackgroundRunner<ActionResult> backgroundRunner = backgroundManager -> {
            backgroundManager.setCancelPossible(false);
            backgroundManager.setIndeterminateProgress();

            Video video = Video.getById(tableVideo.getId(), allVideos);
            backgroundManager.updateMessage("Calculating the number of subtitles to load...");
            int toLoadCount = video.getOptionsToLoad().size();
            int processedCount = 0;
            int successfulCount = 0;
            int failedCount = 0;
            int incorrectCount = 0;

            backgroundManager.setCancelPossible(true);
            backgroundManager.setCancelDescription("Please be patient, this may take a while depending on the video.");
            try {
                for (BuiltInSubtitleOption option : video.getOptionsToLoad()) {
                    TableSubtitleOption tableOption = tableVideo.getOption(option.getId());

                    String action = "Loading " + tableOption.getTitle() + " in " + video.getFile().getName() + "...";
                    backgroundManager.updateMessage(getProgressAction(processedCount, toLoadCount, action));

                    LoadSubtitlesResult loadResult = loadSubtitles(option, video, tableOption, ffmpeg);
                    if (loadResult == LoadSubtitlesResult.SUCCESS) {
                        successfulCount++;
                    } else if (loadResult == LoadSubtitlesResult.FAILED) {
                        failedCount++;
                    } else if (loadResult == LoadSubtitlesResult.INCORRECT_FORMAT) {
                        incorrectCount++;
                    } else {
                        log.error("unexpected load result: " + loadResult + ", most likely a bug");
                        throw new IllegalStateException();
                    }

                    processedCount++;
                }
            } catch (InterruptedException e) {
                /* Do nothing here, will just return the result based on the work done. */
            }

            return getLoadSubtitlesResult(toLoadCount, processedCount, successfulCount, failedCount, incorrectCount);
        };

        BackgroundCallback<ActionResult> callback = tableVideo::setActionResult;

        runInBackground(backgroundRunner, callback);

        lastProcessedVideo = tableVideo;
    }

    private void handleSubtitleOptionPreview(TableSubtitleOption tableOption) {
        TableVideo tableVideo = tableOption.getVideo();

        totalResult.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        Video video = Video.getById(tableVideo.getId(), allVideos);
        SubtitleOption option = video.getOption(tableOption.getId());
        SubtitlesAndInput subtitlesAndInput = option.getSubtitlesAndInput();
        if (subtitlesAndInput == null) {
            log.error("subtitles are not loaded before the preview, most likely a bug");
            throw new IllegalStateException();
        }

        if (option instanceof BuiltInSubtitleOption) {
            String title = video.getFile().getName() + ", " + tableOption.getTitle();
            String subtitleText = new String(subtitlesAndInput.getRawData(), subtitlesAndInput.getEncoding());
            Popups.showSimpleSubtitlesPreview(title, subtitleText, stage);
        } else if (option instanceof ExternalSubtitleOption) {
            ExternalSubtitleOption externalOption = (ExternalSubtitleOption) option;

            SubtitlesAndInput previewSelection = Popups.showEncodingPreview(
                    video.getFile().getAbsolutePath(),
                    subtitlesAndInput,
                    stage
            );
            externalOption.setSubtitlesAndInput(previewSelection);

            if (!StringUtils.isBlank(tableOption.getNotValidReason()) && previewSelection.isCorrectFormat()) {
                tableOption.markAsValid();
            }
        } else {
            log.error("unexpected subtitle option class " + option.getClass() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private void handleMergedSubtitlePreview(TableVideo tableVideo) {
        totalResult.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        Video video = Video.getById(tableVideo.getId(), allVideos);

        MergedPreviewRunner mergedPreviewRunner = new MergedPreviewRunner(video, tableVideo, ffmpeg, settings);

        BackgroundCallback<MergedPreviewRunner.Result> callback = runnerResult -> {
            if (runnerResult == null) {
                totalResult.setOnlyWarn("The merge has been cancelled");
            } else if (!StringUtils.isBlank(runnerResult.getError())) {
                tableVideo.setActionResult(ActionResult.onlyError(runnerResult.getError()));
                lastProcessedVideo = tableVideo;
            } else {
                if (runnerResult.getMergedSubtitleInfo() == null) {
                    log.error("merged subtitle info can't be null, most likely a bug");
                    throw new IllegalStateException();
                }

                video.setMergedSubtitleInfo(runnerResult.getMergedSubtitleInfo());
                Popups.showMergedSubtitlesPreview(
                        tableVideo.getUpperOption().getTitle(),
                        tableVideo.getLowerOption().getTitle(),
                        runnerResult.getMergedSubtitleInfo().getSubtitlesAndOutput().getText(),
                        stage
                );
            }
        };

        runInBackground(mergedPreviewRunner, callback);
    }

    private void handleSortChange(TableSortBy tableSortBy, TableSortDirection tableSortDirection) {
        totalResult.clear();
        clearLastProcessedResult();

        settings.save(sortByFrom(tableSortBy), SettingType.SORT_BY);
        settings.save(sortDirectionFrom(tableSortDirection), SettingType.SORT_DIRECTION);

        BackgroundRunner<TableData> backgroundRunner = backgroundManager -> {
            allTableVideos = getSortedVideos(allTableVideos, settings.getSort(), backgroundManager);

            return getTableData(
                    allTableVideos,
                    hideUnavailableCheckbox.isSelected(),
                    table.getMode(),
                    settings.getSort(),
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
                log.error("unexpected table sort by: " + tableSortBy + ", most likely a bug");
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
                log.error("unexpected table sort direction: " + tableSortDirection + ", most likely a bug");
                throw new IllegalStateException();
        }
    }

    void openSettingsForm() {
        mainFormController.openSettingsForm();
    }

    void processChosenVideoFiles(List<File> videoFiles) {
        settings.saveQuietly(videoFiles.get(0).getParentFile(), SettingType.VIDEO_DIRECTORY);

        hideUnavailableCheckbox.setSelected(false);
        GuiUtils.setVisibleAndManaged(addRemoveVideosPane, true);

        ProcessVideoFilesRunner backgroundRunner = new ProcessVideoFilesRunner(videoFiles, table, ffprobe, settings);

        BackgroundCallback<ProcessVideoFilesRunner.Result> callback = runnerResult -> {
            table.setData(runnerResult.getTableData(), true);

            allVideos = runnerResult.getAllVideos();
            allTableVideos = runnerResult.getAllTableVideos();
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
        refreshButton.setDisable(false);
        totalResult.clear();
        tableAndActionsPane.setDisable(false);
        table.clearTable();
        GuiUtils.setVisibleAndManaged(addRemoveVideosPane, false);

        directoryPath = null;
        lastProcessedVideo = null;

        setActivePane(ActivePane.CHOICE);
        context.setVideosInProgress(false);
    }

    @FXML
    private void refreshClicked() {
        chosenDirectoryField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        totalResult.clear();
        tableAndActionsPane.setDisable(false);
        table.clearTable();

        lastProcessedVideo = null;

        ProcessDirectoryRunner backgroundRunner = new ProcessDirectoryRunner(directoryPath, table, ffprobe, settings);

        BackgroundCallback<ProcessDirectoryRunner.Result> callback = runnerResult -> {
            if (runnerResult.getNotValidReason() != null) {
                chosenDirectoryField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                refreshButton.setDisable(runnerResult.isDisableRefresh());
                totalResult.setOnlyError(runnerResult.getNotValidReason());
                tableAndActionsPane.setDisable(true);

                allVideos = null;
                allTableVideos = null;
            } else {
                hideUnavailableCheckbox.setSelected(runnerResult.isHideUnavailable());
                table.setData(runnerResult.getTableData(), true);

                /* See the huge comment in the allTableVideosnavailableClicked() method. */
                table.scrollTo(0);

                allVideos = runnerResult.getAllVideos();
                allTableVideos = runnerResult.getAllTableVideos();
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void hideUnavailableClicked() {
        totalResult.clear();
        clearLastProcessedResult();

        BackgroundRunner<TableData> backgroundRunner = backgroundManager ->
                getTableData(
                        allTableVideos,
                        hideUnavailableCheckbox.isSelected(),
                        table.getMode(),
                        settings.getSort(),
                        backgroundManager
                );

        BackgroundCallback<TableData> callback = tableData -> {
            table.setData(tableData, false);

            /*
             * There is a strange bug with the TableView - when the list is shrunk in size (because for example the
             * "hide unavailable" checkbox is checked but it can also happen when the refresh is clicked I suppose) and
             * both the big list and the shrunk list have vertical scrollbars the table isn't shrunk unless you move the
             * scrollbar. I've tried many workarounds but this one seems the best so far - just show the beginning of
             * the table. I couldn't find the bug with a precise description but these ones fit quite well -
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

        AutoSelectRunner backgroundRunner = new AutoSelectRunner(table.getItems(), allVideos, ffmpeg, settings);
        BackgroundCallback<ActionResult> callback = actionResult -> totalResult.setActionResult(actionResult);
        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void loadAllSubtitlesClicked() {
        totalResult.clear();
        lastProcessedVideo = null;

        AllSubtitleLoader backgroundRunner = new AllSubtitleLoader(table.getItems(), allVideos, ffmpeg);
        BackgroundCallback<ActionResult> callback = actionResult -> totalResult.setActionResult(actionResult);
        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void mergeClicked() {
        totalResult.clear();
        lastProcessedVideo = null;

        MergePrepareRunner preparationRunner = new MergePrepareRunner(table.getItems(), allVideos, ffmpeg, settings);

        BackgroundCallback<MergePrepareRunner.Result> callback = runnerResult -> {
            if (runnerResult.isCanceled()) {
                totalResult.setOnlyWarn("Merge has been cancelled");
                return;
            }

            if (runnerResult.getFilesWithoutSelectionCount() != 0) {
                totalResult.setActionResult(
                        getFilesWithoutSelectionResult(
                                runnerResult.getFilesWithoutSelectionCount(),
                                table.getSelectedCount()
                        )
                );
                return;
            }

            String agreementMessage = getFreeSpaceAgreementMessage(runnerResult);
            if (agreementMessage != null) {
                if (!Popups.askAgreement(agreementMessage, "yes", "no", stage)) {
                    totalResult.setOnlyWarn("Merge has been cancelled");
                    return;
                }
            }

            List<File> confirmedFilesToOverwrite = new ArrayList<>();
            if (!CollectionUtils.isEmpty(runnerResult.getFilesToOverwrite())) {
                confirmedFilesToOverwrite = getConfirmedFilesToOverwrite(runnerResult, stage);
                if (confirmedFilesToOverwrite == null) {
                    totalResult.setOnlyWarn("Merge has been cancelled");
                    return;
                }
            }

            MergeRunner mergeRunner = new MergeRunner(
                    runnerResult.getFilesMergeInfo(),
                    confirmedFilesToOverwrite,
                    runnerResult.getDirectoryForTempFile(),
                    table.getItems(),
                    allVideos,
                    context.getFfprobe(),
                    ffmpeg,
                    settings
            );

            BackgroundCallback<ActionResult> mergeCallback = actionResult -> totalResult.setActionResult(actionResult);

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

    @Nullable
    private static String getFreeSpaceAgreementMessage(MergePrepareRunner.Result preparationResult) {
        if (preparationResult.getRequiredTempSpace() != null) {
            if (preparationResult.getRequiredTempSpace() <= preparationResult.getAvailableTempSpace()) {
                return null;
            }

            return "Merge requires approximately "
                    + Utils.getSizeTextual(preparationResult.getRequiredTempSpace(), false)
                    + " of free disk space during the process but only "
                    + Utils.getSizeTextual(preparationResult.getAvailableTempSpace(), false)
                    + " is available, proceed anyway?";
        } else {
            return null;
        }
    }

    @Nullable
    private static List<File> getConfirmedFilesToOverwrite(
            MergePrepareRunner.Result mergePreparationResult,
            Stage stage
    ) {
        if (CollectionUtils.isEmpty(mergePreparationResult.getFilesToOverwrite())) {
            return new ArrayList<>();
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
                return null;
            } else if (agreementResult == AgreementResult.YES) {
                result.add(fileToOverwrite);
            } else if (agreementResult == AgreementResult.YES_TO_ALL) {
                List<File> filesLeft = mergePreparationResult.getFilesToOverwrite().subList(
                        mergePreparationResult.getFilesToOverwrite().size() - filesToOverwriteLeft,
                        mergePreparationResult.getFilesToOverwrite().size()
                );
                result.addAll(filesLeft);
                return result;
            } else if (agreementResult == AgreementResult.NO_TO_ALL) {
                return result;
            }

            filesToOverwriteLeft--;
        }

        return result;
    }

    @FXML
    private void removeVideosClicked() {
        totalResult.clear();
        clearLastProcessedResult();

        int originalVideoCount = allTableVideos.size();

        BackgroundRunner<TableData> backgroundRunner = backgroundManager -> {
            backgroundManager.setCancelPossible(false);
            backgroundManager.setIndeterminateProgress();
            backgroundManager.updateMessage("Removing videos from the list...");

            for (TableVideo tableVideo : table.getItems()) {
                if (tableVideo.isSelected()) {
                    allTableVideos.remove(tableVideo);
                    allVideos.remove(Video.getById(tableVideo.getId(), allVideos));
                }
            }

            return getTableData(
                    allTableVideos,
                    hideUnavailableCheckbox.isSelected(),
                    table.getMode(),
                    settings.getSort(),
                    backgroundManager
            );
        };

        BackgroundCallback<TableData> callback = tableData -> {
            int removedCount = originalVideoCount - allVideos.size();

            if (removedCount == 0) {
                log.error("no videos have been removed, most likely a bug");
                throw new IllegalStateException();
            } else if (removedCount == 1) {
                totalResult.setOnlySuccess("The video has been removed from the list successfully");
            } else {
                totalResult.setOnlySuccess(removedCount + " videos have been removed from the list successfully");
            }

            table.setData(tableData, false);
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void addVideosClicked() {
        totalResult.clear();
        clearLastProcessedResult();

        List<File> videoFilesToAdd = getVideoFiles(stage, settings);
        if (CollectionUtils.isEmpty(videoFilesToAdd)) {
            return;
        }
        settings.saveQuietly(videoFilesToAdd.get(0).getParentFile(), SettingType.VIDEO_DIRECTORY);

        ProcessExtraVideoFilesRunner backgroundRunner = new ProcessExtraVideoFilesRunner(
                videoFilesToAdd,
                allVideos,
                allTableVideos,
                hideUnavailableCheckbox.isSelected(),
                table,
                context
        );

        BackgroundCallback<ProcessExtraVideoFilesRunner.Result> callback = runnerResult -> {
            totalResult.setActionResult(runnerResult.getActionResult());

            if (!runnerResult.getActionResult().haveErrors()) {
                table.setData(runnerResult.getTableData(), true);

                allVideos = runnerResult.getAllVideos();
                allTableVideos = runnerResult.getAllTableVideos();
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    private static List<File> getVideoFiles(Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose videos");
        fileChooser.setInitialDirectory(settings.getVideoDirectory());
        fileChooser.getExtensionFilters().add(GuiConstants.VIDEO_EXTENSION_FILTER);

        return fileChooser.showOpenMultipleDialog(stage);
    }
}
