package kirill.subtitlemerger.gui.tabs.subtitle_files;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.utils.entities.AbstractController;
import kirill.subtitlemerger.gui.utils.forms_and_controls.SubtitlePreviewController;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerCallback;
import kirill.subtitlemerger.gui.utils.forms_and_controls.ActionResultLabels;
import kirill.subtitlemerger.gui.utils.entities.ActionResult;
import kirill.subtitlemerger.gui.utils.entities.FileOrigin;
import kirill.subtitlemerger.gui.utils.entities.NodeInfo;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.core.SubRipParser;
import kirill.subtitlemerger.logic.core.SubRipWriter;
import kirill.subtitlemerger.logic.core.SubtitleMerger;
import kirill.subtitlemerger.logic.core.entities.SubtitleFormatException;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.settings.SettingException;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.file_validation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@CommonsLog
public class SubtitleFilesTabController extends AbstractController {
    private Stage stage;

    private Settings settings;

    @FXML
    private Button upperChooseButton;

    @FXML
    private TextField upperPathField;

    @FXML
    private Button upperPreview;

    @FXML
    private Button lowerChooseButton;

    @FXML
    private TextField lowerPathField;

    @FXML
    private Button lowerPreview;

    @FXML
    private Button mergedChooseButton;

    @FXML
    private TextField mergedPathField;

    @FXML
    private Button mergedPreview;

    @FXML
    private Button mergeButton;

    @FXML
    private ActionResultLabels actionResultLabels;

    private FilesInfo filesInfo;

    /*
     * We need this special flag because otherwise the dialog window will be shown twice if we change the value and
     * press enter. Because pressing the enter button will fire the event but after the dialog windows is opened another
     * event (losing text field's focus) is fired.
     */
    private boolean agreeToOverwriteInProgress;

    public void initialize(Stage stage, GuiContext context) {
        this.stage = stage;
        this.settings = context.getSettings();
        this.filesInfo = new FilesInfo(null, null, null, null);

        GuiUtils.setTextFieldChangeListeners(
                upperPathField,
                (path) -> processInputFilePath(path, InputFileType.UPPER_SUBTITLES, FileOrigin.TEXT_FIELD)
        );
        GuiUtils.setTextFieldChangeListeners(
                lowerPathField,
                (path) -> processInputFilePath(path, InputFileType.LOWER_SUBTITLES, FileOrigin.TEXT_FIELD)
        );
        GuiUtils.setTextFieldChangeListeners(
                mergedPathField,
                (path) -> processMergedFilePath(path, FileOrigin.TEXT_FIELD)
        );
    }

    private void processInputFilePath(String path, InputFileType fileType, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && pathNotChanged(path, fileType.getExtendedFileType(), filesInfo)) {
            return;
        }

        BackgroundRunner<InputFileInfo> backgroundRunner = runnerManager -> {
            runnerManager.updateMessage("Processing file " + path + "...");
            return getInputFileInfo(path, fileType, fileOrigin, filesInfo).orElse(null);
        };

        BackgroundRunnerCallback<InputFileInfo> callback = inputFileInfo -> {
            updateFilesInfo(inputFileInfo, fileType, filesInfo);
            markOtherFileNotDuplicate(fileType, filesInfo);
            if (fileOrigin == FileOrigin.FILE_CHOOSER && inputFileInfo != null) {
                @SuppressWarnings("SimplifyOptionalCallChains")
                File parent = Utils.getParentDirectory(inputFileInfo.getFile()).orElse(null);
                if (parent != null) {
                    saveDirectoryInConfig(fileType.getExtendedFileType(), parent, settings);
                }
            }
            filesInfo.setMergedSubtitles(null);

            updateScene(fileOrigin);
        };

        runInBackground(backgroundRunner, callback);
    }

    private static boolean pathNotChanged(String path, ExtendedFileType fileType, FilesInfo filesInfo) {
        String currentPath;
        if (fileType == ExtendedFileType.UPPER_SUBTITLES) {
            currentPath = filesInfo.getUpperFileInfo() != null ? filesInfo.getUpperFileInfo().getPath() : "";
        } else if (fileType == ExtendedFileType.LOWER_SUBTITLES) {
            currentPath = filesInfo.getLowerFileInfo() != null ? filesInfo.getLowerFileInfo().getPath() : "";
        } else if (fileType == ExtendedFileType.MERGED_SUBTITLES) {
            currentPath = filesInfo.getMergedFileInfo() != null ? filesInfo.getMergedFileInfo().getPath() : "";
        } else {
            throw new IllegalStateException();
        }

        return Objects.equals(path, currentPath);
    }

    private static Optional<InputFileInfo> getInputFileInfo(
            String path,
            InputFileType fileType,
            FileOrigin fileOrigin,
            FilesInfo filesInfo
    ) {
        InputFileValidationOptions validationOptions = InputFileValidationOptions.builder()
                .allowedExtensions( Collections.singletonList("srt"))
                .allowEmpty(false)
                .maxAllowedSize(LogicConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024L)
                .loadContent(true)
                .build();
        kirill.subtitlemerger.logic.utils.file_validation.InputFileInfo validatorFileInfo = FileValidator.getInputFileInfo(
                path,
                validationOptions
        );
        if (validatorFileInfo.getNotValidReason() == InputFileNotValidReason.PATH_IS_EMPTY) {
            return Optional.empty();
        }

        if (validatorFileInfo.getNotValidReason() != null) {
            return Optional.of(
                    new InputFileInfo(
                            path,
                            validatorFileInfo.getFile(),
                            fileOrigin,
                            isDuplicate(path, validatorFileInfo.getFile(), fileType, filesInfo),
                            validatorFileInfo.getContent(),
                            null,
                            null,
                            SubtitleFilesTabController.InputFileInfo.from(validatorFileInfo.getNotValidReason())
                    )
            );
        }

        try {
            Subtitles subtitles = SubRipParser.from(new String(validatorFileInfo.getContent(), StandardCharsets.UTF_8));

            return Optional.of(
                    new InputFileInfo(
                            path,
                            validatorFileInfo.getFile(),
                            fileOrigin,
                            isDuplicate(path, validatorFileInfo.getFile(), fileType, filesInfo),
                            validatorFileInfo.getContent(),
                            StandardCharsets.UTF_8,
                            subtitles,
                            null
                    )
            );
        } catch (SubtitleFormatException e) {
            return Optional.of(
                    new InputFileInfo(
                            path,
                            validatorFileInfo.getFile(),
                            fileOrigin,
                            isDuplicate(path, validatorFileInfo.getFile(), fileType, filesInfo),
                            validatorFileInfo.getContent(),
                            StandardCharsets.UTF_8,
                            null,
                            SubtitleFilesTabController.IncorrectInputFileReason.INCORRECT_SUBTITLE_FORMAT
                    )
            );
        }
    }

    private static boolean isDuplicate(String path, File file, InputFileType fileType, FilesInfo filesInfo) {
        String otherPath;
        File otherFile;
        if (fileType == InputFileType.UPPER_SUBTITLES) {
            if (filesInfo.getLowerFileInfo() == null) {
                return false;
            }

            otherPath = filesInfo.getLowerFileInfo().getPath();
            otherFile = filesInfo.getLowerFileInfo().getFile();
        } else if (fileType == InputFileType.LOWER_SUBTITLES) {
            if (filesInfo.getUpperFileInfo() == null) {
                return false;
            }

            otherPath = filesInfo.getUpperFileInfo().getPath();
            otherFile = filesInfo.getUpperFileInfo().getFile();
        } else {
            throw new IllegalStateException();
        }

        if (otherFile != null) {
            return Objects.equals(file, otherFile);
        } else {
            return Objects.equals(path, otherPath);
        }
    }

    private static void updateFilesInfo(InputFileInfo inputFileInfo, InputFileType fileType, FilesInfo filesInfo) {
        if (fileType == InputFileType.UPPER_SUBTITLES) {
            filesInfo.setUpperFileInfo(inputFileInfo);
        } else if (fileType == InputFileType.LOWER_SUBTITLES) {
            filesInfo.setLowerFileInfo(inputFileInfo);
        } else {
            throw new IllegalStateException();
        }
    }

    /*
     * Only one of two input files should ever be marked as duplicate, so after we process the file we should always
     * mark the other file as not-duplicate.
     */
    private static void markOtherFileNotDuplicate(InputFileType fileType, FilesInfo filesInfo) {
        if (fileType == InputFileType.UPPER_SUBTITLES) {
            if (filesInfo.getLowerFileInfo() != null) {
                filesInfo.getLowerFileInfo().setDuplicate(false);
            }
        } else if (fileType == InputFileType.LOWER_SUBTITLES) {
            if (filesInfo.getUpperFileInfo() != null) {
                filesInfo.getUpperFileInfo().setDuplicate(false);
            }
        } else {
            throw new IllegalStateException();
        }
    }

    private static void saveDirectoryInConfig(ExtendedFileType fileType, File parent, Settings settings) {
        try {
            switch (fileType) {
                case UPPER_SUBTITLES:
                    settings.saveUpperDirectory(parent.getAbsolutePath());
                    return;
                case LOWER_SUBTITLES:
                    settings.saveLowerDirectory(parent.getAbsolutePath());
                    return;
                case MERGED_SUBTITLES:
                    settings.saveMergedDirectory(parent.getAbsolutePath());
                    return;
                default:
                    throw new IllegalStateException();
            }
        } catch (SettingException e) {
            log.warn(
                    "failed to save last directory " + parent.getAbsolutePath() + ": "
                            + ExceptionUtils.getStackTrace(e)
            );
        }
    }

    private void updateScene(FileOrigin fileOrigin) {
        clearState();

        if (fileOrigin != FileOrigin.TEXT_FIELD) {
            updatePathFields();
        }

        setPreviewButtonsVisibility();
        setMergeButtonVisibility();
        showErrorsIfNecessary();
    }

    private void clearState() {
        upperPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        upperChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        lowerPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        lowerChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        mergedPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        mergedChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);

        actionResultLabels.clear();
    }

    private void updatePathFields() {
        upperPathField.setText(
                filesInfo.getUpperFileInfo() != null && filesInfo.getUpperFileInfo().getFile() != null
                        ? filesInfo.getUpperFileInfo().getFile().getAbsolutePath()
                        : null
        );
        lowerPathField.setText(
                filesInfo.getLowerFileInfo() != null && filesInfo.getLowerFileInfo().getFile() != null
                        ? filesInfo.getLowerFileInfo().getFile().getAbsolutePath()
                        : null
        );
        mergedPathField.setText(
                filesInfo.getMergedFileInfo() != null && filesInfo.getMergedFileInfo().getFile() != null
                        ? filesInfo.getMergedFileInfo().getFile().getAbsolutePath()
                        : null
        );
    }

    private void setPreviewButtonsVisibility() {
        upperPreview.setDisable(makePreviewDisabled(filesInfo.getUpperFileInfo()));
        lowerPreview.setDisable(makePreviewDisabled(filesInfo.getLowerFileInfo()));
        mergedPreview.setDisable(!filesInfo.upperFileOk() || !filesInfo.lowerFileOk());
    }

    private static boolean makePreviewDisabled(InputFileInfo fileInfo) {
        if (fileInfo == null || fileInfo.isDuplicate()) {
            return true;
        }

        return fileInfo.getIncorrectFileReason() != null
                && fileInfo.getIncorrectFileReason() != IncorrectInputFileReason.INCORRECT_SUBTITLE_FORMAT;
    }

    private void setMergeButtonVisibility() {
        boolean disable = !filesInfo.upperFileOk()
                || !filesInfo.lowerFileOk()
                || filesInfo.getMergedFileInfo() == null
                || filesInfo.getMergedFileInfo().getIncorrectFileReason() != null;
        mergeButton.setDisable(disable);
    }

    private void showErrorsIfNecessary() {
        List<String> errorMessageParts = new ArrayList<>();

        InputFileInfo upperFileInfo = filesInfo.getUpperFileInfo();
        if (upperFileInfo != null && (upperFileInfo.isDuplicate() || upperFileInfo.getIncorrectFileReason() != null)) {
            showFileElementsAsIncorrect(ExtendedFileType.UPPER_SUBTITLES);

            if (upperFileInfo.getFile() != null && upperFileInfo.isDuplicate()) {
                errorMessageParts.add("You have already selected this file for the lower subtitles");
            } else if (upperFileInfo.getIncorrectFileReason() != null) {
                errorMessageParts.add(getErrorText(upperFileInfo.getPath(), upperFileInfo.getIncorrectFileReason()));
            }
        }

        InputFileInfo lowerFileInfo = filesInfo.getLowerFileInfo();
        if (lowerFileInfo != null && (lowerFileInfo.isDuplicate() || lowerFileInfo.getIncorrectFileReason() != null)) {
            showFileElementsAsIncorrect(ExtendedFileType.LOWER_SUBTITLES);

            if (lowerFileInfo.getFile() != null && lowerFileInfo.isDuplicate()) {
                errorMessageParts.add("You have already selected this file for the upper subtitles");
            } else if (lowerFileInfo.getIncorrectFileReason() != null) {
                errorMessageParts.add(getErrorText(lowerFileInfo.getPath(), lowerFileInfo.getIncorrectFileReason()));
            }
        }

        MergedFileInfo mergedFileInfo = filesInfo.getMergedFileInfo();
        if (mergedFileInfo != null && mergedFileInfo.getIncorrectFileReason() != null) {
            showFileElementsAsIncorrect(ExtendedFileType.MERGED_SUBTITLES);

            if (mergedFileInfo.getIncorrectFileReason() != null) {
                errorMessageParts.add(getErrorText(mergedFileInfo.getPath(), mergedFileInfo.getIncorrectFileReason()));
            }
        }

        if (CollectionUtils.isEmpty(errorMessageParts)) {
            return;
        }

        StringBuilder errorMessage = new StringBuilder();
        int index = 0;
        for (String part : errorMessageParts) {
            if (index != 0) {
                errorMessage.append(System.lineSeparator());
            }

            errorMessage.append("\u2022").append(" ").append(part);

            index++;
        }

        actionResultLabels.setOnlyError(errorMessage.toString());
    }

    private void showFileElementsAsIncorrect(ExtendedFileType fileType) {
        if (fileType == ExtendedFileType.UPPER_SUBTITLES) {
            upperPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            upperPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);

            if (filesInfo.getUpperFileInfo().getFileOrigin() == FileOrigin.FILE_CHOOSER) {
                upperChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
                upperChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        } else if (fileType == ExtendedFileType.LOWER_SUBTITLES) {
            lowerPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            lowerPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);

            if (filesInfo.getLowerFileInfo().getFileOrigin() == FileOrigin.FILE_CHOOSER) {
                lowerChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
                lowerChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        } else if (fileType == ExtendedFileType.MERGED_SUBTITLES) {
            mergedPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            mergedPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);

            if (filesInfo.getMergedFileInfo().getFileOrigin() == FileOrigin.FILE_CHOOSER) {
                mergedChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
                mergedChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        } else {
            throw new IllegalStateException();
        }
    }

    private static String getErrorText(String path, IncorrectInputFileReason reason) {
        path = getShortenedPath(path);

        switch (reason) {
            case PATH_IS_TOO_LONG:
                return "File path is too long";
            case INVALID_PATH:
                return "File path is invalid";
            case IS_A_DIRECTORY:
                return path + " is a directory, not a file";
            case DOES_NOT_EXIST:
                return "File '" + path + "' doesn't exist";
            case NO_EXTENSION:
                return "File '" + path + "' has no extension";
            case NOT_ALLOWED_EXTENSION:
                return "File '" + path + "' has an incorrect extension";
            case FILE_IS_EMPTY:
                return "File '" + path + "' is empty";
            case FILE_IS_TOO_BIG:
                return "File '" + path + "' is too big (>"
                        + LogicConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES + " megabytes)";
            case FAILED_TO_READ_CONTENT:
                return path + ": failed to read the file";
            case INCORRECT_SUBTITLE_FORMAT:
                return "File '" + path + "' has an incorrect subtitle format, it can happen if the file is not UTF-8-encoded"
                        + ", you can change the encoding pressing the preview button";
            default:
                throw new IllegalStateException();
        }
    }

    private static String getShortenedPath(String path) {
        return GuiUtils.getShortenedStringIfNecessary(path, 20, 40);
    }

    private static String getErrorText(String path, IncorrectMergedFileReason reason) {
        path = getShortenedPath(path);

        switch (reason) {
            case PATH_IS_TOO_LONG:
                return "File path is too long";
            case INVALID_PATH:
                return "File path is invalid";
            case IS_A_DIRECTORY:
                return path + " is a directory, not a file";
            case NO_EXTENSION:
                return "File '" + path + "' has no extension";
            case NOT_ALLOWED_EXTENSION:
                return "File '" + path + "' has an incorrect extension";
            default:
                throw new IllegalStateException();
        }
    }

    private void processMergedFilePath(String path, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && pathNotChanged(path, ExtendedFileType.MERGED_SUBTITLES, filesInfo)) {
            return;
        }

        MergedFileInfo mergedFileInfo = getMergedFileInfo(path, fileOrigin).orElse(null);
        if (fileOrigin == FileOrigin.TEXT_FIELD && fileExists(mergedFileInfo)) {
            if (agreeToOverwriteInProgress) {
                return;
            }

            if (!agreeToOverwrite(mergedFileInfo)) {
                mergedPathField.setText(
                        filesInfo.getMergedFileInfo() != null ? filesInfo.getMergedFileInfo().getPath() : ""
                );
                return;
            }
        }

        filesInfo.setMergedFileInfo(mergedFileInfo);
        if (fileOrigin == FileOrigin.FILE_CHOOSER && mergedFileInfo != null) {
            @SuppressWarnings("SimplifyOptionalCallChains")
            File parent = Utils.getParentDirectory(mergedFileInfo.getFile()).orElse(null);
            if (parent != null) {
                saveDirectoryInConfig(ExtendedFileType.MERGED_SUBTITLES, parent, settings);
            }
        }

        updateScene(fileOrigin);
    }

    private static Optional<MergedFileInfo> getMergedFileInfo(String path, FileOrigin fileOrigin) {
        OutputFileValidationOptions validationOptions = new OutputFileValidationOptions(
                Collections.singletonList("srt"),
                true
        );
        OutputFileInfo validatorFileInfo = FileValidator.getOutputFileInfo(path, validationOptions);
        if (validatorFileInfo.getNotValidReason() == OutputFileNotValidReason.PATH_IS_EMPTY) {
            return Optional.empty();
        }

        return Optional.of(
                new MergedFileInfo(
                        path,
                        validatorFileInfo.getFile(),
                        fileOrigin,
                        validatorFileInfo.getNotValidReason() != null
                                ? MergedFileInfo.from(validatorFileInfo.getNotValidReason())
                                : null
                )
        );
    }

    private static boolean fileExists(MergedFileInfo fileInfo) {
        return fileInfo != null && fileInfo.getFile() != null && fileInfo.getFile().exists();
    }

    private boolean agreeToOverwrite(MergedFileInfo mergedFileInfo) {
        agreeToOverwriteInProgress = true;

        String fileName = GuiUtils.getShortenedStringIfNecessary(
                mergedFileInfo.getFile().getName(),
                0,
                32
        );

        boolean result = GuiUtils.showAgreementPopup(
                "File '" + fileName + "' already exists. Do you want to overwrite it?",
                "Yes",
                "No",
                stage
        );

        agreeToOverwriteInProgress = false;

        return result;
    }

    @FXML
    private void upperPreviewClicked() {
        SubtitlePreviewController.UserSelection userSelection = showInputSubtitlePreview(filesInfo.getUpperFileInfo());
        updateSubtitlesAndEncodingIfChanged(filesInfo.getUpperFileInfo(), userSelection);
    }

    private SubtitlePreviewController.UserSelection showInputSubtitlePreview(InputFileInfo fileInfo) {
        NodeInfo nodeInfo = GuiUtils.loadNode("/gui/javafx/forms_and_controls/subtitlePreview.fxml");

        Stage previewStage = GuiUtils.createPopupStage("Subtitle preview", nodeInfo.getNode(), stage);

        SubtitlePreviewController controller = nodeInfo.getController();
        controller.initializeWithEncoding(
                fileInfo.getRawData(),
                fileInfo.getEncoding(),
                fileInfo.getPath(),
                previewStage
        );

        previewStage.showAndWait();

        return controller.getUserSelection();
    }

    private void updateSubtitlesAndEncodingIfChanged(
            InputFileInfo fileInfo,
            SubtitlePreviewController.UserSelection userSelection
    ) {
        if (Objects.equals(fileInfo.getEncoding(), userSelection.getEncoding())) {
            return;
        }

        fileInfo.setEncoding(userSelection.getEncoding());
        fileInfo.setSubtitles(userSelection.getSubtitles());
        if (userSelection.getSubtitles() == null) {
            fileInfo.setIncorrectFileReason(IncorrectInputFileReason.INCORRECT_SUBTITLE_FORMAT);
        } else {
            fileInfo.setIncorrectFileReason(null);
        }
        filesInfo.setMergedSubtitles(null);

        updateScene(fileInfo.getFileOrigin());
    }

    @FXML
    private void upperChooseClicked() {
        File file = getInputFile(InputFileType.UPPER_SUBTITLES, stage, settings).orElse(null);
        if (file == null) {
            clearState();
            return;
        }

        processInputFilePath(file.getAbsolutePath(), InputFileType.UPPER_SUBTITLES, FileOrigin.FILE_CHOOSER);
    }

    private static Optional<File> getInputFile(InputFileType fileType, Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        String chooserTitle;
        File chooserInitialDirectory;
        if (fileType == InputFileType.UPPER_SUBTITLES) {
            chooserTitle = "Please choose the file with the upper subtitles";
            chooserInitialDirectory = ObjectUtils.firstNonNull(
                    settings.getUpperDirectory(),
                    settings.getLowerDirectory(),
                    settings.getMergedDirectory()
            );
        } else if (fileType == InputFileType.LOWER_SUBTITLES) {
            chooserTitle = "Please choose the file with the lower subtitles";
            chooserInitialDirectory = ObjectUtils.firstNonNull(
                    settings.getLowerDirectory(),
                    settings.getUpperDirectory(),
                    settings.getMergedDirectory()
            );
        } else {
            throw new IllegalStateException();
        }

        fileChooser.setTitle(chooserTitle);
        fileChooser.setInitialDirectory(chooserInitialDirectory);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("subrip files (*.srt)", "*.srt")
        );

        return Optional.ofNullable(fileChooser.showOpenDialog(stage));
    }

    @FXML
    private void lowerPreviewClicked() {
        SubtitlePreviewController.UserSelection dialogResult = showInputSubtitlePreview(filesInfo.getLowerFileInfo());
        updateSubtitlesAndEncodingIfChanged(filesInfo.getLowerFileInfo(), dialogResult);
    }

    @FXML
    private void lowerChooseClicked() {
        File file = getInputFile(InputFileType.LOWER_SUBTITLES, stage, settings).orElse(null);
        if (file == null) {
            clearState();
            return;
        }

        processInputFilePath(file.getAbsolutePath(), InputFileType.LOWER_SUBTITLES, FileOrigin.FILE_CHOOSER);
    }

    @FXML
    private void mergedPreviewClicked() {
        clearState();

        BackgroundRunner<Optional<Subtitles>> backgroundRunner = runnerManager -> {
            if (filesInfo.getMergedSubtitles() != null) {
                return Optional.of(filesInfo.getMergedSubtitles());
            }

            runnerManager.setCancellationPossible(true);
            runnerManager.updateMessage("Merging subtitles...");

            try {
                return Optional.of(
                        SubtitleMerger.mergeSubtitles(
                                filesInfo.getUpperFileInfo().getSubtitles(),
                                filesInfo.getLowerFileInfo().getSubtitles()
                        )
                );
            } catch (InterruptedException e) {
                return Optional.empty();
            }
        };

        BackgroundRunnerCallback<Optional<Subtitles>> callback = result -> {
            Subtitles subtitles = result.orElse(null);
            if (subtitles == null) {
                actionResultLabels.setOnlyWarn("Merge has been cancelled");
                return;
            }

            filesInfo.setMergedSubtitles(subtitles);

            NodeInfo nodeInfo = GuiUtils.loadNode("/gui/javafx/forms_and_controls/subtitlePreview.fxml");

            Stage previewStage = GuiUtils.createPopupStage("Subtitle preview", nodeInfo.getNode(), stage);

            SubtitlePreviewController controller = nodeInfo.getController();
            controller.initializeMerged(
                    subtitles,
                    filesInfo.getUpperFileInfo().getPath(),
                    filesInfo.getLowerFileInfo().getPath(),
                    previewStage
            );

            previewStage.showAndWait();
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void mergedSubtitlesButtonClicked() {
        File file = getMergedFile(stage, settings).orElse(null);
        if (file == null) {
            return;
        }

        processMergedFilePath(file.getAbsolutePath(), FileOrigin.FILE_CHOOSER);
    }

    private static Optional<File> getMergedFile(Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose where to save the result");
        fileChooser.setInitialDirectory(
                ObjectUtils.firstNonNull(
                        settings.getMergedDirectory(),
                        settings.getUpperDirectory(),
                        settings.getLowerDirectory()
                )
        );
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("subrip files (*.srt)", "*.srt")
        );

        return Optional.ofNullable(fileChooser.showSaveDialog(stage));
    }

    @FXML
    private void mergeButtonClicked() {
        clearState();

        BackgroundRunner<ActionResult> backgroundRunner = runnerManager -> {
            Subtitles mergedSubtitles;

            if (filesInfo.getMergedSubtitles() != null) {
                mergedSubtitles = filesInfo.getMergedSubtitles();
            } else {
                runnerManager.setCancellationPossible(true);
                runnerManager.updateMessage("Merging subtitles...");

                try {
                    mergedSubtitles = SubtitleMerger.mergeSubtitles(
                            filesInfo.getUpperFileInfo().getSubtitles(),
                            filesInfo.getLowerFileInfo().getSubtitles()
                    );
                } catch (InterruptedException e) {
                    return ActionResult.onlyWarn("Merge has been cancelled");
                }
            }

            try {
                FileUtils.writeStringToFile(
                        filesInfo.getMergedFileInfo().getFile(),
                        SubRipWriter.toText(mergedSubtitles),
                        StandardCharsets.UTF_8
                );

                return ActionResult.onlySuccess("Subtitles have been merged successfully!");
            } catch (IOException e) {
                return ActionResult.onlyError(
                        "Can't merge subtitles:" + System.lineSeparator() + "\u2022 can't write to this file"
                );
            }
        };

        BackgroundRunnerCallback<ActionResult> callback = actionResult -> {
            if (!StringUtils.isBlank(actionResult.getError())) {
                showFileElementsAsIncorrect(ExtendedFileType.MERGED_SUBTITLES);
            }
            actionResultLabels.set(actionResult);
        };

        runInBackground(backgroundRunner, callback);
    }

    @AllArgsConstructor
    @Getter
    private enum InputFileType {
        UPPER_SUBTITLES(ExtendedFileType.UPPER_SUBTITLES),

        LOWER_SUBTITLES(ExtendedFileType.LOWER_SUBTITLES);

        private ExtendedFileType extendedFileType;
    }

    private enum ExtendedFileType {
        UPPER_SUBTITLES,
        LOWER_SUBTITLES,
        MERGED_SUBTITLES
    }

    @AllArgsConstructor
    @Getter
    private static class InputFileInfo {
        private String path;

        private File file;

        private FileOrigin fileOrigin;

        @Setter
        private boolean isDuplicate;

        private byte[] rawData;

        @Setter
        private Charset encoding;

        @Setter
        private Subtitles subtitles;

        @Setter
        private IncorrectInputFileReason incorrectFileReason;

        static IncorrectInputFileReason from(InputFileNotValidReason reason) {
            switch (reason) {
                case PATH_IS_TOO_LONG:
                    return SubtitleFilesTabController.IncorrectInputFileReason.PATH_IS_TOO_LONG;
                case INVALID_PATH:
                    return SubtitleFilesTabController.IncorrectInputFileReason.INVALID_PATH;
                case IS_A_DIRECTORY:
                    return SubtitleFilesTabController.IncorrectInputFileReason.IS_A_DIRECTORY;
                case DOES_NOT_EXIST:
                    return SubtitleFilesTabController.IncorrectInputFileReason.DOES_NOT_EXIST;
                case NO_EXTENSION:
                    return SubtitleFilesTabController.IncorrectInputFileReason.NO_EXTENSION;
                case NOT_ALLOWED_EXTENSION:
                    return SubtitleFilesTabController.IncorrectInputFileReason.NOT_ALLOWED_EXTENSION;
                case FILE_IS_EMPTY:
                    return SubtitleFilesTabController.IncorrectInputFileReason.FILE_IS_EMPTY;
                case FILE_IS_TOO_BIG:
                    return SubtitleFilesTabController.IncorrectInputFileReason.FILE_IS_TOO_BIG;
                case FAILED_TO_READ_CONTENT:
                    return SubtitleFilesTabController.IncorrectInputFileReason.FAILED_TO_READ_CONTENT;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private enum IncorrectInputFileReason {
        PATH_IS_TOO_LONG,
        INVALID_PATH,
        IS_A_DIRECTORY,
        DOES_NOT_EXIST,
        NO_EXTENSION,
        NOT_ALLOWED_EXTENSION,
        FILE_IS_EMPTY,
        FILE_IS_TOO_BIG,
        FAILED_TO_READ_CONTENT,
        INCORRECT_SUBTITLE_FORMAT
    }

    @AllArgsConstructor
    @Getter
    private static class MergedFileInfo {
        private String path;

        private File file;

        private FileOrigin fileOrigin;

        private IncorrectMergedFileReason incorrectFileReason;

        static IncorrectMergedFileReason from(OutputFileNotValidReason reason) {
            switch (reason) {
                case PATH_IS_TOO_LONG:
                    return IncorrectMergedFileReason.PATH_IS_TOO_LONG;
                case INVALID_PATH:
                    return IncorrectMergedFileReason.INVALID_PATH;
                case IS_A_DIRECTORY:
                    return IncorrectMergedFileReason.IS_A_DIRECTORY;
                case NO_EXTENSION:
                    return IncorrectMergedFileReason.NO_EXTENSION;
                case NOT_ALLOWED_EXTENSION:
                    return IncorrectMergedFileReason.NOT_ALLOWED_EXTENSION;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private enum IncorrectMergedFileReason {
        PATH_IS_TOO_LONG,
        INVALID_PATH,
        IS_A_DIRECTORY,
        NO_EXTENSION,
        NOT_ALLOWED_EXTENSION
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class FilesInfo {
        private InputFileInfo upperFileInfo;

        private InputFileInfo lowerFileInfo;

        private MergedFileInfo mergedFileInfo;

        private Subtitles mergedSubtitles;

        boolean upperFileOk() {
            return inputFileOk(upperFileInfo);
        }

        boolean lowerFileOk() {
            return inputFileOk(lowerFileInfo);
        }

        private static boolean inputFileOk(InputFileInfo fileInfo) {
            return fileInfo != null && !fileInfo.isDuplicate && fileInfo.getIncorrectFileReason() == null;
        }
    }
}