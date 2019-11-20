package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class MergeFilesTabInteractions {
    private MergeFilesTab tab;

    private File upperSubtitlesFile;

    private File lowerSubtitlesFile;

    private File resultFile;

    private Set<File> incorrectFiles;

    MergeFilesTabInteractions(MergeFilesTab tab) {
        this.tab = tab;
    }

    void addCallbacks() {
        tab.getUpperSubtitlesFileChooseButton().setOnAction(this::upperSubtitlesFileButtonClicked);
        tab.getLowerSubtitlesFileChooseButton().setOnAction(this::lowerSubtitlesFileButtonClicked);
        tab.getResultFileChooseButton().setOnAction(this::resultFileButtonClicked);
        tab.getMergeButton().setOnAction(this::mergeButtonClicked);
    }

    private void upperSubtitlesFileButtonClicked(ActionEvent event) {
        upperSubtitlesFile = tab.getUpperSubtitlesFileChooser().showOpenDialog(tab.getStage());
        tab.getUpperSubtitlesPathLabel().setText(getPathLabelText(upperSubtitlesFile));

        updateButtonErrorClass(tab.getUpperSubtitlesFileChooseButton(), upperSubtitlesFile, incorrectFiles);
        updateMergeButtonVisibility();
    }

    private static void updateButtonErrorClass(Button button, File file, Set<File> incorrectFiles) {
        if (!CollectionUtils.isEmpty(incorrectFiles) && incorrectFiles.contains(file)) {
            if (!button.getStyleClass().contains(MergeFilesTab.BUTTON_ERROR_CLASS)) {
                button.getStyleClass().add(MergeFilesTab.BUTTON_ERROR_CLASS);
            }
        } else {
            button.getStyleClass().remove(MergeFilesTab.BUTTON_ERROR_CLASS);
        }
    }

    private static String getPathLabelText(File file) {
        if (file == null) {
            return "not selected";
        }

        return file.getAbsolutePath();
    }

    private void updateMergeButtonVisibility() {
        tab.getMergeButton().setDisable(
                upperSubtitlesFile == null || lowerSubtitlesFile == null || resultFile == null
        );
    }

    private void lowerSubtitlesFileButtonClicked(ActionEvent event) {
        lowerSubtitlesFile = tab.getLowerSubtitlesFileChooser().showOpenDialog(tab.getStage());
        tab.getLowerSubtitlesPathLabel().setText(getPathLabelText(lowerSubtitlesFile));

        updateButtonErrorClass(tab.getLowerSubtitlesFileChooseButton(), lowerSubtitlesFile, incorrectFiles);
        updateMergeButtonVisibility();
    }

    private void resultFileButtonClicked(ActionEvent event) {
        resultFile = tab.getResultFileChooser().showSaveDialog(tab.getStage());
        tab.getResultPathLabel().setText(getPathLabelText(resultFile));

        updateButtonErrorClass(tab.getResultFileChooseButton(), resultFile, incorrectFiles);
        updateMergeButtonVisibility();
    }

    private void mergeButtonClicked(ActionEvent event) {
        incorrectFiles = new HashSet<>(Arrays.asList(lowerSubtitlesFile, upperSubtitlesFile, resultFile));

        updateButtonErrorClass(tab.getUpperSubtitlesFileChooseButton(), upperSubtitlesFile, incorrectFiles);
        updateButtonErrorClass(tab.getLowerSubtitlesFileChooseButton(), lowerSubtitlesFile, incorrectFiles);
        updateButtonErrorClass(tab.getResultFileChooseButton(), resultFile, incorrectFiles);
    }
}
