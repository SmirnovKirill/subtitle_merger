package kirill.subtitlemerger.gui.tabs.subtitle_files;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.core.GuiUtils;
import kirill.subtitlemerger.gui.core.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.core.custom_controls.MultiColorResultLabels;
import kirill.subtitlemerger.gui.core.custom_controls.SubtitlePreviewWithEncoding;
import kirill.subtitlemerger.gui.core.custom_controls.SubtitlePreviewWithEncodingController;
import kirill.subtitlemerger.gui.core.entities.MultiPartResult;
import kirill.subtitlemerger.logic.core.Merger;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.core.Writer;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.utils.FileValidator;
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
public class SubtitleFilesTabController {
    private Stage stage;

    private GuiSettings settings;

    @FXML
    private Pane mainPane;

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
    private Pane progressPane;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label progressLabel;

    @FXML
    private MultiColorResultLabels resultLabels;

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
        this.filesInfo = new FilesInfo(null, null, null);

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

        //todo refactor, logic is cluttered
        BackgroundTask<Optional<InputFileInfo>> task = new BackgroundTask<>() {
            @Override
            protected Optional<InputFileInfo> run() {
                updateMessage("processing file " + path + "...");
                return getInputFileInfo(path, fileType, fileOrigin, filesInfo);
            }

            @Override
            protected void onFinish(Optional<InputFileInfo> result) {
                InputFileInfo inputFileInfo = result.orElse(null);
                updateFilesInfo(inputFileInfo, fileType, filesInfo);
                markOtherFileNotDuplicate(fileType, filesInfo);
                if (fileOrigin == FileOrigin.FILE_CHOOSER && inputFileInfo != null && inputFileInfo.getParent() != null) {
                    saveLastDirectoryInConfig(fileType.getExtendedFileType(), inputFileInfo.getParent(), settings);
                }

                updateScene(fileOrigin);

                progressPane.setVisible(false);
                mainPane.setDisable(false);
            }
        };

        progressPane.setVisible(true);
        mainPane.setDisable(true);
        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());

        task.start();
    }

    private static boolean pathNotChanged(String path, ExtendedFileType fileType, FilesInfo filesInfo) {
        String currentPath;
        if (fileType == ExtendedFileType.UPPER_SUBTITLES) {
            currentPath = filesInfo.getUpperFileInfo() != null ? filesInfo.getUpperFileInfo().getPath() : null;
        } else if (fileType == ExtendedFileType.LOWER_SUBTITLES) {
            currentPath = filesInfo.getLowerFileInfo() != null ? filesInfo.getLowerFileInfo().getPath() : null;
        } else if (fileType == ExtendedFileType.MERGED_SUBTITLES) {
            currentPath = filesInfo.getMergedFileInfo() != null ? filesInfo.getMergedFileInfo().getPath() : null;
        } else {
            throw new IllegalStateException();
        }

        if (StringUtils.isBlank(path)) {
            return currentPath == null;
        } else {
            return Objects.equals(path, currentPath);
        }
    }

    private static Optional<InputFileInfo> getInputFileInfo(
            String path,
            InputFileType fileType,
            FileOrigin fileOrigin,
            FilesInfo filesInfo
    ) {
        FileValidator.InputFileInfo validatorFileInfo = FileValidator.getInputFileInfo(
                path,
                Collections.singletonList("srt"),
                GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024,
                true
        ).orElse(null);
        if (validatorFileInfo == null) {
            return Optional.empty();
        }

        if (validatorFileInfo.getIncorrectFileReason() != null) {
            return Optional.of(
                    new InputFileInfo(
                            path,
                            validatorFileInfo.getFile(),
                            validatorFileInfo.getParent(),
                            fileOrigin,
                            false,
                            validatorFileInfo.getContent(),
                            null,
                            null,
                            SubtitleFilesTabController.InputFileInfo.from(validatorFileInfo.getIncorrectFileReason())
                    )
            );
        }

        try {
            Subtitles subtitles = Parser.fromSubRipText(
                    new String(validatorFileInfo.getContent(), StandardCharsets.UTF_8),
                    null
            );

            return Optional.of(
                    new InputFileInfo(
                            path,
                            validatorFileInfo.getFile(),
                            validatorFileInfo.getParent(),
                            fileOrigin,
                            isDuplicate(validatorFileInfo.getFile(), fileType, filesInfo),
                            validatorFileInfo.getContent(),
                            StandardCharsets.UTF_8,
                            subtitles,
                            null
                    )
            );
        } catch (Parser.IncorrectFormatException e) {
            return Optional.of(
                    new InputFileInfo(
                            path,
                            validatorFileInfo.getFile(),
                            validatorFileInfo.getParent(),
                            fileOrigin,
                            false,
                            validatorFileInfo.getContent(),
                            StandardCharsets.UTF_8,
                            null,
                            SubtitleFilesTabController.IncorrectInputFileReason.INCORRECT_SUBTITLE_FORMAT
                    )
            );
        }
    }

    private static boolean isDuplicate(File file, InputFileType fileType, FilesInfo filesInfo) {
        if (fileType == InputFileType.UPPER_SUBTITLES) {
            return filesInfo.getLowerFileInfo() != null && Objects.equals(file, filesInfo.getLowerFileInfo().getFile());
        } else if (fileType == InputFileType.LOWER_SUBTITLES) {
            return filesInfo.getUpperFileInfo() != null && Objects.equals(file, filesInfo.getUpperFileInfo().getFile());
        } else {
            throw new IllegalStateException();
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

    private static void saveLastDirectoryInConfig(ExtendedFileType fileType, File parent, GuiSettings settings) {
        try {
            switch (fileType) {
                case UPPER_SUBTITLES:
                    settings.saveUpperSubtitlesLastDirectory(parent.getAbsolutePath());
                    return;
                case LOWER_SUBTITLES:
                    settings.saveLowerSubtitlesLastDirectory(parent.getAbsolutePath());
                    return;
                case MERGED_SUBTITLES:
                    settings.saveMergedSubtitlesLastDirectory(parent.getAbsolutePath());
                    return;
                default:
                    throw new IllegalStateException();
            }
        } catch (GuiSettings.ConfigException e) {
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

        setPreviewButtonsDisabled();
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

        resultLabels.clear();
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

    private void setPreviewButtonsDisabled() {
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
        if (upperFileInfo != null && (upperFileInfo.isDuplicate || upperFileInfo.getIncorrectFileReason() != null)) {
            showFileElementsAsIncorrect(ExtendedFileType.UPPER_SUBTITLES);

            if (upperFileInfo.getIncorrectFileReason() != null) {
                errorMessageParts.add(getErrorText(upperFileInfo.getPath(), upperFileInfo.getIncorrectFileReason()));
            } else if (upperFileInfo.isDuplicate()) {
                errorMessageParts.add("You have already selected this file for the lower subtitles");
            }
        }

        InputFileInfo lowerFileInfo = filesInfo.getLowerFileInfo();
        if (lowerFileInfo != null && (lowerFileInfo.isDuplicate || lowerFileInfo.getIncorrectFileReason() != null)) {
            showFileElementsAsIncorrect(ExtendedFileType.LOWER_SUBTITLES);

            if (lowerFileInfo.getIncorrectFileReason() != null) {
                errorMessageParts.add(getErrorText(lowerFileInfo.getPath(), lowerFileInfo.getIncorrectFileReason()));
            } else if (lowerFileInfo.isDuplicate()) {
                errorMessageParts.add("You have already selected this file for the upper subtitles");
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

        resultLabels.update(MultiPartResult.onlyError(errorMessage.toString()));
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
        path = getShortenedPath(path.trim());

        switch (reason) {
            case PATH_IS_TOO_LONG:
                return "File path is too long";
            case INVALID_PATH:
                return "File path is invalid";
            case IS_A_DIRECTORY:
                return path + " is a directory, not a file";
            case FILE_DOES_NOT_EXIST:
                return "File " + path + " doesn't exist";
            case FAILED_TO_GET_PARENT_DIRECTORY:
                return path + ": failed to get parent directory";
            case EXTENSION_IS_NOT_VALID:
                return "File " + path + " has an incorrect extension";
            case FILE_IS_TOO_BIG:
                return "File " + path + " is too big (>"
                        + GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES + " megabytes)";
            case FAILED_TO_READ_CONTENT:
                return path + ": failed to read the file";
            case INCORRECT_SUBTITLE_FORMAT:
                return "File " + path + " has an incorrect subtitle format, it can happen if the file is not UTF-8-encoded"
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
            case EXTENSION_IS_NOT_VALID:
                return "File " + path + " has an incorrect extension";
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
                        filesInfo.getMergedFileInfo() != null ? filesInfo.getMergedFileInfo().getPath() : null
                );
                return;
            }
        }

        filesInfo.setMergedFileInfo(mergedFileInfo);
        if (fileOrigin == FileOrigin.FILE_CHOOSER && mergedFileInfo != null && mergedFileInfo.getParent() != null) {
            saveLastDirectoryInConfig(ExtendedFileType.MERGED_SUBTITLES, mergedFileInfo.getParent(), settings);
        }

        updateScene(fileOrigin);
    }

    private static Optional<MergedFileInfo> getMergedFileInfo(String path, FileOrigin fileOrigin) {
        FileValidator.OutputFileInfo validatorFileInfo = FileValidator.getOutputFileInfo(
                path,
                Collections.singletonList("srt"),
                true
        ).orElse(null);
        if (validatorFileInfo == null) {
            return Optional.empty();
        }

        return Optional.of(
                new MergedFileInfo(
                        path,
                        validatorFileInfo.getFile(),
                        validatorFileInfo.getParent(),
                        fileOrigin,
                        validatorFileInfo.getIncorrectFileReason() != null
                                ? MergedFileInfo.from(validatorFileInfo.getIncorrectFileReason())
                                : null
                )
        );
    }

    private static boolean fileExists(MergedFileInfo fileInfo) {
        return fileInfo != null && fileInfo.getFile() != null && fileInfo.getFile().exists();
    }

    private boolean agreeToOverwrite(MergedFileInfo mergedFileInfo) {
        agreeToOverwriteInProgress = true;

        Stage dialogStage = new Stage();

        String fileName = GuiUtils.getShortenedStringIfNecessary(
                mergedFileInfo.getFile().getName(),
                0,
                32
        );
        String parentName = "-";
        if (mergedFileInfo.getParent() != null) {
            parentName = GuiUtils.getShortenedStringIfNecessary(
                    mergedFileInfo.getParent().getName(),
                    0,
                    32
            );
        }

        FileExistsDialog fileExistsDialog = new FileExistsDialog();
        fileExistsDialog.initialize(fileName, parentName, dialogStage);

        //todo set stage parameters in the same manner everywhere
        dialogStage.setTitle("File exists!");
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.setResizable(false);

        Scene scene = new Scene(fileExistsDialog);
        scene.getStylesheets().add("/gui/style.css");
        dialogStage.setScene(scene);

        dialogStage.showAndWait();

        agreeToOverwriteInProgress = false;

        return fileExistsDialog.isAgreeToOverwrite();
    }

    @FXML
    private void upperPreviewClicked() {
        SubtitlePreviewWithEncodingController.UserSelection userSelection = showInputSubtitlePreview(filesInfo.getUpperFileInfo());
        updateSubtitlesAndEncodingIfChanged(filesInfo.getUpperFileInfo(), userSelection);
    }

    private SubtitlePreviewWithEncodingController.UserSelection showInputSubtitlePreview(InputFileInfo fileInfo) {
        String path = GuiUtils.getShortenedStringIfNecessary(
                fileInfo.getPath().trim(),
                0,
                128
        );

        Stage dialogStage = new Stage();

        SubtitlePreviewWithEncodingController dialogController = new SubtitlePreviewWithEncodingController();
        SubtitlePreviewWithEncoding subtitlePreviewDialog = new SubtitlePreviewWithEncoding(dialogController);
        dialogController.initialize(
                fileInfo.getRawData(),
                fileInfo.getEncoding(),
                fileInfo.getSubtitles(),
                path,
                dialogStage
        );

        dialogStage.setTitle("Subtitle preview");
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.setResizable(false);

        Scene scene = new Scene(subtitlePreviewDialog);
        scene.getStylesheets().add("/gui/style.css");
        dialogStage.setScene(scene);

        dialogStage.showAndWait();

        return dialogController.getUserSelection();
    }

    private void updateSubtitlesAndEncodingIfChanged(
            InputFileInfo fileInfo,
            SubtitlePreviewWithEncodingController.UserSelection userSelection
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

        updateScene(fileInfo.getFileOrigin());
    }

    @FXML
    private void upperChooseClicked() {
        File file = getInputFile(InputFileType.UPPER_SUBTITLES, stage, settings).orElse(null);
        if (file == null) {
            return;
        }

        processInputFilePath(file.getAbsolutePath(), InputFileType.UPPER_SUBTITLES, FileOrigin.FILE_CHOOSER);
    }

    private static Optional<File> getInputFile(InputFileType fileType, Stage stage, GuiSettings settings) {
        FileChooser fileChooser = new FileChooser();

        String chooserTitle;
        File chooserInitialDirectory;
        if (fileType == InputFileType.UPPER_SUBTITLES) {
            chooserTitle = "Please choose the file with the upper subtitles";
            chooserInitialDirectory = ObjectUtils.firstNonNull(
                    settings.getUpperSubtitlesLastDirectory(),
                    settings.getLowerSubtitlesLastDirectory(),
                    settings.getMergedSubtitlesLastDirectory()
            );
        } else if (fileType == InputFileType.LOWER_SUBTITLES) {
            chooserTitle = "Please choose the file with the lower subtitles";
            chooserInitialDirectory = ObjectUtils.firstNonNull(
                    settings.getLowerSubtitlesLastDirectory(),
                    settings.getUpperSubtitlesLastDirectory(),
                    settings.getMergedSubtitlesLastDirectory()
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
        SubtitlePreviewWithEncodingController.UserSelection dialogResult = showInputSubtitlePreview(filesInfo.getLowerFileInfo());
        updateSubtitlesAndEncodingIfChanged(filesInfo.getLowerFileInfo(), dialogResult);
    }

    @FXML
    private void lowerChooseClicked() {
        File file = getInputFile(InputFileType.LOWER_SUBTITLES, stage, settings).orElse(null);
        if (file == null) {
            return;
        }

        processInputFilePath(file.getAbsolutePath(), InputFileType.LOWER_SUBTITLES, FileOrigin.FILE_CHOOSER);
    }

    @FXML
    private void mergedSubtitlesButtonClicked() {
        File file = getMergedFile(stage, settings).orElse(null);
        if (file == null) {
            return;
        }

        processMergedFilePath(file.getAbsolutePath(), FileOrigin.FILE_CHOOSER);
    }

    private static Optional<File> getMergedFile(Stage stage, GuiSettings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Please choose where to save the result");
        fileChooser.setInitialDirectory(
                ObjectUtils.firstNonNull(
                        settings.getMergedSubtitlesLastDirectory(),
                        settings.getUpperSubtitlesLastDirectory(),
                        settings.getLowerSubtitlesLastDirectory()
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

        //todo refactor, logic is cluttered
        BackgroundTask<MultiPartResult> task = new BackgroundTask<>() {
            @Override
            protected MultiPartResult run() {
                updateMessage("merging subtitles...");

                Subtitles result = Merger.mergeSubtitles(
                        filesInfo.getUpperFileInfo().getSubtitles(),
                        filesInfo.getLowerFileInfo().getSubtitles()
                );

                try {
                    FileUtils.writeStringToFile(
                            filesInfo.getMergedFileInfo().getFile(),
                            Writer.toSubRipText(result),
                            StandardCharsets.UTF_8
                    );

                    return MultiPartResult.onlySuccess("Subtitles have been merged successfully!");
                } catch (IOException e) {
                    return MultiPartResult.onlyError(
                            "Can't merge subtitles:" + System.lineSeparator() + "\u2022 can't write to this file"
                    );
                }
            }

            @Override
            protected void onFinish(MultiPartResult result) {
                if (!StringUtils.isBlank(result.getError())) {
                    showFileElementsAsIncorrect(ExtendedFileType.MERGED_SUBTITLES);
                }
                resultLabels.update(result);

                progressPane.setVisible(false);
                mainPane.setDisable(false);
            }
        };

        progressPane.setVisible(true);
        mainPane.setDisable(true);
        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());

        task.start();
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

    private enum FileOrigin {
        TEXT_FIELD,
        FILE_CHOOSER
    }

    @AllArgsConstructor
    @Getter
    private static class InputFileInfo {
        private String path;

        private File file;

        private File parent;

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

        static IncorrectInputFileReason from(FileValidator.IncorrectInputFileReason reason) {
            switch (reason) {
                case PATH_IS_TOO_LONG:
                    return SubtitleFilesTabController.IncorrectInputFileReason.PATH_IS_TOO_LONG;
                case INVALID_PATH:
                    return SubtitleFilesTabController.IncorrectInputFileReason.INVALID_PATH;
                case IS_A_DIRECTORY:
                    return SubtitleFilesTabController.IncorrectInputFileReason.IS_A_DIRECTORY;
                case FILE_DOES_NOT_EXIST:
                    return SubtitleFilesTabController.IncorrectInputFileReason.FILE_DOES_NOT_EXIST;
                case FAILED_TO_GET_PARENT_DIRECTORY:
                    return SubtitleFilesTabController.IncorrectInputFileReason.FAILED_TO_GET_PARENT_DIRECTORY;
                case EXTENSION_IS_NOT_VALID:
                    return SubtitleFilesTabController.IncorrectInputFileReason.EXTENSION_IS_NOT_VALID;
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
        FILE_DOES_NOT_EXIST,
        FAILED_TO_GET_PARENT_DIRECTORY,
        EXTENSION_IS_NOT_VALID,
        FILE_IS_TOO_BIG,
        FAILED_TO_READ_CONTENT,
        INCORRECT_SUBTITLE_FORMAT
    }

    @AllArgsConstructor
    @Getter
    private static class MergedFileInfo {
        private String path;

        private File file;

        private File parent;

        private FileOrigin fileOrigin;

        private IncorrectMergedFileReason incorrectFileReason;

        static IncorrectMergedFileReason from(FileValidator.IncorrectOutputFileReason reason) {
            switch (reason) {
                case PATH_IS_TOO_LONG:
                    return IncorrectMergedFileReason.PATH_IS_TOO_LONG;
                case INVALID_PATH:
                    return IncorrectMergedFileReason.INVALID_PATH;
                case IS_A_DIRECTORY:
                    return IncorrectMergedFileReason.IS_A_DIRECTORY;
                case EXTENSION_IS_NOT_VALID:
                    return IncorrectMergedFileReason.EXTENSION_IS_NOT_VALID;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private enum IncorrectMergedFileReason {
        PATH_IS_TOO_LONG,
        INVALID_PATH,
        IS_A_DIRECTORY,
        EXTENSION_IS_NOT_VALID
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class FilesInfo {
        private InputFileInfo upperFileInfo;

        private InputFileInfo lowerFileInfo;

        private MergedFileInfo mergedFileInfo;

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