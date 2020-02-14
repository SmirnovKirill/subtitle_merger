package kirill.subtitlemerger.gui.tabs.subtitle_files;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.core.GuiUtils;
import kirill.subtitlemerger.gui.core.custom_controls.MultiColorResultLabels;
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
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@CommonsLog
public class SubtitleFilesTabController {
    //todo refactor
    public static final int INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES = 10;

    private Stage stage;

    private GuiSettings settings;

    @FXML
    private Button upperChooseButton;

    @FXML
    private TextField upperPathField;

    @FXML
    private Button lowerChooseButton;

    @FXML
    private TextField lowerPathField;

    @FXML
    private Button mergedChooseButton;

    @FXML
    private TextField mergedPathField;

    @FXML
    private Button mergeButton;

    @FXML
    private MultiColorResultLabels resultLabels;

    private FilesInfo filesInfo;

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
                (path) -> processOutputFilePath(path, FileOrigin.TEXT_FIELD)
        );
    }

    private void processInputFilePath(String path, InputFileType fileType, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && pathNotChanged(path, fileType.getExtendedFileType(), filesInfo)) {
            return;
        }

        InputFileInfo inputFileInfo = getInputFileInfo(path, fileType, filesInfo).orElse(null);
        updateFilesInfo(inputFileInfo, fileType, filesInfo);
        if (fileOrigin == FileOrigin.FILE_CHOOSER && inputFileInfo != null && inputFileInfo.getParent() != null) {
            saveLastDirectoryInConfig(fileType.getExtendedFileType(), inputFileInfo.getParent(), settings);
        }

        updateScene(fileOrigin != FileOrigin.TEXT_FIELD);
    }

    private static boolean pathNotChanged(String path, ExtendedFileType fileType, FilesInfo filesInfo) {
        if (fileType == ExtendedFileType.UPPER_SUBTITLES) {
            return filesInfo.getUpperFileInfo() != null && Objects.equals(path, filesInfo.getUpperFileInfo().getPath());
        } else if (fileType == ExtendedFileType.LOWER_SUBTITLES) {
            return filesInfo.getLowerFileInfo() != null && Objects.equals(path, filesInfo.getLowerFileInfo().getPath());
        } else if (fileType == ExtendedFileType.MERGED_SUBTITLES) {
            return filesInfo.getMergedFileInfo() != null
                    && Objects.equals(path, filesInfo.getMergedFileInfo().getPath());
        } else {
            throw new IllegalStateException();
        }
    }

    private static Optional<InputFileInfo> getInputFileInfo(String path, InputFileType fileType, FilesInfo filesInfo) {
        FileValidator.InputFileInfo validatorFileInfo = FileValidator.getInputFileInfo(
                path,
                Collections.singletonList("srt"),
                INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES * 1024 * 1024,
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
                            false,
                            null,
                            SubtitleFilesTabController.InputFileInfo.from(validatorFileInfo.getIncorrectFileReason())
                    )
            );
        }

        try {
            Subtitles subtitles = Parser.fromSubRipText(
                    new String(validatorFileInfo.getContent(), StandardCharsets.UTF_8), //todo auto-detect encoding
                    fileType.toString(),
                    null
            );

            return Optional.of(
                    new InputFileInfo(
                            path,
                            validatorFileInfo.getFile(),
                            validatorFileInfo.getParent(),
                            isDuplicate(validatorFileInfo.getFile(), fileType, filesInfo),
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
                            false,
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

    private void updateScene(boolean updatePathFields) {
        clearState();

        if (updatePathFields) {
            updatePathFields();
        }

        setMergeButtonVisibility();
        showErrorsIfNecessary();
    }

    private void clearState() {
        upperPathField.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        upperChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        lowerPathField.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        lowerChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        mergedPathField.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        mergedChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);

        resultLabels.clear();
    }

    private void updatePathFields() {
        upperPathField.setText(
                filesInfo.getUpperFileInfo() != null
                        ? filesInfo.getUpperFileInfo().getFile().getAbsolutePath()
                        : null
        );
        lowerPathField.setText(
                filesInfo.getLowerFileInfo() != null
                        ? filesInfo.getLowerFileInfo().getFile().getAbsolutePath()
                        : null
        );
        mergedPathField.setText(
                filesInfo.getMergedFileInfo() != null
                        ? filesInfo.getMergedFileInfo().getFile().getAbsolutePath()
                        : null
        );
    }

    private void setMergeButtonVisibility() {
        boolean disable = filesInfo.getUpperFileInfo() == null
                || filesInfo.getUpperFileInfo().getIncorrectFileReason() != null
                || filesInfo.getUpperFileInfo().isDuplicate()
                || filesInfo.getLowerFileInfo() == null
                || filesInfo.getLowerFileInfo().getIncorrectFileReason() != null
                || filesInfo.getLowerFileInfo().isDuplicate()
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

        OutputFileInfo mergedFileInfo = filesInfo.getMergedFileInfo();
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
                errorMessage.append("\n");
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

            upperChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
            upperChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
        } else if (fileType == ExtendedFileType.LOWER_SUBTITLES) {
            lowerPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            lowerPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);

            lowerChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
            lowerChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
        } else if (fileType == ExtendedFileType.MERGED_SUBTITLES) {
            mergedPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            mergedPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);

            mergedChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
            mergedChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
        } else {
            throw new IllegalStateException();
        }
    }

    private static String getErrorText(String path, IncorrectInputFileReason reason) {
        path = GuiUtils.getShortenedStringIfNecessary(path, 50, 50);

        switch (reason) {
            case FILE_DOES_NOT_EXIST:
                return "File " + path + " doesn't exist";
            case NOT_A_FILE:
                return path + " is not a file";
            case FAILED_TO_GET_PARENT_DIRECTORY:
                return path + ": failed to get parent directory";
            case EXTENSION_IS_NOT_VALID:
                return "File " + path + " has an incorrect extension";
            case FILE_IS_TOO_BIG:
                return "File " + path + " is too big (>" + INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES + " megabytes)";
            case FAILED_TO_READ_CONTENT:
                return path + ": failed to read the file";
            case INCORRECT_SUBTITLE_FORMAT:
                return "File " + path + " has incorrect subtitle format";
            default:
                throw new IllegalStateException();
        }
    }

    private static String getErrorText(String path, IncorrectOutputFileReason reason) {
        path = GuiUtils.getShortenedStringIfNecessary(path, 50, 50);

        switch (reason) {
            case FILE_DOES_NOT_EXIST:
                return "File " + path + " doesn't exist";
            case NOT_A_FILE:
                return path + " is not a file";
            case FAILED_TO_GET_PARENT_DIRECTORY:
                return path + ": failed to get parent directory";
            case EXTENSION_IS_NOT_VALID:
                return "File " + path + " has an incorrect extension";
            default:
                throw new IllegalStateException();
        }
    }

    private void processOutputFilePath(String path, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.TEXT_FIELD && pathNotChanged(path, ExtendedFileType.MERGED_SUBTITLES, filesInfo)) {
            return;
        }

        OutputFileInfo outputFileInfo = getOutputFileInfo(path).orElse(null);
        filesInfo.setMergedFileInfo(outputFileInfo);
        if (fileOrigin == FileOrigin.FILE_CHOOSER && outputFileInfo != null && outputFileInfo.getParent() != null) {
            saveLastDirectoryInConfig(ExtendedFileType.MERGED_SUBTITLES, outputFileInfo.getParent(), settings);
        }

        updateScene(fileOrigin != FileOrigin.TEXT_FIELD);
    }

    private static Optional<OutputFileInfo> getOutputFileInfo(String path) {
        FileValidator.OutputFileInfo validatorFileInfo = FileValidator.getOutputFileInfo(
                path,
                Collections.singletonList("srt")
        ).orElse(null);
        if (validatorFileInfo == null) {
            return Optional.empty();
        }

        return Optional.of(
                new OutputFileInfo(
                        path,
                        validatorFileInfo.getFile(),
                        validatorFileInfo.getParent(),
                        validatorFileInfo.getIncorrectFileReason() != null
                                ? OutputFileInfo.from(validatorFileInfo.getIncorrectFileReason())
                                : null
                )
        );
    }

    @FXML
    private void upperSubtitlesButtonClicked() {
        File file = getInputFile(InputFileType.UPPER_SUBTITLES, stage, settings).orElse(null);
        processInputFilePath(
                file != null ? file.getAbsolutePath() : null,
                InputFileType.UPPER_SUBTITLES,
                FileOrigin.FILE_CHOOSER
        );
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
    private void lowerSubtitlesButtonClicked() {
        File file = getInputFile(InputFileType.LOWER_SUBTITLES, stage, settings).orElse(null);
        processInputFilePath(
                file != null ? file.getAbsolutePath() : null,
                InputFileType.LOWER_SUBTITLES,
                FileOrigin.FILE_CHOOSER
        );
    }

    @FXML
    private void mergedSubtitlesButtonClicked() {
        File file = getOutputFile(stage, settings).orElse(null);
        processOutputFilePath(file != null ? file.getAbsolutePath() : null, FileOrigin.FILE_CHOOSER);
    }

    private static Optional<File> getOutputFile(Stage stage, GuiSettings settings) {
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
        } catch (IOException e) {
            showFileElementsAsIncorrect(ExtendedFileType.MERGED_SUBTITLES);
            resultLabels.update(MultiPartResult.onlyError("Can't merge subtitles:\n\u2022 can't write to this file"));
            return;
        }

        resultLabels.update(MultiPartResult.onlySuccess("Subtitles have been merged successfully!"));
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

        private boolean isDuplicate;

        private Subtitles subtitles;

        private IncorrectInputFileReason incorrectFileReason;

        static IncorrectInputFileReason from(FileValidator.IncorrectInputFileReason reason) {
            switch (reason) {
                case FILE_DOES_NOT_EXIST:
                    return SubtitleFilesTabController.IncorrectInputFileReason.FILE_DOES_NOT_EXIST;
                case NOT_A_FILE:
                    return SubtitleFilesTabController.IncorrectInputFileReason.NOT_A_FILE;
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
        FILE_DOES_NOT_EXIST,
        NOT_A_FILE,
        FAILED_TO_GET_PARENT_DIRECTORY,
        EXTENSION_IS_NOT_VALID,
        FILE_IS_TOO_BIG,
        FAILED_TO_READ_CONTENT,
        INCORRECT_SUBTITLE_FORMAT
    }

    @AllArgsConstructor
    @Getter
    private static class OutputFileInfo {
        private String path;

        private File file;

        private File parent;

        private IncorrectOutputFileReason incorrectFileReason;

        static IncorrectOutputFileReason from(FileValidator.IncorrectOutputFileReason reason) {
            switch (reason) {
                case FILE_DOES_NOT_EXIST:
                    return IncorrectOutputFileReason.FILE_DOES_NOT_EXIST;
                case NOT_A_FILE:
                    return IncorrectOutputFileReason.NOT_A_FILE;
                case FAILED_TO_GET_PARENT_DIRECTORY:
                    return IncorrectOutputFileReason.FAILED_TO_GET_PARENT_DIRECTORY;
                case EXTENSION_IS_NOT_VALID:
                    return IncorrectOutputFileReason.EXTENSION_IS_NOT_VALID;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private enum IncorrectOutputFileReason {
        FILE_DOES_NOT_EXIST,
        NOT_A_FILE,
        FAILED_TO_GET_PARENT_DIRECTORY,
        EXTENSION_IS_NOT_VALID
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class FilesInfo {
        private InputFileInfo upperFileInfo;

        private InputFileInfo lowerFileInfo;

        private OutputFileInfo mergedFileInfo;
    }
}