package kirill.subtitlesmerger.gui.tabs.merge_single_files;

import javafx.fxml.FXML;
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
import java.util.Optional;
import java.util.stream.Collectors;

@CommonsLog
public class MergeSingleFilesTabController {
    private Stage stage;

    private GuiPreferences preferences;

    @Getter
    private MergeSingleFilesTabModel model;

    private File upperSubtitlesFile;

    private File lowerSubtitlesFile;

    private File mergedSubtitlesFile;

    private List<IncorrectInputFile> incorrectInputFiles;

    private IncorrectOutputFile incorrectOutputFile;

    public MergeSingleFilesTabController() {
        this.model = new MergeSingleFilesTabModel();
    }

    public void initialize(Stage stage, GuiContext context) {
        this.stage = stage;
        this.preferences = context.getPreferences();
    }

    @FXML
    private void upperSubtitlesButtonClicked() {
        upperSubtitlesFile = getFile(FileType.UPPER_SUBTITLES, stage, preferences).orElse(null);

        redrawAfterFileChosen(FileType.UPPER_SUBTITLES);
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

    private void redrawAfterFileChosen(FileType fileType) {
        clearErrorsAndResult();
        updatePathLabels(fileType);
        showErrorsIfNecessary();
        updateMergeButtonVisibility();
        saveLastDirectoryInConfigIfNecessary(fileType);
    }

    private void clearErrorsAndResult() {
        model.getUpperSubtitlesChooseButtonClass().clear();
        model.getLowerSubtitlesChooseButtonClass().clear();
        model.getMergedSubtitlesChooseButtonClass().clear();
        model.getResultClass().clear();
        model.setResultText("");
    }

    private void updatePathLabels(FileType fileType) {
        switch (fileType) {
            case UPPER_SUBTITLES:
                model.setUpperSubtitlesPath(getPathText(upperSubtitlesFile));
                break;
            case LOWER_SUBTITLES:
                model.setLowerSubtitlesPath(getPathText(lowerSubtitlesFile));
                break;
            case MERGED_SUBTITLES:
                model.setMergedSubtitlesPath(getPathText(mergedSubtitlesFile));
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private static String getPathText(File file) {
        if (file == null) {
            return MergeSingleFilesTabModel.FILE_NOT_CHOSEN_PATH;
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
        boolean atLeastOneError = !StringUtils.isBlank(upperSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(lowerSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(mergedSubtitlesFileErrorMessage);
        if (!atLeastOneError) {
            throw new IllegalStateException();
        }

        if (!StringUtils.isBlank(upperSubtitlesFileErrorMessage)) {
            model.getUpperSubtitlesChooseButtonClass().clear();
            model.getUpperSubtitlesChooseButtonClass().add(GuiConstants.BUTTON_ERROR_CLASS);
        }

        if (!StringUtils.isBlank(lowerSubtitlesFileErrorMessage)) {
            model.getLowerSubtitlesChooseButtonClass().clear();
            model.getLowerSubtitlesChooseButtonClass().add(GuiConstants.BUTTON_ERROR_CLASS);
        }

        if (!StringUtils.isBlank(mergedSubtitlesFileErrorMessage)) {
            model.getMergedSubtitlesChooseButtonClass().clear();
            model.getMergedSubtitlesChooseButtonClass().add(GuiConstants.BUTTON_ERROR_CLASS);
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

        model.setResultText(combinedErrorsMessage.toString());
        model.getResultClass().clear();
        model.getResultClass().add(GuiConstants.LABEL_ERROR_CLASS);
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

    private void updateMergeButtonVisibility() {
        boolean disable = upperSubtitlesFile == null
                || lowerSubtitlesFile == null
                || mergedSubtitlesFile == null
                || inputFilesTheSame();
        model.setMergeButtonDisable(disable);
    }

    private void saveLastDirectoryInConfigIfNecessary(FileType fileType) {
        try {
            switch (fileType) {
                case UPPER_SUBTITLES:
                    if (upperSubtitlesFile != null) {
                        preferences.saveUpperSubtitlesLastDirectory(upperSubtitlesFile.getParent());
                    }
                    break;
                case LOWER_SUBTITLES:
                    if (lowerSubtitlesFile != null) {
                        preferences.saveLowerSubtitlesLastDirectory(lowerSubtitlesFile.getParent());
                    }
                    break;
                case MERGED_SUBTITLES:
                    if (mergedSubtitlesFile != null) {
                        preferences.saveMergedSubtitlesLastDirectory(mergedSubtitlesFile.getParent());
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
        lowerSubtitlesFile = getFile(FileType.LOWER_SUBTITLES, stage, preferences).orElse(null);

        redrawAfterFileChosen(FileType.LOWER_SUBTITLES);
    }

    @FXML
    private void mergedSubtitlesButtonClicked() {
        mergedSubtitlesFile = getFile(FileType.MERGED_SUBTITLES, stage, preferences).orElse(null);

        if (mergedSubtitlesFile != null && !mergedSubtitlesFile.getAbsolutePath().endsWith(".srt")) {
            mergedSubtitlesFile = new File(mergedSubtitlesFile.getAbsolutePath() + ".srt");
        }

        redrawAfterFileChosen(FileType.MERGED_SUBTITLES);
    }

    @FXML
    private void mergeButtonClicked() {
        clearErrorsAndResult();

        this.incorrectInputFiles = new ArrayList<>();
        this.incorrectOutputFile = null;

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

        model.setResultText("Subtitles have been merged successfully!");
        model.getResultClass().clear();
        model.getResultClass().add(GuiConstants.LABEL_SUCCESS_CLASS);
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