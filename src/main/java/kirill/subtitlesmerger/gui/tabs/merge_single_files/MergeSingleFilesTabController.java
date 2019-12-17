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
import kirill.subtitlesmerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CommonsLog
public class MergeSingleFilesTabController {
    private Stage stage;

    private GuiContext guiContext;

    private File upperSubtitlesFile;

    private File lowerSubtitlesFile;

    private File mergedSubtitlesFile;

    private List<IncorrectInputFile> incorrectInputFiles;

    private IncorrectOutputFile incorrectOutputFile;

    @FXML
    private Button upperSubtitlesChooseButton;

    @FXML
    private Label upperSubtitlesPathLabel;

    @FXML
    private FileChooser upperSubtitlesChooser;

    @FXML
    private Button lowerSubtitlesChooseButton;

    @FXML
    private Label lowerSubtitlesPathLabel;

    @FXML
    private FileChooser lowerSubtitlesChooser;

    @FXML
    private Button mergedSubtitlesChooseButton;

    @FXML
    private Label mergedSubtitlesPathLabel;

    @FXML
    private FileChooser mergedSubtitlesChooser;

    @FXML
    private Button mergeButton;

    @FXML
    private Label resultLabel;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.stage = stage;
        this.guiContext = guiContext;

        this.upperSubtitlesChooser.getExtensionFilters().add(generateExtensionFilter());
        this.lowerSubtitlesChooser.getExtensionFilters().add(generateExtensionFilter());
        this.mergedSubtitlesChooser.getExtensionFilters().add(generateExtensionFilter());

        updateFileChoosers();
    }

    private static FileChooser.ExtensionFilter generateExtensionFilter() {
        return new FileChooser.ExtensionFilter("subrip files (*.srt)", "*.srt");
    }

    private void updateFileChoosers() {
        File upperSubtitlesDirectory = ObjectUtils.firstNonNull(
                guiContext.getConfig().getUpperSubtitlesLastDirectory(),
                guiContext.getConfig().getLowerSubtitlesLastDirectory(),
                guiContext.getConfig().getMergedSubtitlesLastDirectory()
        );
        if (upperSubtitlesDirectory != null) {
            upperSubtitlesChooser.setInitialDirectory(upperSubtitlesDirectory);
        }

        File lowerSubtitlesDirectory = ObjectUtils.firstNonNull(
                guiContext.getConfig().getLowerSubtitlesLastDirectory(),
                guiContext.getConfig().getUpperSubtitlesLastDirectory(),
                guiContext.getConfig().getMergedSubtitlesLastDirectory()
        );
        if (lowerSubtitlesDirectory != null) {
            lowerSubtitlesChooser.setInitialDirectory(lowerSubtitlesDirectory);
        }

        File mergedSubtitlesDirectory = ObjectUtils.firstNonNull(
                guiContext.getConfig().getMergedSubtitlesLastDirectory(),
                guiContext.getConfig().getUpperSubtitlesLastDirectory(),
                guiContext.getConfig().getLowerSubtitlesLastDirectory()
        );
        if (mergedSubtitlesDirectory != null) {
            mergedSubtitlesChooser.setInitialDirectory(mergedSubtitlesDirectory);
        }
    }

    @FXML
    private void upperSubtitlesButtonClicked() {
        upperSubtitlesFile = upperSubtitlesChooser.showOpenDialog(stage);

        redrawAfterFileChosen(FileType.UPPER_SUBTITLES);
    }

    private void redrawAfterFileChosen(FileType fileType) {
        removeErrorsAndResult();
        updatePathLabels(fileType);
        showErrorsIfNecessary();
        updateMergeButtonVisibility();
        saveLastDirectoryInConfigIfNecessary(fileType);
        updateFileChoosers();
    }

    private void removeErrorsAndResult() {
        removeButtonErrorClass(upperSubtitlesChooseButton);
        removeButtonErrorClass(lowerSubtitlesChooseButton);
        removeButtonErrorClass(mergedSubtitlesChooseButton);
        clearResult();
    }

    private static void removeButtonErrorClass(Button button) {
        button.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);
    }

    private void clearResult() {
        resultLabel.setText("");
        resultLabel.getStyleClass().remove(GuiConstants.LABEL_SUCCESS_CLASS);
        resultLabel.getStyleClass().remove(GuiConstants.LABEL_ERROR_CLASS);
    }

    private void updatePathLabels(FileType fileType) {
        switch (fileType) {
            case UPPER_SUBTITLES:
                this.upperSubtitlesPathLabel.setText(getPathLabelText(upperSubtitlesFile));
                break;
            case LOWER_SUBTITLES:
                this.lowerSubtitlesPathLabel.setText(getPathLabelText(lowerSubtitlesFile));
                break;
            case MERGED_SUBTITLES:
                this.mergedSubtitlesPathLabel.setText(getPathLabelText(mergedSubtitlesFile));
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private static String getPathLabelText(File file) {
        if (file == null) {
            return "not selected";
        } else {
            return file.getAbsolutePath();
        }
    }

    private void showErrorsIfNecessary() {
        if (inputFilesTheSame()) {
            showErrors(
                    null,
                    "you can't use the same file twice",
                    null
            );

            return;
        }

        String upperSubtitlesFileErrorMessage = null;
        if (upperSubtitlesFile != null && !CollectionUtils.isEmpty(incorrectInputFiles)) {
            IncorrectInputFile incorrectInputFile = incorrectInputFiles.stream()
                    .filter(file -> Objects.equals(file.getFile(), upperSubtitlesFile))
                    .findAny().orElse(null);
            if (incorrectInputFile != null) {
                upperSubtitlesFileErrorMessage = getErrorText(upperSubtitlesFile, incorrectInputFile.getReason());
            }
        }

        String lowerSubtitlesFileErrorMessage = null;
        if (lowerSubtitlesFile != null && !CollectionUtils.isEmpty(incorrectInputFiles)) {
            IncorrectInputFile incorrectInputFile = incorrectInputFiles.stream()
                    .filter(file -> Objects.equals(file.getFile(), lowerSubtitlesFile))
                    .findAny().orElse(null);
            if (incorrectInputFile != null) {
                lowerSubtitlesFileErrorMessage = getErrorText(lowerSubtitlesFile, incorrectInputFile.getReason());
            }
        }

        String mergedSubtitlesFileErrorMessage = null;
        if (mergedSubtitlesFile != null && incorrectOutputFile != null) {
            if (Objects.equals(mergedSubtitlesFile, incorrectOutputFile.getFile())) {
                mergedSubtitlesFileErrorMessage = getErrorText(mergedSubtitlesFile, incorrectOutputFile.getReason());
            }
        }

        boolean atLeastOneError = !StringUtils.isBlank(upperSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(lowerSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(mergedSubtitlesFileErrorMessage);

        if (atLeastOneError) {
            showErrors(upperSubtitlesFileErrorMessage, lowerSubtitlesFileErrorMessage, mergedSubtitlesFileErrorMessage);
        }
    }

    private boolean inputFilesTheSame() {
        if (upperSubtitlesFile == null || lowerSubtitlesFile == null) {
            return false;
        }

        return Objects.equals(upperSubtitlesFile, lowerSubtitlesFile);
    }

    private void showErrors(
            String upperSubtitlesFileErrorMessage,
            String lowerSubtitlesFileErrorMessage,
            String mergedSubtitlesFileErrorMessage
    ) {
        if (!StringUtils.isBlank(upperSubtitlesFileErrorMessage)) {
            addButtonErrorClass(upperSubtitlesChooseButton);
        } else {
            removeButtonErrorClass(upperSubtitlesChooseButton);
        }

        if (!StringUtils.isBlank(lowerSubtitlesFileErrorMessage)) {
            addButtonErrorClass(lowerSubtitlesChooseButton);
        } else {
            removeButtonErrorClass(lowerSubtitlesChooseButton);
        }

        if (!StringUtils.isBlank(mergedSubtitlesFileErrorMessage)) {
            addButtonErrorClass(mergedSubtitlesChooseButton);
        } else {
            removeButtonErrorClass(mergedSubtitlesChooseButton);
        }

        boolean atLeastOneError = !StringUtils.isBlank(upperSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(lowerSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(mergedSubtitlesFileErrorMessage);
        if (!atLeastOneError) {
            throw new IllegalStateException();
        }

        StringBuilder combinedErrorsMessage = new StringBuilder("Can't merge subtitles:");

        if (!StringUtils.isBlank(upperSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\n").append("\u2022").append(" ").append(upperSubtitlesFileErrorMessage);
        }

        if (!StringUtils.isBlank(lowerSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\n").append("\u2022").append(" ").append(lowerSubtitlesFileErrorMessage);
        }

        if (!StringUtils.isBlank(mergedSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\n").append("\u2022").append(" ").append(mergedSubtitlesFileErrorMessage);
        }

        showErrorMessage(combinedErrorsMessage.toString());
    }

    private static void addButtonErrorClass(Button button) {
        if (!button.getStyleClass().contains(GuiConstants.BUTTON_ERROR_CLASS)) {
            button.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);
        }
    }

    private static String getErrorText(File file, IncorrectInputFileReason reason) {
        StringBuilder result = new StringBuilder("file ").append(file.getAbsolutePath()).append(" is incorrect: ");

        switch (reason) {
            case CAN_NOT_READ_FILE:
                result.append("can't read the file");
                break;
            case FILE_IS_TOO_BIG:
                result.append("file is too big (>")
                        .append(GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES).append(" megabytes)");
                break;
            case INCORRECT_SUBTITLES_FORMAT:
                result.append("incorrect subtitles format");
                break;
            default:
                throw new IllegalStateException();
        }

        return result.toString();
    }

    private static String getErrorText(File file, IncorrectOutputFileReason reason) {
        StringBuilder result = new StringBuilder("file ").append(file.getAbsolutePath()).append(" is incorrect: ");

        if (reason == IncorrectOutputFileReason.CAN_NOT_WRITE_TO_FILE) {
            result.append("can't write to this file");
        } else {
            throw new IllegalStateException();
        }

        return result.toString();
    }

    private void showErrorMessage(String text) {
        clearResult();
        resultLabel.getStyleClass().add(GuiConstants.LABEL_ERROR_CLASS);
        resultLabel.setText(text);
    }

    private void updateMergeButtonVisibility() {
        boolean disable = upperSubtitlesFile == null
                || lowerSubtitlesFile == null
                || mergedSubtitlesFile == null
                || inputFilesTheSame();
        mergeButton.setDisable(disable);
    }

    private void saveLastDirectoryInConfigIfNecessary(FileType fileType) {
        try {
            switch (fileType) {
                case UPPER_SUBTITLES:
                    if (upperSubtitlesFile != null) {
                        guiContext.getConfig().saveUpperSubtitlesLastDirectory(upperSubtitlesFile.getParent());
                    }
                    break;
                case LOWER_SUBTITLES:
                    if (lowerSubtitlesFile != null) {
                        guiContext.getConfig().saveLowerSubtitlesLastDirectory(lowerSubtitlesFile.getParent());
                    }
                    break;
                case MERGED_SUBTITLES:
                    if (mergedSubtitlesFile != null) {
                        guiContext.getConfig().saveMergedSubtitlesLastDirectory(mergedSubtitlesFile.getParent());
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        } catch (GuiPreferences.ConfigException e) {
            throw new IllegalStateException();
        }
    }

    @FXML
    private void lowerSubtitlesButtonClicked() {
        lowerSubtitlesFile = lowerSubtitlesChooser.showOpenDialog(stage);

        redrawAfterFileChosen(FileType.LOWER_SUBTITLES);
    }

    @FXML
    private void mergedSubtitlesButtonClicked() {
        mergedSubtitlesFile = mergedSubtitlesChooser.showSaveDialog(stage);
        if (mergedSubtitlesFile != null && !mergedSubtitlesFile.getAbsolutePath().endsWith(".srt")) {
            mergedSubtitlesFile = new File(mergedSubtitlesFile.getAbsolutePath() + ".srt");
        }

        redrawAfterFileChosen(FileType.MERGED_SUBTITLES);
    }

    @FXML
    private void mergeButtonClicked() {
        this.incorrectInputFiles = new ArrayList<>();
        this.incorrectOutputFile = null;
        removeErrorsAndResult();

        List<ParsedSubtitlesInfo> allParsedSubtitles = getAllParsedSubtitles();

        this.incorrectInputFiles = getIncorrectInputFiles(allParsedSubtitles);
        if (!CollectionUtils.isEmpty(incorrectInputFiles)) {
            showErrorsIfNecessary();
            return;
        }

        Subtitles result = Merger.mergeSubtitles(
                allParsedSubtitles.get(0).getSubtitles(),
                allParsedSubtitles.get(1).getSubtitles()
        );

        try {
            FileUtils.writeStringToFile(mergedSubtitlesFile, result.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            incorrectOutputFile = new IncorrectOutputFile(
                    mergedSubtitlesFile,
                    IncorrectOutputFileReason.CAN_NOT_WRITE_TO_FILE
            );
            showErrorsIfNecessary();
            return;
        }

        showSuccessMessage();
    }

    private void showSuccessMessage() {
        clearResult();
        resultLabel.getStyleClass().add(GuiConstants.LABEL_SUCCESS_CLASS);
        resultLabel.setText("Subtitles have been merged successfully!");
    }

    private List<ParsedSubtitlesInfo> getAllParsedSubtitles() {
        List<ParsedSubtitlesInfo> result = new ArrayList<>();

        if (upperSubtitlesFile == null || lowerSubtitlesFile == null || mergedSubtitlesFile == null) {
            log.error("at least one of the files is null, how is that possible?!");
            throw new IllegalStateException();
        }

        result.add(getParsedSubtitlesSingleFile(upperSubtitlesFile, "upper"));
        result.add(getParsedSubtitlesSingleFile(lowerSubtitlesFile, "lower"));

        return result;
    }

    private static ParsedSubtitlesInfo getParsedSubtitlesSingleFile(File file, String name) {
        if (file.length() / 1024 / 1024 > GuiConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES) {
            return new ParsedSubtitlesInfo(file, null, IncorrectInputFileReason.FILE_IS_TOO_BIG);
        }

        try {
            Subtitles subtitles = Parser.fromSubRipText(
                    FileUtils.readFileToString(file, StandardCharsets.UTF_8),
                    name,
                    null
            );

            return new ParsedSubtitlesInfo(file, subtitles, null);
        } catch (IOException e) {
            return new ParsedSubtitlesInfo(file, null, IncorrectInputFileReason.CAN_NOT_READ_FILE);
        } catch (Parser.IncorrectFormatException e) {
            return new ParsedSubtitlesInfo(file, null, IncorrectInputFileReason.INCORRECT_SUBTITLES_FORMAT);
        }
    }

    private List<IncorrectInputFile> getIncorrectInputFiles(List<ParsedSubtitlesInfo> allParsedSubtitles) {
        return allParsedSubtitles.stream()
                .filter(subtitles -> subtitles.getIncorrectFileReason() != null)
                .map(subtitles -> new IncorrectInputFile(subtitles.getFile(), subtitles.getIncorrectFileReason()))
                .collect(Collectors.toList());
    }

    private enum FileType {
        UPPER_SUBTITLES,
        LOWER_SUBTITLES,
        MERGED_SUBTITLES
    }

    @AllArgsConstructor
    @Getter
    private static class ParsedSubtitlesInfo {
        private File file;

        private Subtitles subtitles;

        private IncorrectInputFileReason incorrectFileReason;
    }

    @AllArgsConstructor
    @Getter
    private static class IncorrectInputFile {
        private File file;

        private IncorrectInputFileReason reason;
    }

    @AllArgsConstructor
    @Getter
    private static class IncorrectOutputFile {
        private File file;

        private IncorrectOutputFileReason reason;
    }

    private enum IncorrectInputFileReason {
        CAN_NOT_READ_FILE,
        INCORRECT_SUBTITLES_FORMAT,
        FILE_IS_TOO_BIG
    }

    private enum IncorrectOutputFileReason {
        CAN_NOT_WRITE_TO_FILE
    }
}
