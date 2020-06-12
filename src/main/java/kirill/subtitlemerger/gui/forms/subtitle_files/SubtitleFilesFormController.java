package kirill.subtitlemerger.gui.forms.subtitle_files;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.common_controls.ActionResultLabel;
import kirill.subtitlemerger.gui.forms.common.BackgroundTaskFormController;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.Popups;
import kirill.subtitlemerger.gui.utils.background.BackgroundCallback;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.entities.FileOrigin;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.settings.SettingType;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.subtitles.SubRipWriter;
import kirill.subtitlemerger.logic.subtitles.SubtitleMerger;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitleFormat;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndOutput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.utils.entities.ActionResultType;
import kirill.subtitlemerger.logic.utils.file_validation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CommonsLog
public class SubtitleFilesFormController extends BackgroundTaskFormController {
    @FXML
    private TextField upperPathField;

    @FXML
    private Button upperPreviewButton;

    @FXML
    private Button upperChooseButton;

    @FXML
    private TextField lowerPathField;

    @FXML
    private Button lowerPreviewButton;

    @FXML
    private Button lowerChooseButton;

    @FXML
    private TextField mergedPathField;

    @FXML
    private Button mergedPreviewButton;

    @FXML
    private Button mergedChooseButton;

    @FXML
    private Button mergeButton;

    @FXML
    private ActionResultLabel totalResultLabel;

    private Stage stage;

    private Settings settings;

    private InputSubtitlesInfo upperSubtitlesInfo;

    private InputSubtitlesInfo lowerSubtitlesInfo;

    /*
     * Unlike with input subtitles, information regarding merged subtitles should be split between two variables
     * because subtitles can be merged without selecting a file to write the result to (through the preview) and vice
     * verse - selecting a file doesn't affect the merged subtitles themselves.
     */
    private MergedSubtitlesFileInfo mergedSubtitlesFileInfo;

    private Subtitles mergedSubtitles;

    /*
     * We need this special flag because otherwise the popup window will be shown twice if we change the value and press
     * enter. Because pressing the enter button will fire an event but after the popup window is opened another event
     * (losing the text field's focus) is fired.
     */
    private boolean overwritePopupDisplayed;

    public void initialize(Stage stage, GuiContext context) {
        this.stage = stage;
        settings = context.getSettings();

        GuiUtils.setTextEnteredHandler(
                upperPathField,
                (path) -> processInputSubtitlesPath(path, InputSubtitlesType.UPPER, FileOrigin.TEXT_FIELD)
        );
        GuiUtils.setTextEnteredHandler(
                lowerPathField,
                (path) -> processInputSubtitlesPath(path, InputSubtitlesType.LOWER, FileOrigin.TEXT_FIELD)
        );
        GuiUtils.setTextEnteredHandler(
                mergedPathField,
                (path) -> processMergedSubtitlesPath(path, FileOrigin.TEXT_FIELD)
        );
    }

    private void processInputSubtitlesPath(String path, InputSubtitlesType subtitleType, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && pathNotChanged(path, subtitleType.getBroaderType())) {
            return;
        }

        BackgroundRunner<InputSubtitlesInfo> backgroundRunner = backgroundManager -> {
            backgroundManager.setCancelPossible(false);
            backgroundManager.setIndeterminateProgress();
            backgroundManager.updateMessage("Processing " + path + "...");
            return getInputSubtitlesInfo(path, subtitleType, fileOrigin);
        };

        BackgroundCallback<InputSubtitlesInfo> callback = subtitleInfo -> {
            updateSubtitleInfoVariable(subtitleInfo, subtitleType);
            markOtherSubtitlesNotDuplicate(subtitleType);
            if (fileOrigin == FileOrigin.FILE_CHOOSER && subtitleInfo != null) {
                saveDirectoryInSettings(subtitleInfo.getFile(), subtitleType.getBroaderType(), settings);
            }
            mergedSubtitles = null;

            updateScene(fileOrigin);
        };

        runInBackground(backgroundRunner, callback);
    }

    private boolean pathNotChanged(String path, SubtitleType subtitleType) {
        String currentPath;
        if (subtitleType == SubtitleType.UPPER) {
            currentPath = upperSubtitlesInfo != null ? upperSubtitlesInfo.getPath() : "";
        } else if (subtitleType == SubtitleType.LOWER) {
            currentPath = lowerSubtitlesInfo != null ? lowerSubtitlesInfo.getPath() : "";
        } else if (subtitleType == SubtitleType.MERGED) {
            currentPath = mergedSubtitlesFileInfo != null ? mergedSubtitlesFileInfo.getPath() : "";
        } else {
            log.error("unexpected subtitle type: " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }

        return Objects.equals(path, currentPath);
    }

    @Nullable
    private InputSubtitlesInfo getInputSubtitlesInfo(
            String path,
            InputSubtitlesType subtitleType,
            FileOrigin fileOrigin
    ) {
        InputFileValidationOptions validationOptions = InputFileValidationOptions.builder()
                .allowedExtensions(SubtitleFormat.SUB_RIP.getExtensions())
                .allowEmpty(false)
                .maxAllowedSize(LogicConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024L)
                .loadContent(true)
                .build();
        InputFileInfo fileInfo = FileValidator.getInputFileInfo(path, validationOptions);
        if (fileInfo.getNotValidReason() == InputFileNotValidReason.PATH_IS_EMPTY) {
            return null;
        }

        if (fileInfo.getNotValidReason() != null) {
            return new InputSubtitlesInfo(
                    path,
                    fileInfo.getFile(),
                    fileOrigin,
                    fileInfo.getNotValidReason(),
                    isDuplicate(fileInfo.getFile(), subtitleType),
                    null
            );
        }

        return new InputSubtitlesInfo(
                path,
                fileInfo.getFile(),
                fileOrigin,
                null,
                isDuplicate(fileInfo.getFile(), subtitleType),
                SubtitlesAndInput.from(fileInfo.getContent(), StandardCharsets.UTF_8)
        );
    }

    private boolean isDuplicate(File file, InputSubtitlesType subtitleType) {
        InputSubtitlesInfo otherSubtitlesInfo;
        if (subtitleType == InputSubtitlesType.UPPER) {
            if (lowerSubtitlesInfo == null) {
                return false;
            }

            otherSubtitlesInfo = lowerSubtitlesInfo;
        } else if (subtitleType == InputSubtitlesType.LOWER) {
            if (upperSubtitlesInfo == null) {
                return false;
            }

            otherSubtitlesInfo = upperSubtitlesInfo;
        } else {
            log.error("unexpected subtitle type: " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }

        return Objects.equals(file, otherSubtitlesInfo.getFile());
    }

    private void updateSubtitleInfoVariable(InputSubtitlesInfo subtitleInfo, InputSubtitlesType subtitleType) {
        if (subtitleType == InputSubtitlesType.UPPER) {
            upperSubtitlesInfo = subtitleInfo;
        } else if (subtitleType == InputSubtitlesType.LOWER) {
            lowerSubtitlesInfo = subtitleInfo;
        } else {
            log.error("unexpected subtitle type: " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    /*
     * Only one of the two input subtitles should ever be marked as duplicate, so after we process the subtitles we
     * should always mark the other subtitles as non-duplicate.
     */
    private void markOtherSubtitlesNotDuplicate(InputSubtitlesType subtitleType) {
        if (subtitleType == InputSubtitlesType.UPPER) {
            if (lowerSubtitlesInfo != null) {
                lowerSubtitlesInfo.setDuplicate(false);
            }
        } else if (subtitleType == InputSubtitlesType.LOWER) {
            if (upperSubtitlesInfo != null) {
                upperSubtitlesInfo.setDuplicate(false);
            }
        } else {
            log.error("unexpected subtitle type: " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private static void saveDirectoryInSettings(File file, SubtitleType subtitleType, Settings settings) {
        switch (subtitleType) {
            case UPPER:
                settings.saveQuietly(file.getParentFile(), SettingType.LAST_DIRECTORY_WITH_UPPER_SUBTITLES);
                return;
            case LOWER:
                settings.saveQuietly(file.getParentFile(), SettingType.LAST_DIRECTORY_WITH_LOWER_SUBTITLES);
                return;
            case MERGED:
                settings.saveQuietly(file.getParentFile(), SettingType.LAST_DIRECTORY_WITH_MERGED_SUBTITLES);
                return;
            default:
                log.error("unexpected subtitle type: " + subtitleType + ", most likely a bug");
                throw new IllegalStateException();
        }
    }

    private void updateScene(FileOrigin fileOrigin) {
        clearState();

        if (fileOrigin != FileOrigin.TEXT_FIELD) {
            updatePathFields();
        }

        setPreviewButtonsVisibility();
        setMergeButtonVisibility();
        showErrors();
    }

    private void clearState() {
        upperPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        upperChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        lowerPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        lowerChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        mergedPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        mergedChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);

        totalResultLabel.clear();
    }

    private void updatePathFields() {
        upperPathField.setText(upperSubtitlesInfo != null ? upperSubtitlesInfo.getFile().getAbsolutePath() : "");
        lowerPathField.setText(lowerSubtitlesInfo != null ? lowerSubtitlesInfo.getFile().getAbsolutePath() : "");
        mergedPathField.setText(
                mergedSubtitlesFileInfo != null ? mergedSubtitlesFileInfo.getFile().getAbsolutePath() : ""
        );
    }

    private void setPreviewButtonsVisibility() {
        upperPreviewButton.setDisable(inputPreviewNotAvailable(upperSubtitlesInfo));
        lowerPreviewButton.setDisable(inputPreviewNotAvailable(lowerSubtitlesInfo));
        mergedPreviewButton.setDisable(cantBeMerged(upperSubtitlesInfo) || cantBeMerged(lowerSubtitlesInfo));
    }

    /*
     * Note that a preview is available if the format is incorrect because it should be possible to open the preview and
     * change the encoding if that's what is causing the problem.
     */
    private static boolean inputPreviewNotAvailable(InputSubtitlesInfo subtitleInfo) {
        return subtitleInfo == null || subtitleInfo.isDuplicate() || subtitleInfo.getNotValidReason() != null;
    }

    /*
     * Note that unlike with previews merging should not be available if the format is incorrect.
     */
    private static boolean cantBeMerged(InputSubtitlesInfo subtitleInfo) {
        return subtitleInfo == null || subtitleInfo.isDuplicate() || subtitleInfo.getNotValidReason() != null
                || subtitleInfo.incorrectFormat();
    }

    private void setMergeButtonVisibility() {
        boolean disable = cantBeMerged(upperSubtitlesInfo) || cantBeMerged(lowerSubtitlesInfo)
                || mergedSubtitlesFileInfo == null || mergedSubtitlesFileInfo.getNotValidReason() != null;
        mergeButton.setDisable(disable);
    }

    private void showErrors() {
        List<String> errorParts = new ArrayList<>();

        String upperSubtitlesError = getInputSubtitlesError(upperSubtitlesInfo);
        if (!StringUtils.isBlank(upperSubtitlesError)) {
            displayFileElementsAsIncorrect(SubtitleType.UPPER);
            errorParts.add(upperSubtitlesError);
        }

        String lowerSubtitlesError = getInputSubtitlesError(lowerSubtitlesInfo);
        if (!StringUtils.isBlank(lowerSubtitlesError)) {
            displayFileElementsAsIncorrect(SubtitleType.LOWER);
            errorParts.add(lowerSubtitlesError);
        }

        String mergedSubtitlesError = getMergedSubtitlesError(mergedSubtitlesFileInfo);
        if (!StringUtils.isBlank(mergedSubtitlesError)) {
            displayFileElementsAsIncorrect(SubtitleType.MERGED);
            errorParts.add(mergedSubtitlesError);
        }

        if (CollectionUtils.isEmpty(errorParts)) {
            return;
        }

        for (int i = 0; i < errorParts.size(); i++) {
            errorParts.set(i, "\u2022 " + errorParts.get(i));
        }

        totalResultLabel.setError(StringUtils.join(errorParts, System.lineSeparator()));
    }

    @Nullable
    private static String getInputSubtitlesError(InputSubtitlesInfo subtitleInfo) {
        if (subtitleInfo == null) {
            return null;
        }

        if (subtitleInfo.isDuplicate()) {
            return "You have already selected this file for the other subtitles";
        }

        String shortenedPath = getShortenedPath(subtitleInfo.getPath());

        if (subtitleInfo.getNotValidReason() != null) {
            InputFileNotValidReason notValidReason = subtitleInfo.getNotValidReason();
            switch (notValidReason) {
                case PATH_IS_TOO_LONG:
                    return "The file path is too long";
                case INVALID_PATH:
                    return "The file path is invalid";
                case IS_A_DIRECTORY:
                    return "'" + shortenedPath + "' is a directory, not a file";
                case DOES_NOT_EXIST:
                    return "The file '" + shortenedPath + "' doesn't exist";
                case NO_EXTENSION:
                    return "The file '" + shortenedPath + "' has no extension";
                case NOT_ALLOWED_EXTENSION:
                    return "The file '" + shortenedPath + "' has an incorrect extension";
                case FILE_IS_EMPTY:
                    return "The file '" + shortenedPath + "' is empty";
                case FILE_IS_TOO_BIG:
                    return "The file '" + shortenedPath + "' is too big (>"
                            + LogicConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES + " megabytes)";
                case FAILED_TO_READ_CONTENT:
                    return shortenedPath + ": failed to read the file";
                default:
                    log.error("unexpected input subtitle not valid reason: " + notValidReason + ", most likely a bug");
                    throw new IllegalStateException();
            }
        }

        if (subtitleInfo.incorrectFormat()) {
            return "Subtitles in '" + shortenedPath + "'can't be parsed, it can happen if a file is not UTF-8-encoded; "
                    + "you can change the encoding after pressing the preview button";
        }

        return null;
    }

    private static String getShortenedPath(String path) {
        return Utils.getShortenedString(path, 0, 64);
    }

    @Nullable
    private static String getMergedSubtitlesError(MergedSubtitlesFileInfo subtitleFileInfo) {
        if (subtitleFileInfo == null) {
            return null;
        }

        if (subtitleFileInfo.getNotValidReason() != null) {
            String shortenedPath = getShortenedPath(subtitleFileInfo.getPath());

            OutputFileNotValidReason notValidReason = subtitleFileInfo.getNotValidReason();
            switch (notValidReason) {
                case PATH_IS_TOO_LONG:
                    return "The file path is too long";
                case INVALID_PATH:
                    return "The file path is invalid";
                case IS_A_DIRECTORY:
                    return "'" + shortenedPath + "' is a directory, not a file";
                case NO_EXTENSION:
                    return "The file '" + shortenedPath + "' has no extension";
                case NOT_ALLOWED_EXTENSION:
                    return "The file '" + shortenedPath + "' has an incorrect extension";
                default:
                    log.error("unexpected merged subtitle not valid reason: " + notValidReason + ", most likely a bug");
                    throw new IllegalStateException();
            }
        }

        return null;
    }

    private void displayFileElementsAsIncorrect(SubtitleType subtitleType) {
        if (subtitleType == SubtitleType.UPPER) {
            upperPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            upperPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);

            if (upperSubtitlesInfo.getFileOrigin() == FileOrigin.FILE_CHOOSER) {
                upperChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
                upperChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        } else if (subtitleType == SubtitleType.LOWER) {
            lowerPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            lowerPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);

            if (lowerSubtitlesInfo.getFileOrigin() == FileOrigin.FILE_CHOOSER) {
                lowerChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
                lowerChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        } else if (subtitleType == SubtitleType.MERGED) {
            mergedPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            mergedPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);

            if (mergedSubtitlesFileInfo.getFileOrigin() == FileOrigin.FILE_CHOOSER) {
                mergedChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
                mergedChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        } else {
            log.error("unexpected subtitle type: " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private void processMergedSubtitlesPath(String path, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && pathNotChanged(path, SubtitleType.MERGED)) {
            return;
        }

        MergedSubtitlesFileInfo subtitleFileInfo = getMergedSubtitlesFileInfo(path, fileOrigin);
        if (fileOrigin == FileOrigin.TEXT_FIELD && fileExists(subtitleFileInfo)) {
            if (overwritePopupDisplayed) {
                return;
            }

            if (!agreeToOverwrite(subtitleFileInfo)) {
                mergedPathField.setText(subtitleFileInfo.getPath());
                return;
            }
        }

        mergedSubtitlesFileInfo = subtitleFileInfo;
        if (fileOrigin == FileOrigin.FILE_CHOOSER && subtitleFileInfo != null) {
            saveDirectoryInSettings(subtitleFileInfo.getFile(), SubtitleType.MERGED, settings);
        }

        updateScene(fileOrigin);
    }

    @Nullable
    private static MergedSubtitlesFileInfo getMergedSubtitlesFileInfo(String path, FileOrigin fileOrigin) {
        OutputFileValidationOptions validationOptions = new OutputFileValidationOptions(
                SubtitleFormat.SUB_RIP.getExtensions(),
                true
        );
        OutputFileInfo validatorFileInfo = FileValidator.getOutputFileInfo(path, validationOptions);
        if (validatorFileInfo.getNotValidReason() == OutputFileNotValidReason.PATH_IS_EMPTY) {
            return null;
        }

        return new MergedSubtitlesFileInfo(
                path,
                validatorFileInfo.getFile(),
                fileOrigin,
                validatorFileInfo.getNotValidReason()
        );
    }

    private static boolean fileExists(MergedSubtitlesFileInfo subtitleFileInfo) {
        return subtitleFileInfo != null
                /* This condition is important, there shouldn't be any popups for not valid files. */
                && subtitleFileInfo.getNotValidReason() == null
                && subtitleFileInfo.getFile().exists();
    }

    private boolean agreeToOverwrite(MergedSubtitlesFileInfo subtitleFileInfo) {
        overwritePopupDisplayed = true;

        String fileName = Utils.getShortenedString(
                subtitleFileInfo.getFile().getName(),
                0,
                64
        );

        boolean result = Popups.askAgreement(
                "The file '" + fileName + "' already exists. Do you want to overwrite it?",
                "Yes",
                "No",
                stage
        );

        overwritePopupDisplayed = false;

        return result;
    }

    @FXML
    private void upperPreviewClicked() {
        Charset previousEncoding = upperSubtitlesInfo.getEncoding();
        SubtitlesAndInput previewSelection = Popups.showEncodingPreview(
                Utils.getShortenedString(upperSubtitlesInfo.getPath(), 0, 64),
                upperSubtitlesInfo.getSubtitlesAndInput(),
                stage
        );

        upperSubtitlesInfo.setSubtitlesAndInput(previewSelection);
        if (!Objects.equals(previousEncoding, previewSelection.getEncoding())) {
            mergedSubtitles = null;
        }
        updateScene(upperSubtitlesInfo.getFileOrigin());
    }

    @FXML
    private void upperChooserClicked() {
        clearState();

        File file = getInputFile(InputSubtitlesType.UPPER, stage, settings);
        if (file == null) {
            clearState();
            return;
        }

        processInputSubtitlesPath(file.getAbsolutePath(), InputSubtitlesType.UPPER, FileOrigin.FILE_CHOOSER);
    }

    private static File getInputFile(InputSubtitlesType subtitleType, Stage stage, Settings settings) {
        String chooserTitle;
        File chooserInitialDirectory;
        if (subtitleType == InputSubtitlesType.UPPER) {
            chooserTitle = "Please choose a file with upper subtitles";
            chooserInitialDirectory = ObjectUtils.firstNonNull(
                    settings.getLastDirectoryWithUpperSubtitles(),
                    settings.getLastDirectoryWithLowerSubtitles(),
                    settings.getLastDirectoryWithMergedSubtitles()
            );
        } else if (subtitleType == InputSubtitlesType.LOWER) {
            chooserTitle = "Please choose a file with lower subtitles";
            chooserInitialDirectory = ObjectUtils.firstNonNull(
                    settings.getLastDirectoryWithLowerSubtitles(),
                    settings.getLastDirectoryWithUpperSubtitles(),
                    settings.getLastDirectoryWithMergedSubtitles()
            );
        } else {
            log.error("unexpected subtitle type: " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle(chooserTitle);
        fileChooser.setInitialDirectory(chooserInitialDirectory);
        fileChooser.getExtensionFilters().add(GuiConstants.SUBTITLE_EXTENSION_FILTER);

        return fileChooser.showOpenDialog(stage);
    }

    @FXML
    private void lowerPreviewClicked() {
        Charset previousEncoding = lowerSubtitlesInfo.getEncoding();
        clearState();

        SubtitlesAndInput previewSelection = Popups.showEncodingPreview(
                Utils.getShortenedString(lowerSubtitlesInfo.getPath(), 0, 64),
                lowerSubtitlesInfo.getSubtitlesAndInput(),
                stage
        );

        lowerSubtitlesInfo.setSubtitlesAndInput(previewSelection);
        if (!Objects.equals(previousEncoding, previewSelection.getEncoding())) {
            mergedSubtitles = null;
        }
        updateScene(lowerSubtitlesInfo.getFileOrigin());
    }

    @FXML
    private void lowerChooserClicked() {
        File file = getInputFile(InputSubtitlesType.LOWER, stage, settings);
        if (file == null) {
            clearState();
            return;
        }

        processInputSubtitlesPath(file.getAbsolutePath(), InputSubtitlesType.LOWER, FileOrigin.FILE_CHOOSER);
    }

    @FXML
    private void mergedPreviewClicked() {
        clearState();

        BackgroundRunner<SubtitlesAndOutput> backgroundRunner = backgroundManager -> {
            if (mergedSubtitles != null) {
                return SubtitlesAndOutput.from(mergedSubtitles, settings.isPlainTextSubtitles());
            }

            backgroundManager.setCancelPossible(true);
            backgroundManager.setIndeterminateProgress();
            backgroundManager.updateMessage("Merging the subtitles...");
            try {
                return SubtitlesAndOutput.from(
                        SubtitleMerger.mergeSubtitles(
                                upperSubtitlesInfo.getSubtitles(),
                                lowerSubtitlesInfo.getSubtitles()
                        ),
                        settings.isPlainTextSubtitles()
                );
            } catch (InterruptedException e) {
                return null;
            }
        };

        BackgroundCallback<SubtitlesAndOutput> callback = subtitlesAndOutput -> {
            if (subtitlesAndOutput == null) {
                totalResultLabel.setWarning("Merging has been canceled");
                return;
            }

            mergedSubtitles = subtitlesAndOutput.getSubtitles();

            Popups.showMergedSubtitlesPreview(
                    upperSubtitlesInfo.getPath(),
                    lowerSubtitlesInfo.getPath(),
                    subtitlesAndOutput.getText(),
                    stage
            );
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void mergedChooserClicked() {
        File file = getMergedFile(stage, settings);
        if (file == null) {
            return;
        }

        processMergedSubtitlesPath(file.getAbsolutePath(), FileOrigin.FILE_CHOOSER);
    }

    @Nullable
    private static File getMergedFile(Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose where to save the result");
        fileChooser.setInitialDirectory(
                ObjectUtils.firstNonNull(
                        settings.getLastDirectoryWithMergedSubtitles(),
                        settings.getLastDirectoryWithUpperSubtitles(),
                        settings.getLastDirectoryWithLowerSubtitles()
                )
        );
        fileChooser.getExtensionFilters().add(GuiConstants.SUBTITLE_EXTENSION_FILTER);

        return fileChooser.showSaveDialog(stage);
    }

    @FXML
    private void mergeClicked() {
        clearState();

        BackgroundRunner<ActionResult> backgroundRunner = backgroundManager -> {
            if (mergedSubtitles == null) {
                backgroundManager.setCancelPossible(true);
                backgroundManager.setIndeterminateProgress();
                backgroundManager.updateMessage("Merging the subtitles...");
                try {
                    mergedSubtitles = SubtitleMerger.mergeSubtitles(
                            upperSubtitlesInfo.getSubtitles(),
                            lowerSubtitlesInfo.getSubtitles()
                    );
                } catch (InterruptedException e) {
                    return ActionResult.warning("Merging has been canceled");
                }
            }

            try {
                backgroundManager.setCancelPossible(false);
                backgroundManager.setIndeterminateProgress();
                backgroundManager.updateMessage("Writing the result...");

                FileUtils.writeStringToFile(
                        mergedSubtitlesFileInfo.getFile(),
                        SubRipWriter.toText(mergedSubtitles, settings.isPlainTextSubtitles()),
                        StandardCharsets.UTF_8
                );

                return ActionResult.success("The subtitles have been merged successfully!");
            } catch (IOException e) {
                return ActionResult.error(
                        "Failed to write the merged subtitles to the file, please check access to the file"
                );
            }
        };

        BackgroundCallback<ActionResult> callback = actionResult -> {
            if (actionResult.getType() == ActionResultType.ERROR) {
                displayFileElementsAsIncorrect(SubtitleType.MERGED);
            }
            totalResultLabel.setActionResult(actionResult);
        };

        runInBackground(backgroundRunner, callback);
    }

    @AllArgsConstructor
    @Getter
    private enum InputSubtitlesType {
        UPPER(SubtitleType.UPPER),

        LOWER(SubtitleType.LOWER);

        private SubtitleType broaderType;
    }

    private enum SubtitleType {
        UPPER,
        LOWER,
        MERGED
    }

    @AllArgsConstructor
    @Getter
    private static class InputSubtitlesInfo {
        private String path;

        private File file;

        private FileOrigin fileOrigin;

        private InputFileNotValidReason notValidReason;

        @Setter
        private boolean isDuplicate;

        @Setter
        private SubtitlesAndInput subtitlesAndInput;

        @Nullable
        Subtitles getSubtitles() {
            return subtitlesAndInput != null ? subtitlesAndInput.getSubtitles() : null;
        }

        @Nullable
        Charset getEncoding() {
            return subtitlesAndInput != null ? subtitlesAndInput.getEncoding() : null;
        }

        boolean incorrectFormat() {
            return subtitlesAndInput != null && !subtitlesAndInput.isCorrectFormat();
        }
    }

    @AllArgsConstructor
    @Getter
    private static class MergedSubtitlesFileInfo {
        private String path;

        private File file;

        private FileOrigin fileOrigin;

        private OutputFileNotValidReason notValidReason;
    }
}