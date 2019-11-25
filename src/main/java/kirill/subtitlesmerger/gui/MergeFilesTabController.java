package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import kirill.subtitlesmerger.logic.Constants;
import kirill.subtitlesmerger.logic.Merger;
import kirill.subtitlesmerger.logic.Parser;
import kirill.subtitlesmerger.logic.data.Config;
import kirill.subtitlesmerger.logic.data.Subtitles;
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
class MergeFilesTabController {
    private MergeFilesTab tab;

    private Config config;

    private File upperSubtitlesFile;

    private File lowerSubtitlesFile;

    private File mergedSubtitlesFile;

    private List<IncorrectInputFile> incorrectInputFiles;

    private IncorrectOutputFile incorrectOutputFile;

    MergeFilesTabController(MergeFilesTab tab, Config config) {
        this.tab = tab;
        this.config = config;
    }

    void initialize() {
        updateFileChooserInitialDirectories();

        tab.setUpperSubtitlesFileChooseButtonHandler(this::upperSubtitlesFileButtonClicked);
        tab.setLowerSubtitlesFileChooseButtonHandler(this::lowerSubtitlesFileButtonClicked);
        tab.setMergedSubtitlesFileChooseButtonHandler(this::mergedSubtitlesFileButtonClicked);
        tab.setMergeButtonHandler(this::mergeButtonClicked);
    }

    private void updateFileChooserInitialDirectories() {
        File upperSubtitlesDirectory = ObjectUtils.firstNonNull(
                config.getUpperSubtitlesLastDirectory(),
                config.getLowerSubtitlesLastDirectory(),
                config.getMergedSubtitlesLastDirectory()
        );

        File lowerSubtitlesDirectory = ObjectUtils.firstNonNull(
                config.getLowerSubtitlesLastDirectory(),
                config.getUpperSubtitlesLastDirectory(),
                config.getMergedSubtitlesLastDirectory()
        );

        File mergedSubtitlesDirectory = ObjectUtils.firstNonNull(
                config.getMergedSubtitlesLastDirectory(),
                config.getUpperSubtitlesLastDirectory(),
                config.getLowerSubtitlesLastDirectory()
        );

        tab.updateFileChooserInitialDirectories(
                upperSubtitlesDirectory,
                lowerSubtitlesDirectory,
                mergedSubtitlesDirectory
        );
    }

    private void upperSubtitlesFileButtonClicked(ActionEvent event) {
        upperSubtitlesFile = tab.getSelectedUpperSubtitlesFile().orElse(null);

        redrawAfterFileChosen(MergeFilesTab.FileType.UPPER_SUBTITLES);

        saveLastDirectoryInConfigIfNecessary(MergeFilesTab.FileType.UPPER_SUBTITLES);
        updateFileChooserInitialDirectories();
    }

    private void redrawAfterFileChosen(MergeFilesTab.FileType fileType) {
        tab.removeErrorsAndResult();
        updatePathLabels(fileType);
        showErrorsIfNecessary();
        updateMergeButtonVisibility();
    }

    private void updatePathLabels(MergeFilesTab.FileType fileType) {
        switch (fileType) {
            case UPPER_SUBTITLES:
                tab.setUpperSubtitlesPathLabel(getPathLabelText(upperSubtitlesFile));
                break;
            case LOWER_SUBTITLES:
                tab.setLowerSubtitlesPathLabel(getPathLabelText(lowerSubtitlesFile));
                break;
            case MERGED_SUBTITLES:
                tab.setMergedSubtitlesPathLabel(getPathLabelText(mergedSubtitlesFile));
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
            tab.showErrors(
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
            tab.showErrors(
                    upperSubtitlesFileErrorMessage,
                    lowerSubtitlesFileErrorMessage,
                    mergedSubtitlesFileErrorMessage
            );
        }
    }
    private boolean inputFilesTheSame() {
        if (upperSubtitlesFile == null || lowerSubtitlesFile == null) {
            return false;
        }

        return Objects.equals(upperSubtitlesFile, lowerSubtitlesFile);
    }

    private static String getErrorText(File file, IncorrectInputFileReason reason) {
        StringBuilder result = new StringBuilder("file ").append(file.getAbsolutePath()).append(" is incorrect: ");

        switch (reason) {
            case CAN_NOT_READ_FILE:
                result.append("can't read the file");
                break;
            case FILE_IS_TOO_BIG:
                result.append("file is too big (>")
                        .append(Constants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES).append(" megabytes)");
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
        tab.setMergeButtonDisable(disable);
    }

    private void saveLastDirectoryInConfigIfNecessary(MergeFilesTab.FileType fileType) {
        try {
            switch (fileType) {
                case UPPER_SUBTITLES:
                    if (upperSubtitlesFile != null) {
                        config.saveUpperSubtitlesLastDirectory(upperSubtitlesFile.getParent());
                    }
                    break;
                case LOWER_SUBTITLES:
                    if (lowerSubtitlesFile != null) {
                        config.saveLowerSubtitlesLastDirectory(lowerSubtitlesFile.getParent());
                    }
                    break;
                case MERGED_SUBTITLES:
                    if (mergedSubtitlesFile != null) {
                        config.saveMergedSubtitlesLastDirectory(mergedSubtitlesFile.getParent());
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        } catch (Config.ConfigException e) {
            throw new IllegalStateException();
        }
    }

    private void lowerSubtitlesFileButtonClicked(ActionEvent event) {
        lowerSubtitlesFile = tab.getSelectedLowerSubtitlesFile().orElse(null);

        redrawAfterFileChosen(MergeFilesTab.FileType.LOWER_SUBTITLES);

        saveLastDirectoryInConfigIfNecessary(MergeFilesTab.FileType.LOWER_SUBTITLES);
        updateFileChooserInitialDirectories();
    }

    private void mergedSubtitlesFileButtonClicked(ActionEvent event) {
        mergedSubtitlesFile = tab.getSelectedMergedSubtitlesFile().orElse(null);
        if (mergedSubtitlesFile != null && !mergedSubtitlesFile.getAbsolutePath().endsWith(".srt")) {
            mergedSubtitlesFile = new File(mergedSubtitlesFile.getAbsolutePath() + ".srt");
        }

        redrawAfterFileChosen(MergeFilesTab.FileType.MERGED_SUBTITLES);

        saveLastDirectoryInConfigIfNecessary(MergeFilesTab.FileType.MERGED_SUBTITLES);
        updateFileChooserInitialDirectories();
    }

    private void mergeButtonClicked(ActionEvent event) {
        this.incorrectInputFiles = new ArrayList<>();
        this.incorrectOutputFile = null;
        tab.removeErrorsAndResult();

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

        tab.showSuccessMessage();
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
        if (file.length() / 1024 / 1024 > Constants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES) {
            return new ParsedSubtitlesInfo(file, null, IncorrectInputFileReason.FILE_IS_TOO_BIG);
        }

        try {
            Subtitles subtitles = Parser.parseSubtitles(
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
