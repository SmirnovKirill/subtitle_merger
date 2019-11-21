package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import kirill.subtitlesmerger.logic.data.Config;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class MergeFilesTabInteractions {
    private MergeFilesTab tab;

    private Config config;

    private File upperSubtitlesFile;

    private File lowerSubtitlesFile;

    private File mergedSubtitlesFile;

    private Set<File> incorrectFiles;

    MergeFilesTabInteractions(MergeFilesTab tab, Config config) {
        this.tab = tab;
        this.config = config;
    }

    void addCallbacks() {
        updateFileChooserInitialDirectories();

        tab.getUpperSubtitlesFileChooseButton().setOnAction(this::upperSubtitlesFileButtonClicked);
        tab.getLowerSubtitlesFileChooseButton().setOnAction(this::lowerSubtitlesFileButtonClicked);
        tab.getMergedSubtitlesFileChooseButton().setOnAction(this::mergedSubtitlesFileButtonClicked);
        tab.getMergeButton().setOnAction(this::mergeButtonClicked);
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

        if (upperSubtitlesDirectory != null) {
            tab.getUpperSubtitlesFileChooser().setInitialDirectory(upperSubtitlesDirectory);
        }
        if (lowerSubtitlesDirectory != null) {
            tab.getLowerSubtitlesFileChooser().setInitialDirectory(lowerSubtitlesDirectory);
        }
        if (mergedSubtitlesDirectory != null) {
            tab.getMergedSubtitlesFileChooser().setInitialDirectory(mergedSubtitlesDirectory);
        }
    }

    private void upperSubtitlesFileButtonClicked(ActionEvent event) {
        upperSubtitlesFile = tab.getUpperSubtitlesFileChooser().showOpenDialog(tab.getStage());

        updatePathLabelText(MergeFilesTab.FileType.UPPER_SUBTITLES);
        saveLastDirectoryInConfigIfNecessary(MergeFilesTab.FileType.UPPER_SUBTITLES);
        updateFileChooserInitialDirectories();
        updateButtonErrorClass(MergeFilesTab.FileType.UPPER_SUBTITLES);
        updateMergeButtonVisibility();
    }

    private void updatePathLabelText(MergeFilesTab.FileType fileType) {
        Label label;
        File file;
        switch (fileType) {
            case UPPER_SUBTITLES:
                label = tab.getUpperSubtitlesPathLabel();
                file = upperSubtitlesFile;
                break;
            case LOWER_SUBTITLES:
                label = tab.getLowerSubtitlesPathLabel();
                file = lowerSubtitlesFile;
                break;
            case MERGED_SUBTITLES:
                label = tab.getMergedSubtitlesPathLabel();
                file = mergedSubtitlesFile;
                break;
            default:
                throw new IllegalStateException();
        }

        if (file == null) {
            label.setText("not selected");
        } else {
            label.setText(file.getAbsolutePath());
        }
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

    private void updateButtonErrorClass(MergeFilesTab.FileType fileType) {
        Button button;
        File file;
        switch (fileType) {
            case UPPER_SUBTITLES:
                button = tab.getUpperSubtitlesFileChooseButton();
                file = upperSubtitlesFile;
                break;
            case LOWER_SUBTITLES:
                button = tab.getLowerSubtitlesFileChooseButton();
                file = lowerSubtitlesFile;
                break;
            case MERGED_SUBTITLES:
                button = tab.getMergedSubtitlesFileChooseButton();
                file = mergedSubtitlesFile;
                break;
            default:
                throw new IllegalStateException();
        }

        if (file != null && !CollectionUtils.isEmpty(incorrectFiles) && incorrectFiles.contains(file)) {
            if (!button.getStyleClass().contains(MergeFilesTab.BUTTON_ERROR_CLASS)) {
                button.getStyleClass().add(MergeFilesTab.BUTTON_ERROR_CLASS);
            }
        } else {
            button.getStyleClass().remove(MergeFilesTab.BUTTON_ERROR_CLASS);
        }
    }

    private void updateMergeButtonVisibility() {
        tab.getMergeButton().setDisable(
                upperSubtitlesFile == null || lowerSubtitlesFile == null || mergedSubtitlesFile == null
        );
    }

    private void lowerSubtitlesFileButtonClicked(ActionEvent event) {
        lowerSubtitlesFile = tab.getLowerSubtitlesFileChooser().showOpenDialog(tab.getStage());

        updatePathLabelText(MergeFilesTab.FileType.LOWER_SUBTITLES);
        saveLastDirectoryInConfigIfNecessary(MergeFilesTab.FileType.LOWER_SUBTITLES);
        updateFileChooserInitialDirectories();
        updateButtonErrorClass(MergeFilesTab.FileType.LOWER_SUBTITLES);
        updateMergeButtonVisibility();
    }

    private void mergedSubtitlesFileButtonClicked(ActionEvent event) {
        mergedSubtitlesFile = tab.getMergedSubtitlesFileChooser().showSaveDialog(tab.getStage());

        updatePathLabelText(MergeFilesTab.FileType.MERGED_SUBTITLES);
        saveLastDirectoryInConfigIfNecessary(MergeFilesTab.FileType.MERGED_SUBTITLES);
        updateFileChooserInitialDirectories();
        updateButtonErrorClass(MergeFilesTab.FileType.MERGED_SUBTITLES);
        updateMergeButtonVisibility();
    }

    private void mergeButtonClicked(ActionEvent event) {
        incorrectFiles = new HashSet<>(Arrays.asList(lowerSubtitlesFile, upperSubtitlesFile, mergedSubtitlesFile));

        updateButtonErrorClass(MergeFilesTab.FileType.UPPER_SUBTITLES);
        updateButtonErrorClass(MergeFilesTab.FileType.LOWER_SUBTITLES);
        updateButtonErrorClass(MergeFilesTab.FileType.MERGED_SUBTITLES);
    }
}
