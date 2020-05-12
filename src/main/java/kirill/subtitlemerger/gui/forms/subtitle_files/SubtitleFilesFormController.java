package kirill.subtitlemerger.gui.forms.subtitle_files;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.common_controls.ActionResultPane;
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
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndOutput;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@CommonsLog
public class SubtitleFilesFormController extends BackgroundTaskFormController {
    @FXML
    private TextField upperPathField;

    @FXML
    private Button upperPreview;

    @FXML
    private Button upperChooseButton;

    @FXML
    private TextField lowerPathField;

    @FXML
    private Button lowerPreview;

    @FXML
    private Button lowerChooseButton;

    @FXML
    private TextField mergedPathField;

    @FXML
    private Button mergedPreview;

    @FXML
    private Button mergedChooseButton;

    @FXML
    private Button mergeButton;

    @FXML
    private ActionResultPane totalResultPane;

    private Stage stage;

    private Settings settings;

    private InputSubtitleInfo upperSubtitleInfo;

    private InputSubtitleInfo lowerSubtitleInfo;

    /*
     * Unlike with input subtitles, information regarding merged subtitles should be split between two variables
     * because subtitles can be merged without selecting a file to write the result to (through the preview) and vice
     * verse - selecting a file doesn't affect the merged subtitles themselves.
     */
    private MergedSubtitleFileInfo mergedSubtitleFileInfo;

    private Subtitles mergedSubtitles;

    /*
     * We need this special flag because otherwise the popup window will be shown twice if we change the value and press
     * enter. Because pressing the enter button will fire the event but after the popup window is opened another event
     * (losing the text field's focus) is fired.
     */
    private boolean overwritePopupDisplayed;

    public void initialize(Stage stage, GuiContext context) {
        this.stage = stage;
        settings = context.getSettings();

        GuiUtils.setTextEnteredHandler(
                upperPathField,
                (path) -> processInputSubtitlePath(path, InputSubtitleType.UPPER, FileOrigin.TEXT_FIELD)
        );
        GuiUtils.setTextEnteredHandler(
                lowerPathField,
                (path) -> processInputSubtitlePath(path, InputSubtitleType.LOWER, FileOrigin.TEXT_FIELD)
        );
        GuiUtils.setTextEnteredHandler(
                mergedPathField,
                (path) -> processMergedSubtitlePath(path, FileOrigin.TEXT_FIELD)
        );
    }

    private void processInputSubtitlePath(String path, InputSubtitleType subtitleType, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && pathNotChanged(path, subtitleType.getBroaderType())) {
            return;
        }

        BackgroundRunner<InputSubtitleInfo> backgroundRunner = backgroundManager -> {
            backgroundManager.setCancelPossible(false);
            backgroundManager.setIndeterminateProgress();
            backgroundManager.updateMessage("Processing the file " + path + "...");
            return getInputSubtitleInfo(path, subtitleType, fileOrigin);
        };

        BackgroundCallback<InputSubtitleInfo> callback = subtitleInfo -> {
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
            currentPath = upperSubtitleInfo != null ? upperSubtitleInfo.getPath() : "";
        } else if (subtitleType == SubtitleType.LOWER) {
            currentPath = lowerSubtitleInfo != null ? lowerSubtitleInfo.getPath() : "";
        } else if (subtitleType == SubtitleType.MERGED) {
            currentPath = mergedSubtitleFileInfo != null ? mergedSubtitleFileInfo.getPath() : "";
        } else {
            log.error("unexpected subtitle type " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }

        return Objects.equals(path, currentPath);
    }

    @Nullable
    private InputSubtitleInfo getInputSubtitleInfo(String path, InputSubtitleType subtitleType, FileOrigin fileOrigin) {
        InputFileValidationOptions validationOptions = InputFileValidationOptions.builder()
                .allowedExtensions( Collections.singletonList("srt"))
                .allowEmpty(false)
                .maxAllowedSize(LogicConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024L)
                .loadContent(true)
                .build();
        InputFileInfo fileInfo = FileValidator.getInputFileInfo(path, validationOptions);
        if (fileInfo.getNotValidReason() == InputFileNotValidReason.PATH_IS_EMPTY) {
            return null;
        }

        if (fileInfo.getNotValidReason() != null) {
            return new InputSubtitleInfo(
                    path,
                    fileInfo.getFile(),
                    fileOrigin,
                    fileInfo.getNotValidReason(),
                    isDuplicate(fileInfo.getFile(), subtitleType),
                    null
            );
        }

        return new InputSubtitleInfo(
                path,
                fileInfo.getFile(),
                fileOrigin,
                null,
                isDuplicate(fileInfo.getFile(), subtitleType),
                SubtitlesAndInput.from(fileInfo.getContent(), StandardCharsets.UTF_8)
        );
    }

    private boolean isDuplicate(File file, InputSubtitleType subtitleType) {
        InputSubtitleInfo otherSubtitleInfo;
        if (subtitleType == InputSubtitleType.UPPER) {
            if (lowerSubtitleInfo == null) {
                return false;
            }

            otherSubtitleInfo = lowerSubtitleInfo;
        } else if (subtitleType == InputSubtitleType.LOWER) {
            if (upperSubtitleInfo == null) {
                return false;
            }

            otherSubtitleInfo = upperSubtitleInfo;
        } else {
            log.error("unexpected subtitle type " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }

        return Objects.equals(file, otherSubtitleInfo.getFile());
    }

    private void updateSubtitleInfoVariable(InputSubtitleInfo subtitleInfo, InputSubtitleType subtitleType) {
        if (subtitleType == InputSubtitleType.UPPER) {
            upperSubtitleInfo = subtitleInfo;
        } else if (subtitleType == InputSubtitleType.LOWER) {
            lowerSubtitleInfo = subtitleInfo;
        } else {
            log.error("unexpected subtitle type " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    /*
     * Only one of the two input subtitles should ever be marked as duplicate, so after we process the subtitles we
     * should always mark the other subtitles as non-duplicate.
     */
    private void markOtherSubtitlesNotDuplicate(InputSubtitleType subtitleType) {
        if (subtitleType == InputSubtitleType.UPPER) {
            if (lowerSubtitleInfo != null) {
                lowerSubtitleInfo.setDuplicate(false);
            }
        } else if (subtitleType == InputSubtitleType.LOWER) {
            if (upperSubtitleInfo != null) {
                upperSubtitleInfo.setDuplicate(false);
            }
        } else {
            log.error("unexpected subtitle type " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private static void saveDirectoryInSettings(File file, SubtitleType subtitleType, Settings settings) {
        switch (subtitleType) {
            case UPPER:
                settings.saveQuietly(file.getParentFile(), SettingType.UPPER_DIRECTORY);
                return;
            case LOWER:
                settings.saveQuietly(file.getParentFile(), SettingType.LOWER_DIRECTORY);
                return;
            case MERGED:
                settings.saveQuietly(file.getParentFile(), SettingType.MERGED_DIRECTORY);
                return;
            default:
                log.error("unexpected subtitle type " + subtitleType + ", most likely a bug");
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

        totalResultPane.clear();
    }

    private void updatePathFields() {
        upperPathField.setText(upperSubtitleInfo != null ? upperSubtitleInfo.getFile().getAbsolutePath() : null);
        lowerPathField.setText(lowerSubtitleInfo != null ? lowerSubtitleInfo.getFile().getAbsolutePath() : null);
        mergedPathField.setText(
                mergedSubtitleFileInfo != null ? mergedSubtitleFileInfo.getFile().getAbsolutePath() : null
        );
    }

    private void setPreviewButtonsVisibility() {
        upperPreview.setDisable(inputPreviewNotAvailable(upperSubtitleInfo));
        lowerPreview.setDisable(inputPreviewNotAvailable(lowerSubtitleInfo));
        mergedPreview.setDisable(cantBeMerged(upperSubtitleInfo) || cantBeMerged(lowerSubtitleInfo));
    }

    /*
     * Note that the preview is available if the format is incorrect because it should be possible to open the preview
     * and change the encoding if that's what is causing the problem.
     */
    private static boolean inputPreviewNotAvailable(InputSubtitleInfo subtitleInfo) {
        return subtitleInfo == null || subtitleInfo.isDuplicate() || subtitleInfo.getNotValidReason() != null;
    }

    /*
     * Note that unlike with previews merging should not be available if the format is incorrect.
     */
    private static boolean cantBeMerged(InputSubtitleInfo subtitleInfo) {
        return subtitleInfo == null || subtitleInfo.isDuplicate() || subtitleInfo.getNotValidReason() != null
                || !subtitleInfo.isCorrectFormat();
    }

    private void setMergeButtonVisibility() {
        boolean disable = cantBeMerged(upperSubtitleInfo) || cantBeMerged(lowerSubtitleInfo)
                || mergedSubtitleFileInfo == null || mergedSubtitleFileInfo.getNotValidReason() != null;
        mergeButton.setDisable(disable);
    }

    private void showErrors() {
        List<String> errorParts = new ArrayList<>();

        String upperSubtitleError = getInputSubtitleError(upperSubtitleInfo);
        if (!StringUtils.isBlank(upperSubtitleError)) {
            displayFileElementsAsIncorrect(SubtitleType.UPPER);
            errorParts.add(upperSubtitleError);
        }

        String lowerSubtitleError = getInputSubtitleError(lowerSubtitleInfo);
        if (!StringUtils.isBlank(lowerSubtitleError)) {
            displayFileElementsAsIncorrect(SubtitleType.LOWER);
            errorParts.add(lowerSubtitleError);
        }

        String mergedSubtitleError = getMergedSubtitleError(mergedSubtitleFileInfo);
        if (!StringUtils.isBlank(mergedSubtitleError)) {
            displayFileElementsAsIncorrect(SubtitleType.MERGED);
            errorParts.add(mergedSubtitleError);
        }

        if (CollectionUtils.isEmpty(errorParts)) {
            return;
        }

        for (int i = 0; i < errorParts.size(); i++) {
            errorParts.set(i, "\u2022 " + errorParts.get(i));
        }

        totalResultPane.setOnlyError(StringUtils.join(errorParts, System.lineSeparator()));
    }

    @Nullable
    private static String getInputSubtitleError(InputSubtitleInfo subtitleInfo) {
        if (subtitleInfo == null) {
            return null;
        }

        if (subtitleInfo.isDuplicate()) {
            return "You have already selected this file for the other subtitles";
        }

        if (!subtitleInfo.isCorrectFormat()) {
            return "Subtitles in '" + subtitleInfo.getPath() + "'can't be parsed, it can happen if the file is not "
                    + "UTF-8-encoded, you can change the encoding after pressing the preview button";
        }

        if (subtitleInfo.getNotValidReason() != null) {
            String shortenedPath = getShortenedPath(subtitleInfo.getPath());

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

        return null;
    }

    private static String getShortenedPath(String path) {
        return Utils.getShortenedString(path, 20, 40);
    }

    @Nullable
    private static String getMergedSubtitleError(MergedSubtitleFileInfo subtitleFileInfo) {
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

            if (upperSubtitleInfo.getFileOrigin() == FileOrigin.FILE_CHOOSER) {
                upperChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
                upperChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        } else if (subtitleType == SubtitleType.LOWER) {
            lowerPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            lowerPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);

            if (lowerSubtitleInfo.getFileOrigin() == FileOrigin.FILE_CHOOSER) {
                lowerChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
                lowerChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        } else if (subtitleType == SubtitleType.MERGED) {
            mergedPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            mergedPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);

            if (mergedSubtitleFileInfo.getFileOrigin() == FileOrigin.FILE_CHOOSER) {
                mergedChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
                mergedChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        } else {
            log.error("unexpected subtitle type " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }
    }

    private void processMergedSubtitlePath(String path, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && pathNotChanged(path, SubtitleType.MERGED)) {
            return;
        }

        MergedSubtitleFileInfo subtitleFileInfo = getMergedSubtitleFileInfo(path, fileOrigin);
        if (fileOrigin == FileOrigin.TEXT_FIELD && fileExists(subtitleFileInfo)) {
            if (overwritePopupDisplayed) {
                return;
            }

            if (!agreeToOverwrite(subtitleFileInfo)) {
                mergedPathField.setText(subtitleFileInfo.getPath());
                return;
            }
        }

        mergedSubtitleFileInfo = subtitleFileInfo;
        if (fileOrigin == FileOrigin.FILE_CHOOSER && subtitleFileInfo != null) {
            saveDirectoryInSettings(subtitleFileInfo.getFile(), SubtitleType.MERGED, settings);
        }

        updateScene(fileOrigin);
    }

    @Nullable
    private static MergedSubtitleFileInfo getMergedSubtitleFileInfo(String path, FileOrigin fileOrigin) {
        OutputFileValidationOptions validationOptions = new OutputFileValidationOptions(
                Collections.singletonList("srt"),
                true
        );
        OutputFileInfo validatorFileInfo = FileValidator.getOutputFileInfo(path, validationOptions);
        if (validatorFileInfo.getNotValidReason() == OutputFileNotValidReason.PATH_IS_EMPTY) {
            return null;
        }

        return new MergedSubtitleFileInfo(
                path,
                validatorFileInfo.getFile(),
                fileOrigin,
                validatorFileInfo.getNotValidReason()
        );
    }

    private static boolean fileExists(MergedSubtitleFileInfo subtitleFileInfo) {
        return subtitleFileInfo != null && subtitleFileInfo.getFile().exists();
    }

    private boolean agreeToOverwrite(MergedSubtitleFileInfo subtitleFileInfo) {
        overwritePopupDisplayed = true;

        String fileName = Utils.getShortenedString(
                subtitleFileInfo.getFile().getName(),
                0,
                32
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
        Charset previousEncoding = upperSubtitleInfo.getEncoding();
        SubtitlesAndInput previewSelection = Popups.showEncodingPreview(
                upperSubtitleInfo.getPath(),
                upperSubtitleInfo.getSubtitlesAndInput(),
                stage
        );

        upperSubtitleInfo.setSubtitlesAndInput(previewSelection);
        if (!Objects.equals(previousEncoding, previewSelection.getEncoding())) {
            mergedSubtitles = null;
        }
        updateScene(upperSubtitleInfo.getFileOrigin());
    }

    @FXML
    private void upperChooserClicked() {
        clearState();

        File file = getInputFile(InputSubtitleType.UPPER, stage, settings);
        if (file == null) {
            clearState();
            return;
        }

        processInputSubtitlePath(file.getAbsolutePath(), InputSubtitleType.UPPER, FileOrigin.FILE_CHOOSER);
    }

    private static File getInputFile(InputSubtitleType subtitleType, Stage stage, Settings settings) {
        String chooserTitle;
        File chooserInitialDirectory;
        if (subtitleType == InputSubtitleType.UPPER) {
            chooserTitle = "Please choose a file with the upper subtitles";
            chooserInitialDirectory = ObjectUtils.firstNonNull(
                    settings.getUpperDirectory(),
                    settings.getLowerDirectory(),
                    settings.getMergedDirectory()
            );
        } else if (subtitleType == InputSubtitleType.LOWER) {
            chooserTitle = "Please choose a file with the lower subtitles";
            chooserInitialDirectory = ObjectUtils.firstNonNull(
                    settings.getLowerDirectory(),
                    settings.getUpperDirectory(),
                    settings.getMergedDirectory()
            );
        } else {
            log.error("unexpected subtitle type " + subtitleType + ", most likely a bug");
            throw new IllegalStateException();
        }

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle(chooserTitle);
        fileChooser.setInitialDirectory(chooserInitialDirectory);
        fileChooser.getExtensionFilters().add(GuiConstants.SUB_RIP_EXTENSION_FILTER);

        return fileChooser.showOpenDialog(stage);
    }

    @FXML
    private void lowerPreviewClicked() {
        Charset previousEncoding = lowerSubtitleInfo.getEncoding();
        clearState();

        SubtitlesAndInput previewSelection = Popups.showEncodingPreview(
                lowerSubtitleInfo.getPath(),
                lowerSubtitleInfo.getSubtitlesAndInput(),
                stage
        );

        lowerSubtitleInfo.setSubtitlesAndInput(previewSelection);
        if (!Objects.equals(previousEncoding, previewSelection.getEncoding())) {
            mergedSubtitles = null;
        }
        updateScene(lowerSubtitleInfo.getFileOrigin());
    }

    @FXML
    private void lowerChooserClicked() {
        File file = getInputFile(InputSubtitleType.LOWER, stage, settings);
        if (file == null) {
            clearState();
            return;
        }

        processInputSubtitlePath(file.getAbsolutePath(), InputSubtitleType.LOWER, FileOrigin.FILE_CHOOSER);
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
                                upperSubtitleInfo.getSubtitles(),
                                lowerSubtitleInfo.getSubtitles()
                        ),
                        settings.isPlainTextSubtitles()
                );
            } catch (InterruptedException e) {
                return null;
            }
        };

        BackgroundCallback<SubtitlesAndOutput> callback = subtitlesAndOutput -> {
            if (subtitlesAndOutput == null) {
                totalResultPane.setOnlyWarn("The merge has been cancelled");
                return;
            }

            mergedSubtitles = subtitlesAndOutput.getSubtitles();

            Popups.showMergedSubtitlesPreview(
                    upperSubtitleInfo.getPath(),
                    lowerSubtitleInfo.getPath(),
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

        processMergedSubtitlePath(file.getAbsolutePath(), FileOrigin.FILE_CHOOSER);
    }

    @Nullable
    private static File getMergedFile(Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose where to save the result");
        fileChooser.setInitialDirectory(
                ObjectUtils.firstNonNull(
                        settings.getMergedDirectory(),
                        settings.getUpperDirectory(),
                        settings.getLowerDirectory()
                )
        );
        fileChooser.getExtensionFilters().add(GuiConstants.SUB_RIP_EXTENSION_FILTER);

        return fileChooser.showSaveDialog(stage);
    }

    @FXML
    private void mergeButtonClicked() {
        clearState();

        BackgroundRunner<ActionResult> backgroundRunner = backgroundManager -> {
            if (mergedSubtitles == null) {
                backgroundManager.setCancelPossible(true);
                backgroundManager.setIndeterminateProgress();
                backgroundManager.updateMessage("Merging the subtitles...");
                try {
                    mergedSubtitles = SubtitleMerger.mergeSubtitles(
                            upperSubtitleInfo.getSubtitles(),
                            lowerSubtitleInfo.getSubtitles()
                    );
                } catch (InterruptedException e) {
                    return ActionResult.onlyWarn("The merge has been cancelled");
                }
            }

            try {
                backgroundManager.setCancelPossible(false);
                backgroundManager.setIndeterminateProgress();
                backgroundManager.updateMessage("Writing the result...");

                FileUtils.writeStringToFile(
                        mergedSubtitleFileInfo.getFile(),
                        SubRipWriter.toText(mergedSubtitles, settings.isPlainTextSubtitles()),
                        StandardCharsets.UTF_8
                );

                return ActionResult.onlySuccess("The subtitles have been merged successfully!");
            } catch (IOException e) {
                return ActionResult.onlyError(
                        "Can't write the merged subtitles to the file, please check access to the file"
                );
            }
        };

        BackgroundCallback<ActionResult> callback = actionResult -> {
            if (actionResult.haveErrors()) {
                displayFileElementsAsIncorrect(SubtitleType.MERGED);
            }
            totalResultPane.setActionResult(actionResult);
        };

        runInBackground(backgroundRunner, callback);
    }

    @AllArgsConstructor
    @Getter
    private enum InputSubtitleType {
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
    private static class InputSubtitleInfo {
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

        boolean isCorrectFormat() {
            return subtitlesAndInput != null && subtitlesAndInput.isCorrectFormat();
        }
    }

    @AllArgsConstructor
    @Getter
    private static class MergedSubtitleFileInfo {
        private String path;

        private File file;

        private FileOrigin fileOrigin;

        private OutputFileNotValidReason notValidReason;
    }
}