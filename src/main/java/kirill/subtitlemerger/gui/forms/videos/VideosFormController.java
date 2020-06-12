package kirill.subtitlemerger.gui.forms.videos;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.common_controls.MultiPartActionResultPane;
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
import kirill.subtitlemerger.logic.utils.entities.MultiPartActionResult;
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
    private MultiPartActionResultPane totalResultPane;

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

    private GuiContext context;

    private Ffprobe ffprobe;

    private Ffmpeg ffmpeg;

    private Settings settings;

    private String directoryPath;

    private List<Video> allVideos;

    /*
     * This variable stores all (meaning both available and unavailable) videos and the videos have to be sorted. This
     * variable is necessary and the table's getRows() method isn't enough because if the user presses the "hide
     * unavailable" button, the unavailable videos will be removed from the table.
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
        this.context = context;
        ffprobe = context.getFfprobe();
        ffmpeg = context.getFfmpeg();
        settings = context.getSettings();

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
            context.setVideosInProgress(false);
        } else if (activePane == ActivePane.MAIN) {
            missingSettingsFormController.hide();
            choiceFormController.hide();
            context.setVideosInProgress(true);
            show();
        } else {
            log.error("unexpected active pane: " + activePane + ", most likely a bug");
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
        table.setSingleSubtitlesLoader(this::loadSingleSubtitles);
        table.setAllVideoSubtitlesLoader(this::loadAllVideoSubtitles);
        table.setSubtitleOptionPreviewHandler(this::handleSubtitleOptionPreview);
        table.setMergedSubtitlesPreviewHandler(this::handleMergedSubtitlesPreview);
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
        totalResultPane.clear();
        tableAndActionsPane.setDisable(false);
        table.clearTable();

        directoryPath = path;
        if (fileOrigin == FileOrigin.FILE_CHOOSER) {
            settings.saveQuietly(new File(path), SettingType.LAST_DIRECTORY_WITH_VIDEOS);
        }
        lastProcessedVideo = null;

        ProcessDirectoryRunner backgroundRunner = new ProcessDirectoryRunner(directoryPath, table, ffprobe, settings);

        BackgroundCallback<ProcessDirectoryRunner.Result> callback = runnerResult -> {
            if (runnerResult.getNotValidReason() != null) {
                chosenDirectoryField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                refreshButton.setDisable(runnerResult.isDisableRefresh());
                totalResultPane.setOnlyError(runnerResult.getNotValidReason());
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
            tooltip = GuiUtils.getTooltip("Please select at least one video");
        } else if (selectedUnavailableCount > 0) {
            disable = true;
            tooltip = GuiUtils.getTooltip("Please select only available videos");
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
            backgroundManager.updateMessage("Processing the videos...");

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
        totalResultPane.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        Video video = Video.getById(tableVideo.getId(), allVideos);
        File subtitleFile = getSubtitleFile(video, stage, settings);
        if (subtitleFile == null) {
            return;
        }
        settings.saveQuietly(subtitleFile.getParentFile(), SettingType.LAST_DIRECTORY_WITH_VIDEO_SUBTITLES);

        ProcessSubtitleFileRunner backgroundRunner = new ProcessSubtitleFileRunner(subtitleFile, video, tableVideo);

        BackgroundCallback<ProcessSubtitleFileRunner.Result> callback = runnerResult -> {
            tableVideo.setActionResult(runnerResult.getActionResult());
            if (runnerResult.getOption() != null && runnerResult.getTableOption() != null) {
                video.getOptions().add(runnerResult.getOption());
                tableVideo.addOption(runnerResult.getTableOption());
            }

            lastProcessedVideo = tableVideo;
        };

        runInBackground(backgroundRunner, callback);
    }

    private void clearLastProcessedResult() {
        if (lastProcessedVideo != null) {
            lastProcessedVideo.clearActionResult();
        }
    }

    @Nullable
    private static File getSubtitleFile(Video video, Stage stage, Settings settings) {
        File initialDirectory = settings.getLastDirectoryWithVideoSubtitles();
        if (initialDirectory == null) {
            initialDirectory = video.getFile().getParentFile();
        }

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose a file with subtitles");
        fileChooser.setInitialDirectory(initialDirectory);
        fileChooser.getExtensionFilters().add(GuiConstants.SUBTITLE_EXTENSION_FILTER);

        return fileChooser.showOpenDialog(stage);
    }

    private void removeSubtitleFile(TableSubtitleOption tableOption) {
        TableVideo tableVideo = tableOption.getVideo();

        totalResultPane.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        Video video = Video.getById(tableVideo.getId(), allVideos);
        SubtitleOption option = video.getOption(tableOption.getId());
        video.getOptions().remove(option);

        String successMessage = "The file with subtitles has been removed successfully";
        tableVideo.removeOption(tableOption, MultiPartActionResult.onlySuccess(successMessage));
        lastProcessedVideo = tableVideo;
    }

    private void loadSingleSubtitles(TableSubtitleOption tableOption) {
        TableVideo tableVideo = tableOption.getVideo();

        totalResultPane.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        BackgroundRunner<MultiPartActionResult> backgroundRunner = backgroundManager -> {
            Video video = Video.getById(tableVideo.getId(), allVideos);

            backgroundManager.setCancelPossible(true);
            backgroundManager.setCancelDescription(getLoadingCancelDescription(video));
            backgroundManager.setIndeterminateProgress();
            String action = "Loading " + tableOption.getTitle() + " in " + video.getFile().getName() + "...";
            backgroundManager.updateMessage(action);
            try {
                BuiltInSubtitleOption option = video.getBuiltInOption(tableOption.getId());
                LoadSubtitlesResult loadResult = loadSubtitles(option, video, tableOption, ffmpeg);
                if (loadResult == LoadSubtitlesResult.SUCCESS) {
                    return MultiPartActionResult.onlySuccess("The subtitles have been loaded successfully");
                } else if (loadResult == LoadSubtitlesResult.INCORRECT_FORMAT) {
                    return MultiPartActionResult.onlyWarning(
                            "The subtitles have been loaded but have an incorrect format"
                    );
                } else if (loadResult == LoadSubtitlesResult.FAILED) {
                    return MultiPartActionResult.onlyError("Failed to load the subtitles");
                } else {
                    log.error("unexpected load result: " + loadResult + ", most likely a bug");
                    throw new IllegalStateException();
                }
            } catch (InterruptedException e) {
                return MultiPartActionResult.onlyWarning("The task has been canceled");
            }
        };

        BackgroundCallback<MultiPartActionResult> callback = actionResult -> {
            tableVideo.setActionResult(actionResult);
            lastProcessedVideo = tableVideo;
        };

        runInBackground(backgroundRunner, callback);
    }

    private void loadAllVideoSubtitles(TableVideo tableVideo) {
        totalResultPane.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        BackgroundRunner<MultiPartActionResult> backgroundRunner = backgroundManager -> {
            Video video = Video.getById(tableVideo.getId(), allVideos);

            backgroundManager.setCancelPossible(true);
            backgroundManager.setCancelDescription(getLoadingCancelDescription(video));
            backgroundManager.setIndeterminateProgress();

            int toLoadCount = video.getOptionsToLoad().size();
            int processedCount = 0;
            int successfulCount = 0;
            int incorrectCount = 0;
            int failedCount = 0;
            try {
                for (BuiltInSubtitleOption option : video.getOptionsToLoad()) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    TableSubtitleOption tableOption = tableVideo.getOption(option.getId());

                    String action = "Loading " + tableOption.getTitle() + " in " + video.getFile().getName() + "...";
                    backgroundManager.updateMessage(getProgressAction(processedCount, toLoadCount, action));

                    LoadSubtitlesResult loadResult = loadSubtitles(option, video, tableOption, ffmpeg);
                    if (loadResult == LoadSubtitlesResult.SUCCESS) {
                        successfulCount++;
                    } else if (loadResult == LoadSubtitlesResult.INCORRECT_FORMAT) {
                        incorrectCount++;
                    } else if (loadResult == LoadSubtitlesResult.FAILED) {
                        failedCount++;
                    } else {
                        log.error("unexpected load result: " + loadResult + ", most likely a bug");
                        throw new IllegalStateException();
                    }

                    processedCount++;
                }
            } catch (InterruptedException e) {
                /* Do nothing here, will just return a result based on the work done. */
            }

            return getLoadSubtitlesResult(toLoadCount, processedCount, successfulCount, incorrectCount, failedCount);
        };

        BackgroundCallback<MultiPartActionResult> callback = actionResult -> {
            tableVideo.setActionResult(actionResult);
            lastProcessedVideo = tableVideo;
        };

        runInBackground(backgroundRunner, callback);
    }

    private void handleSubtitleOptionPreview(TableSubtitleOption tableOption) {
        TableVideo tableVideo = tableOption.getVideo();

        totalResultPane.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        Video video = Video.getById(tableVideo.getId(), allVideos);
        SubtitleOption option = video.getOption(tableOption.getId());
        SubtitlesAndInput subtitlesAndInput = option.getSubtitlesAndInput();
        if (subtitlesAndInput == null) {
            log.error("subtitles are not loaded before a preview, most likely a bug");
            throw new IllegalStateException();
        }

        if (option instanceof BuiltInSubtitleOption) {
            String title = Utils.getShortenedString(video.getFile().getName(), 0, 64)
                    + ", " + Utils.getShortenedString(tableOption.getTitle(), 64, 0);
            String subtitleText = new String(subtitlesAndInput.getRawData(), subtitlesAndInput.getEncoding());
            Popups.showSimpleSubtitlesPreview(title, subtitleText, stage);
        } else if (option instanceof ExternalSubtitleOption) {
            ExternalSubtitleOption externalOption = (ExternalSubtitleOption) option;

            SubtitlesAndInput previewSelection = Popups.showEncodingPreview(
                    Utils.getShortenedString(tableOption.getTitle(), 0, 64),
                    subtitlesAndInput,
                    stage
            );
            externalOption.setSubtitlesAndInput(previewSelection);

            if (!StringUtils.isBlank(tableOption.getNotValidReason()) && previewSelection.isCorrectFormat()) {
                tableOption.markAsValid();
            }
        } else {
            log.error("unexpected subtitle option class: " + option.getClass() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private void handleMergedSubtitlesPreview(TableVideo tableVideo) {
        totalResultPane.clear();
        clearLastProcessedResult();
        tableVideo.clearActionResult();

        Video video = Video.getById(tableVideo.getId(), allVideos);

        MergedPreviewRunner mergedPreviewRunner = new MergedPreviewRunner(video, tableVideo, ffmpeg, settings);

        BackgroundCallback<MergedPreviewRunner.Result> callback = runnerResult -> {
            if (runnerResult == null) {
                totalResultPane.setOnlyWarning("Merging has been canceled");
            } else if (!StringUtils.isBlank(runnerResult.getError())) {
                tableVideo.setActionResult(MultiPartActionResult.onlyError(runnerResult.getError()));
                lastProcessedVideo = tableVideo;
            } else {
                if (runnerResult.getSubtitleText() == null) {
                    log.error("subtitle text can't be null, most likely a bug");
                    throw new IllegalStateException();
                }

                Popups.showMergedSubtitlesPreview(
                        tableVideo.getUpperOption().getTitle(),
                        tableVideo.getLowerOption().getTitle(),
                        runnerResult.getSubtitleText(),
                        stage
                );
            }
        };

        runInBackground(mergedPreviewRunner, callback);
    }

    private void handleSortChange(TableSortBy tableSortBy, TableSortDirection tableSortDirection) {
        totalResultPane.clear();
        clearLastProcessedResult();

        settings.saveCorrect(sortByFrom(tableSortBy), SettingType.SORT_BY);
        settings.saveCorrect(sortDirectionFrom(tableSortDirection), SettingType.SORT_DIRECTION);

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

    @FXML
    private void backToSelectionClicked() {
        GuiUtils.setVisibleAndManaged(chosenDirectoryPane, false);
        chosenDirectoryField.setText(null);
        refreshButton.setDisable(false);
        totalResultPane.clear();
        tableAndActionsPane.setDisable(false);
        table.clearTable();
        GuiUtils.setVisibleAndManaged(addRemoveVideosPane, false);

        directoryPath = null;
        lastProcessedVideo = null;

        setActivePane(ActivePane.CHOICE);
    }

    @FXML
    private void refreshClicked() {
        chosenDirectoryField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        totalResultPane.clear();
        tableAndActionsPane.setDisable(false);
        table.clearTable();

        lastProcessedVideo = null;

        ProcessDirectoryRunner backgroundRunner = new ProcessDirectoryRunner(directoryPath, table, ffprobe, settings);

        BackgroundCallback<ProcessDirectoryRunner.Result> callback = runnerResult -> {
            if (runnerResult.getNotValidReason() != null) {
                chosenDirectoryField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                refreshButton.setDisable(runnerResult.isDisableRefresh());
                totalResultPane.setOnlyError(runnerResult.getNotValidReason());
                tableAndActionsPane.setDisable(true);

                allVideos = null;
                allTableVideos = null;
            } else {
                hideUnavailableCheckbox.setSelected(runnerResult.isHideUnavailable());
                table.setData(runnerResult.getTableData(), true);

                /* See the huge comment in the hideUnavailableClicked() method. */
                table.scrollTo(0);

                allVideos = runnerResult.getAllVideos();
                allTableVideos = runnerResult.getAllTableVideos();
            }
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void chooseAnotherClicked() {
        File directory = getDirectory(new File(directoryPath), stage);
        if (directory == null) {
            return;
        }

        processDirectoryPath(directory.getAbsolutePath(), FileOrigin.FILE_CHOOSER);
    }

    @Nullable
    private static File getDirectory(File initialDirectory, Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("Choose a directory with videos");
        directoryChooser.setInitialDirectory(initialDirectory);

        return directoryChooser.showDialog(stage);
    }

    @FXML
    private void hideUnavailableClicked() {
        totalResultPane.clear();
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
             * There is a strange bug with a TableView - when the list is shrunk in size (because for example the
             * "hide unavailable" checkbox is checked but it can also happen when the refresh is clicked I suppose) and
             * both the big list and the shrunk list have vertical scrollbars, the table isn't shrunk unless you move
             * the scrollbar. I've tried many workarounds but this one seems the best so far - just show the beginning
             * of the table. I couldn't find the bug with a precise description but these ones fit quite well -
             * https://bugs.openjdk.java.net/browse/JDK-8095384, https://bugs.openjdk.java.net/browse/JDK-8087833.
             */
            table.scrollTo(0);
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void autoSelectClicked() {
        totalResultPane.clear();
        lastProcessedVideo = null;

        AutoSelectRunner backgroundRunner = new AutoSelectRunner(table.getItems(), allVideos, ffmpeg, settings);
        BackgroundCallback<MultiPartActionResult> callback = totalResultPane::setActionResult;
        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void loadAllSubtitlesClicked() {
        totalResultPane.clear();
        lastProcessedVideo = null;

        AllSubtitlesLoader backgroundRunner = new AllSubtitlesLoader(table.getItems(), allVideos, ffmpeg);
        BackgroundCallback<MultiPartActionResult> callback = totalResultPane::setActionResult;
        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void mergeClicked() {
        totalResultPane.clear();
        lastProcessedVideo = null;

        MergeCheckRunner checkRunner = new MergeCheckRunner(table.getItems(), allVideos, settings);

        BackgroundCallback<MergeCheckRunner.Result> callback = runnerResult -> {
            if (!StringUtils.isBlank(runnerResult.getSelectionWarning())) {
                totalResultPane.setOnlyWarning(runnerResult.getSelectionWarning());
                return;
            }

            if (!StringUtils.isBlank(runnerResult.getFreeSpaceMessage())) {
                if (!Popups.askAgreement(runnerResult.getFreeSpaceMessage(), "yes", "no", stage)) {
                    totalResultPane.setOnlyWarning("Merging has been canceled");
                    return;
                }
            }

            List<File> confirmedFilesToOverwrite = new ArrayList<>();
            if (!CollectionUtils.isEmpty(runnerResult.getFilesToOverwrite())) {
                confirmedFilesToOverwrite = getConfirmedFilesToOverwrite(runnerResult.getFilesToOverwrite(), stage);
                if (confirmedFilesToOverwrite == null) {
                    totalResultPane.setOnlyWarning("Merging has been canceled");
                    return;
                }
            }

            MergeRunner mergeRunner = new MergeRunner(
                    runnerResult.getSelectedTableVideos(),
                    allVideos,
                    confirmedFilesToOverwrite,
                    runnerResult.getLargestFreeSpaceDirectory(),
                    context
            );
            BackgroundCallback<MultiPartActionResult> mergeCallback = totalResultPane::setActionResult;
            runInBackground(mergeRunner, mergeCallback);
        };

        runInBackground(checkRunner, callback);
    }

    @Nullable
    private static List<File> getConfirmedFilesToOverwrite(List<File> filesToOverwrite, Stage stage) {
        List<File> result = new ArrayList<>();

        int filesToOverwriteLeft = filesToOverwrite.size();
        for (File file : filesToOverwrite) {
            String fileName = Utils.getShortenedString(file.getName(), 0, 64);

            String applyToAllText;
            if (filesToOverwriteLeft == 1) {
                applyToAllText = null;
            } else {
                applyToAllText = String.format("Apply to all (%d left)", filesToOverwriteLeft);
            }

            AgreementResult agreementResult = Popups.askAgreement(
                    "The file '" + fileName + "' already exists. Do you want to overwrite it?",
                    applyToAllText,
                    "Yes",
                    "No",
                    stage
            );
            if (agreementResult == AgreementResult.CANCELED) {
                return null;
            } else if (agreementResult == AgreementResult.YES) {
                result.add(file);
            } else if (agreementResult == AgreementResult.YES_TO_ALL) {
                List<File> filesLeft = filesToOverwrite.subList(
                        filesToOverwrite.size() - filesToOverwriteLeft,
                        filesToOverwrite.size()
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
        totalResultPane.clear();
        clearLastProcessedResult();

        int initialVideoCount = allTableVideos.size();

        BackgroundRunner<TableData> backgroundRunner = backgroundManager -> {
            backgroundManager.setCancelPossible(false);
            backgroundManager.setIndeterminateProgress();
            backgroundManager.updateMessage("Removing the videos from the list...");

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
            int removedCount = initialVideoCount - allVideos.size();

            if (removedCount == 0) {
                log.error("no videos have been removed, most likely a bug");
                throw new IllegalStateException();
            } else if (removedCount == 1) {
                totalResultPane.setOnlySuccess("The video has been removed from the list successfully");
            } else {
                totalResultPane.setOnlySuccess(removedCount + " videos have been removed from the list successfully");
            }

            table.setData(tableData, false);
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void addVideosClicked() {
        totalResultPane.clear();
        clearLastProcessedResult();

        List<File> videoFilesToAdd = getVideoFiles(stage, settings);
        if (CollectionUtils.isEmpty(videoFilesToAdd)) {
            return;
        }
        settings.saveQuietly(videoFilesToAdd.get(0).getParentFile(), SettingType.LAST_DIRECTORY_WITH_VIDEOS);

        ProcessExtraVideoFilesRunner backgroundRunner = new ProcessExtraVideoFilesRunner(
                videoFilesToAdd,
                allVideos,
                allTableVideos,
                hideUnavailableCheckbox.isSelected(),
                table,
                context
        );

        BackgroundCallback<ProcessExtraVideoFilesRunner.Result> callback = runnerResult -> {
            totalResultPane.setActionResult(runnerResult.getActionResult());

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
        fileChooser.setInitialDirectory(settings.getLastDirectoryWithVideos());
        fileChooser.getExtensionFilters().add(GuiConstants.VIDEO_EXTENSION_FILTER);

        return fileChooser.showOpenMultipleDialog(stage);
    }

    void openSettingsForm() {
        mainFormController.openSettingsForm();
    }

    void processChosenVideoFiles(List<File> videoFiles) {
        settings.saveQuietly(videoFiles.get(0).getParentFile(), SettingType.LAST_DIRECTORY_WITH_VIDEOS);

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
}
