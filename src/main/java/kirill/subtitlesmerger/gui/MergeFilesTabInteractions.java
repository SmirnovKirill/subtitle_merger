package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;

import java.io.File;

public class MergeFilesTabInteractions {
    private MergeFilesTab tab;

    private File upperSubtitlesFile;

    private File lowerSubtitlesFile;

    private File resultFile;

    public MergeFilesTabInteractions(MergeFilesTab tab) {
        this.tab = tab;
    }

    public void addCallbacks() {
        tab.getUpperSubtitlesFileChooseButton().setOnAction(this::upperSubtitlesFileButtonClicked);
        tab.getLowerSubtitlesFileChooseButton().setOnAction(this::lowerSubtitlesFileButtonClicked);
        tab.getResultFileChooseButton().setOnAction(this::resultFileButtonClicked);
        tab.getMergeButton().setOnAction(this::mergeButtonClicked);
    }

    private void upperSubtitlesFileButtonClicked(ActionEvent event) {
        upperSubtitlesFile = tab.getUpperSubtitlesFileChooser().showOpenDialog(tab.getStage());
        tab.getUpperSubtitlesPathLabel().setText(getPathLabelText(upperSubtitlesFile));

        updateMergeButtonVisibility();
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

        updateMergeButtonVisibility();
    }

    private void resultFileButtonClicked(ActionEvent event) {
        resultFile = tab.getResultFileChooser().showSaveDialog(tab.getStage());
        tab.getResultPathLabel().setText(getPathLabelText(resultFile));

        updateMergeButtonVisibility();
    }

    private void mergeButtonClicked(ActionEvent event) {

    }
}
