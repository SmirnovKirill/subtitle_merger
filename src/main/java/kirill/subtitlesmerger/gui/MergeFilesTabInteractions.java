package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
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
        updateInitialDirectories();

        tab.getUpperSubtitlesFileChooseButton().setOnAction(this::upperSubtitlesFileButtonClicked);
        tab.getLowerSubtitlesFileChooseButton().setOnAction(this::lowerSubtitlesFileButtonClicked);
        tab.getMergedSubtitlesFileChooseButton().setOnAction(this::mergedSubtitlesFileButtonClicked);
        tab.getMergeButton().setOnAction(this::mergeButtonClicked);
    }

    private void updateInitialDirectories() {
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
        tab.getUpperSubtitlesPathLabel().setText(getPathLabelText(upperSubtitlesFile));

        if (upperSubtitlesFile != null) {
            try {
                config.saveUpperSubtitlesLastDirectory(upperSubtitlesFile.getParent());
            } catch (Config.ConfigException e) {
                throw new IllegalStateException();
            }
        }

        updateInitialDirectories();
        updateButtonErrorClass(tab.getUpperSubtitlesFileChooseButton(), upperSubtitlesFile, incorrectFiles);
        updateMergeButtonVisibility();
    }

    private static void updateButtonErrorClass(Button button, File file, Set<File> incorrectFiles) {
        if (file != null && !CollectionUtils.isEmpty(incorrectFiles) && incorrectFiles.contains(file)) {
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
                upperSubtitlesFile == null || lowerSubtitlesFile == null || mergedSubtitlesFile == null
        );
    }

    private void lowerSubtitlesFileButtonClicked(ActionEvent event) {
        lowerSubtitlesFile = tab.getLowerSubtitlesFileChooser().showOpenDialog(tab.getStage());
        tab.getLowerSubtitlesPathLabel().setText(getPathLabelText(lowerSubtitlesFile));

        if (lowerSubtitlesFile != null) {
            try {
                config.saveLowerSubtitlesLastDirectory(lowerSubtitlesFile.getParent());
            } catch (Config.ConfigException e) {
                throw new IllegalStateException();
            }
        }

        updateInitialDirectories();
        updateButtonErrorClass(tab.getLowerSubtitlesFileChooseButton(), lowerSubtitlesFile, incorrectFiles);
        updateMergeButtonVisibility();
    }

    private void mergedSubtitlesFileButtonClicked(ActionEvent event) {
        mergedSubtitlesFile = tab.getMergedSubtitlesFileChooser().showSaveDialog(tab.getStage());
        tab.getMergedSubtitlesPathLabel().setText(getPathLabelText(mergedSubtitlesFile));

        if (mergedSubtitlesFile != null) {
            try {
                config.saveMergedSubtitlesLastDirectory(mergedSubtitlesFile.getParent());
            } catch (Config.ConfigException e) {
                throw new IllegalStateException();
            }
        }

        updateInitialDirectories();
        updateButtonErrorClass(tab.getMergedSubtitlesFileChooseButton(), mergedSubtitlesFile, incorrectFiles);
        updateMergeButtonVisibility();
    }

    private void mergeButtonClicked(ActionEvent event) {
        incorrectFiles = new HashSet<>(Arrays.asList(lowerSubtitlesFile, upperSubtitlesFile, mergedSubtitlesFile));

        updateButtonErrorClass(tab.getUpperSubtitlesFileChooseButton(), upperSubtitlesFile, incorrectFiles);
        updateButtonErrorClass(tab.getLowerSubtitlesFileChooseButton(), lowerSubtitlesFile, incorrectFiles);
        updateButtonErrorClass(tab.getMergedSubtitlesFileChooseButton(), mergedSubtitlesFile, incorrectFiles);
    }
}
