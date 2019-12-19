package kirill.subtitlesmerger.gui.tabs.merge_single_files;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiConstants;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.GuiPreferences;
import kirill.subtitlesmerger.logic.core.Merger;
import kirill.subtitlesmerger.logic.core.Parser;
import kirill.subtitlesmerger.logic.core.Writer;
import kirill.subtitlesmerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

@CommonsLog
public class MergeSingleFilesTabController {
    public static final String FILE_NOT_CHOSEN_PATH = "not selected";

    private Stage stage;

    private GuiPreferences preferences;

    @FXML
    private Button upperSubtitlesChooseButton;

    @FXML
    private Label upperSubtitlesPathLabel;

    @FXML
    private Button lowerSubtitlesChooseButton;

    @FXML
    private Label lowerSubtitlesPathLabel;

    @FXML
    private Button mergedSubtitlesChooseButton;

    @FXML
    private Label mergedSubtitlesPathLabel;

    @FXML
    private Button mergeButton;

    @FXML
    private Label resultLabel;

    private InputFileInfo upperSubtitlesFileInfo;

    private InputFileInfo lowerSubtitlesFileInfo;

    private File mergedSubtitlesFile;

    public void initialize(Stage stage, GuiContext context) {
        this.stage = stage;
        this.preferences = context.getPreferences();
    }

    @FXML
    private void upperSubtitlesButtonClicked() {
        processInputFile(FileType.UPPER_SUBTITLES);
    }

    private void processInputFile(FileType fileType) {
        File file = getFile(fileType, stage, preferences).orElse(null);

        clearErrorsAndResult();
        saveLastDirectoryInConfigIfNecessary(file, fileType);
        setFileInfoAndShowErrorsIfNecessary(file, fileType);
        setPathLabels(fileType);
        setMergeButtonVisibility();
    }

    private static Optional<File> getFile(FileType fileType, Stage stage, GuiPreferences preferences) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle(getChooserTitle(fileType));
        fileChooser.setInitialDirectory(getChooserInitialDirectory(fileType, preferences));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("subrip files (*.srt)", "*.srt")
        );

        switch (fileType) {
            case UPPER_SUBTITLES:
            case LOWER_SUBTITLES:
                return Optional.ofNullable(fileChooser.showOpenDialog(stage));
            case MERGED_SUBTITLES:
                return Optional.ofNullable(fileChooser.showSaveDialog(stage));
            default:
                throw new IllegalStateException();
        }
    }

    private static String getChooserTitle(FileType fileType) {
        switch (fileType) {
            case UPPER_SUBTITLES:
                return "Please choose the file with the upper subtitles";
            case LOWER_SUBTITLES:
                return "Please choose the file with the lower subtitles";
            case MERGED_SUBTITLES:
                return "Please choose where to save the result";
            default:
                throw new IllegalStateException();
        }
    }

    private static File getChooserInitialDirectory(FileType fileType, GuiPreferences preferences) {
        switch (fileType) {
            case UPPER_SUBTITLES:
                return ObjectUtils.firstNonNull(
                        preferences.getUpperSubtitlesLastDirectory(),
                        preferences.getLowerSubtitlesLastDirectory(),
                        preferences.getMergedSubtitlesLastDirectory()
                );
            case LOWER_SUBTITLES:
                return ObjectUtils.firstNonNull(
                        preferences.getLowerSubtitlesLastDirectory(),
                        preferences.getUpperSubtitlesLastDirectory(),
                        preferences.getMergedSubtitlesLastDirectory()
                );
            case MERGED_SUBTITLES:
                return ObjectUtils.firstNonNull(
                        preferences.getMergedSubtitlesLastDirectory(),
                        preferences.getUpperSubtitlesLastDirectory(),
                        preferences.getLowerSubtitlesLastDirectory()
                );
            default:
                throw new IllegalStateException();
        }
    }

    private void clearErrorsAndResult() {
        upperSubtitlesChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        lowerSubtitlesChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        mergedSubtitlesChooseButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
        resultLabel.setText("");
        resultLabel.getStyleClass().removeAll(GuiConstants.LABEL_SUCCESS_CLASS, GuiConstants.LABEL_ERROR_CLASS);
    }

    private void saveLastDirectoryInConfigIfNecessary(File file, FileType fileType) {
        if (file == null) {
            return;
        }

        try {
            switch (fileType) {
                case UPPER_SUBTITLES:
                    preferences.saveUpperSubtitlesLastDirectory(file.getParent());
                    return;
                case LOWER_SUBTITLES:
                    preferences.saveLowerSubtitlesLastDirectory(file.getParent());
                    return;
                case MERGED_SUBTITLES:
                    preferences.saveMergedSubtitlesLastDirectory(file.getParent());
                    return;
                default:
                    throw new IllegalStateException();
            }
        } catch (GuiPreferences.ConfigException e) {
            log.error(
                    "failed to save last directory , file " + file.getAbsolutePath() + ": "
                            + ExceptionUtils.getStackTrace(e)
            );
        }
    }

    private void setFileInfoAndShowErrorsIfNecessary(File file, FileType fileType) {
        if (file != null && isDuplicate(file, fileType)) {
            if (fileType == FileType.UPPER_SUBTITLES) {
                this.upperSubtitlesFileInfo = null;

                showInputFileErrors(
                        "you can't use the same file twice",
                        null
                );
            } else if (fileType == FileType.LOWER_SUBTITLES) {
                this.lowerSubtitlesFileInfo = null;

                showInputFileErrors(
                        null,
                        "you can't use the same file twice"
                );
            } else {
                throw new IllegalStateException();
            }
        } else {
            if (fileType == FileType.UPPER_SUBTITLES) {
                this.upperSubtitlesFileInfo = getInputFileInfo(file, fileType).orElse(null);
            } else if (fileType == FileType.LOWER_SUBTITLES) {
                this.lowerSubtitlesFileInfo = getInputFileInfo(file, fileType).orElse(null);
            } else {
                throw new IllegalStateException();
            }

            showInputFileErrorsIfNecessary();
        }
    }

    private boolean isDuplicate(File file, FileType fileType) {
        if (fileType == FileType.UPPER_SUBTITLES) {
            return lowerSubtitlesFileInfo != null && Objects.equals(file, lowerSubtitlesFileInfo.getFile());
        } else if (fileType == FileType.LOWER_SUBTITLES) {
            return upperSubtitlesFileInfo != null && Objects.equals(file, upperSubtitlesFileInfo.getFile());
        } else {
            throw new IllegalStateException();
        }
    }

    private static Optional<InputFileInfo> getInputFileInfo(File file, FileType fileType) {
        if (file == null) {
            return Optional.empty();
        }

        if (file.length() / 1024 / 1024 > GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES) {
            return Optional.of(new InputFileInfo(file, null, IncorrectInputFileReason.FILE_IS_TOO_BIG));
        }

        try {
            Subtitles subtitles = Parser.fromSubRipText(
                    FileUtils.readFileToString(file, StandardCharsets.UTF_8),
                    fileType.toString(),
                    null
            );

            return Optional.of(new InputFileInfo(file, subtitles, null));
        } catch (IOException e) {
            return Optional.of(new InputFileInfo(file, null, IncorrectInputFileReason.CAN_NOT_READ_FILE));
        } catch (Parser.IncorrectFormatException e) {
            return Optional.of(
                    new InputFileInfo(file, null, IncorrectInputFileReason.INCORRECT_SUBTITLE_FORMAT)
            );
        }
    }

    private void showInputFileErrorsIfNecessary() {
        String upperSubtitlesFileErrorMessage = null;
        if (upperSubtitlesFileInfo != null && upperSubtitlesFileInfo.getIncorrectFileReason() != null) {
            upperSubtitlesFileErrorMessage = getErrorText(upperSubtitlesFileInfo);
        }

        String lowerSubtitlesFileErrorMessage = null;
        if (lowerSubtitlesFileInfo != null && lowerSubtitlesFileInfo.getIncorrectFileReason() != null) {
            lowerSubtitlesFileErrorMessage = getErrorText(lowerSubtitlesFileInfo);
        }

        boolean atLeastOneError = !StringUtils.isBlank(upperSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(lowerSubtitlesFileErrorMessage);

        if (atLeastOneError) {
            showInputFileErrors(upperSubtitlesFileErrorMessage, lowerSubtitlesFileErrorMessage);
        }
    }

    private static String getErrorText(InputFileInfo fileInfo) {
        StringBuilder result = new StringBuilder("file ")
                .append(fileInfo.getFile().getAbsolutePath())
                .append(" is incorrect: ");

        switch (fileInfo.getIncorrectFileReason()) {
            case CAN_NOT_READ_FILE:
                result.append("can't read the file");
                break;
            case FILE_IS_TOO_BIG:
                result.append("file is too big (>")
                        .append(GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES).append(" megabytes)");
                break;
            case INCORRECT_SUBTITLE_FORMAT:
                result.append("incorrect subtitle format");
                break;
            default:
                throw new IllegalStateException();
        }

        return result.toString();
    }

    private void showInputFileErrors(
            String upperSubtitlesFileErrorMessage,
            String lowerSubtitlesFileErrorMessage
    ) {
        if (!StringUtils.isBlank(upperSubtitlesFileErrorMessage)) {
            if (!upperSubtitlesChooseButton.getStyleClass().contains(GuiConstants.BUTTON_ERROR_CLASS)) {
                upperSubtitlesChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        }

        if (!StringUtils.isBlank(lowerSubtitlesFileErrorMessage)) {
            if (!lowerSubtitlesChooseButton.getStyleClass().contains(GuiConstants.BUTTON_ERROR_CLASS)) {
                lowerSubtitlesChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
            }
        }

        StringBuilder combinedErrorsMessage = new StringBuilder();

        if (!StringUtils.isBlank(upperSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\u2022").append(" ").append(upperSubtitlesFileErrorMessage).append("\n");
        }

        if (!StringUtils.isBlank(lowerSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\u2022").append(" ").append(lowerSubtitlesFileErrorMessage).append("\n");
        }

        resultLabel.setText(combinedErrorsMessage.toString());
        resultLabel.getStyleClass().remove(GuiConstants.LABEL_SUCCESS_CLASS);
        if (!resultLabel.getStyleClass().contains(GuiConstants.LABEL_ERROR_CLASS)) {
            resultLabel.getStyleClass().add(GuiConstants.LABEL_ERROR_CLASS);
        }
    }

    private void setPathLabels(FileType fileType) {
        File file;
        switch (fileType) {
            case UPPER_SUBTITLES:
                file = upperSubtitlesFileInfo != null ? upperSubtitlesFileInfo.getFile() : null;
                upperSubtitlesPathLabel.setText(getPathText(file));
                break;
            case LOWER_SUBTITLES:
                file = lowerSubtitlesFileInfo != null ? lowerSubtitlesFileInfo.getFile() : null;
                lowerSubtitlesPathLabel.setText(getPathText(file));
                break;
            case MERGED_SUBTITLES:
                mergedSubtitlesPathLabel.setText(getPathText(mergedSubtitlesFile));
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private static String getPathText(File file) {
        if (file == null) {
            return FILE_NOT_CHOSEN_PATH;
        } else {
            return file.getAbsolutePath();
        }
    }

    private void setMergeButtonVisibility() {
        boolean disable = upperSubtitlesFileInfo == null || upperSubtitlesFileInfo.getIncorrectFileReason() != null
                || lowerSubtitlesFileInfo == null || lowerSubtitlesFileInfo.getIncorrectFileReason() != null
                || mergedSubtitlesFile == null;
        mergeButton.setDisable(disable);
    }

    @FXML
    private void lowerSubtitlesButtonClicked() {
        processInputFile(FileType.LOWER_SUBTITLES);
    }

    @FXML
    private void mergedSubtitlesButtonClicked() {
        File file = getFile(FileType.MERGED_SUBTITLES, stage, preferences).orElse(null);

        clearErrorsAndResult();
        saveLastDirectoryInConfigIfNecessary(file, FileType.MERGED_SUBTITLES);
        this.mergedSubtitlesFile = file;
        showInputFileErrorsIfNecessary();
        setPathLabels(FileType.MERGED_SUBTITLES);
        setMergeButtonVisibility();
    }

    @FXML
    private void mergeButtonClicked() {
        clearErrorsAndResult();

        Subtitles result = Merger.mergeSubtitles(
                upperSubtitlesFileInfo.getSubtitles(),
                lowerSubtitlesFileInfo.getSubtitles()
        );

        try {
            FileUtils.writeStringToFile(mergedSubtitlesFile, Writer.toSubRipText(result), StandardCharsets.UTF_8);
        } catch (IOException e) {
            showFailedToWriteMessage();

            return;
        }

        showSuccessMessage();
    }

    private void showSuccessMessage() {
        resultLabel.setText("Subtitles have been merged successfully!");

        resultLabel.getStyleClass().remove(GuiConstants.LABEL_ERROR_CLASS);
        if (!resultLabel.getStyleClass().contains(GuiConstants.LABEL_SUCCESS_CLASS)) {
            resultLabel.getStyleClass().add(GuiConstants.LABEL_SUCCESS_CLASS);
        }
    }

    private void showFailedToWriteMessage() {
        resultLabel.setText("Can't merge subtitles:" + "\n" + "\u2022" + " " + "can't write to this file");

        if (!mergedSubtitlesChooseButton.getStyleClass().contains(GuiConstants.BUTTON_ERROR_CLASS)) {
            mergedSubtitlesChooseButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
        }

        resultLabel.getStyleClass().remove(GuiConstants.LABEL_SUCCESS_CLASS);
        if (!resultLabel.getStyleClass().contains(GuiConstants.LABEL_ERROR_CLASS)) {
            resultLabel.getStyleClass().add(GuiConstants.LABEL_ERROR_CLASS);
        }
    }

    private enum FileType {
        UPPER_SUBTITLES,
        LOWER_SUBTITLES,
        MERGED_SUBTITLES
    }

    @AllArgsConstructor
    @Getter
    private static class InputFileInfo {
        private File file;

        private Subtitles subtitles;

        private IncorrectInputFileReason incorrectFileReason;
    }

    private enum IncorrectInputFileReason {
        CAN_NOT_READ_FILE,
        INCORRECT_SUBTITLE_FORMAT,
        FILE_IS_TOO_BIG
    }
}