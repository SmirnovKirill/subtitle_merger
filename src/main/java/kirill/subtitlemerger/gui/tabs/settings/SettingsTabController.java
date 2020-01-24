package kirill.subtitlemerger.gui.tabs.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

@CommonsLog
public class SettingsTabController {
    public static final String MERGE_MODE_ORIGINAL_VIDEOS = "Original videos";

    public static final String MERGE_MODE_VIDEO_COPIES = "Copies of the videos";

    public static final String MERGE_MODE_SEPARATE_SUBTITLE_FILES = "Separate subtitle files";

    private static final String UPDATE_FFPROBE_BUTTON_TEXT = "update path to ffprobe";

    private static final String UPDATE_FFMPEG_BUTTON_TEXT = "update path to ffmpeg";

    private static final LanguageCodeStringConverter LANGUAGE_CODE_STRING_CONVERTER = new LanguageCodeStringConverter();

    private Stage stage;

    private GuiContext context;

    private GuiSettings settings;

    @FXML
    private TextField ffprobeField;

    @FXML
    private Button ffprobeSetButton;

    @FXML
    private TextField ffmpegField;

    @FXML
    private Button ffmpegSetButton;

    @FXML
    //todo make editable with drop-down
    private ComboBox<LanguageAlpha3Code> upperLanguageComboBox;

    @FXML
    //todo make editable with drop-down
    private ComboBox<LanguageAlpha3Code> lowerLanguageComboBox;

    @FXML
    private Button swapLanguagesButton;

    @FXML
    private ToggleGroup mergeModeToggleGroup;

    @FXML
    private CheckBox markMergedStreamAsDefaultCheckBox;

    private BooleanProperty markStreamCheckBoxVisible;

    @FXML
    private Label resultLabel;

    public boolean isMarkStreamCheckBoxVisible() {
        return markStreamCheckBoxVisible.get();
    }

    public BooleanProperty markStreamCheckBoxVisibleProperty() {
        return markStreamCheckBoxVisible;
    }

    public void setMarkStreamCheckBoxVisible(boolean markStreamCheckBoxVisible) {
        this.markStreamCheckBoxVisible.set(markStreamCheckBoxVisible);
    }

    public SettingsTabController() {
        this.markStreamCheckBoxVisible = new SimpleBooleanProperty(false);
    }

    public void initialize(Stage stage, GuiContext context) {
        this.stage = stage;
        this.context = context;
        this.settings = context.getSettings();

        setInitialValues();
        mergeModeToggleGroup.selectedToggleProperty().addListener(this::mergeModeChanged);
    }

    private void setInitialValues() {
        setFfprobeInitialValue();
        setFfmpegInitialValue();
        setUpperSubtitlesInitialValue();
        setSwapLanguagesButtonVisibility();
        setLowerSubtitlesInitialValue();
        setMergeModeInitialValue();
        setMarkCheckBoxVisibility();

        markMergedStreamAsDefaultCheckBox.setSelected(settings.isMarkMergedStreamAsDefault());
    }

    private void setFfprobeInitialValue() {
        File ffprobeFile = settings.getFfprobeFile();

        if (ffprobeFile != null) {
            ffprobeField.setText(ffprobeFile.getAbsolutePath());
            ffprobeSetButton.setText(UPDATE_FFPROBE_BUTTON_TEXT);
        } else {
            ffprobeSetButton.setText("choose path to ffprobe");
        }
    }

    private void setFfmpegInitialValue() {
        File ffmpegFile = settings.getFfmpegFile();
        if (ffmpegFile != null) {
            ffmpegField.setText(ffmpegFile.getAbsolutePath());
            ffmpegSetButton.setText(UPDATE_FFMPEG_BUTTON_TEXT);
        } else {
            ffmpegSetButton.setText("choose path to ffmpeg");
        }
    }

    private void setUpperSubtitlesInitialValue() {
        upperLanguageComboBox.getItems().setAll(LogicConstants.ALLOWED_LANGUAGE_CODES);
        upperLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);
        LanguageAlpha3Code upperLanguage = settings.getUpperLanguage();
        if (upperLanguage != null) {
            upperLanguageComboBox.getSelectionModel().select(upperLanguage);
        }
    }

    private void setSwapLanguagesButtonVisibility() {
        boolean swapButtonDisable = settings.getUpperLanguage() == null || settings.getLowerLanguage() == null;

        swapLanguagesButton.setDisable(swapButtonDisable);
    }

    private void setLowerSubtitlesInitialValue() {
        lowerLanguageComboBox.getItems().setAll(LogicConstants.ALLOWED_LANGUAGE_CODES);
        lowerLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);
        LanguageAlpha3Code lowerLanguage = settings.getLowerLanguage();
        if (lowerLanguage != null) {
            lowerLanguageComboBox.getSelectionModel().select(lowerLanguage);
        }
    }

    private void setMergeModeInitialValue() {
        GuiSettings.MergeMode mergeMode = context.getSettings().getMergeMode();
        if (mergeMode == null) {
            return;
        }

        String value;
        switch (mergeMode) {
            case ORIGINAL_VIDEOS:
                value = MERGE_MODE_ORIGINAL_VIDEOS;
                break;
            case VIDEO_COPIES:
                value = MERGE_MODE_VIDEO_COPIES;
                break;
            case SEPARATE_SUBTITLE_FILES:
                value = MERGE_MODE_SEPARATE_SUBTITLE_FILES;
                break;
            default:
                throw new IllegalStateException();
        }

        for (Toggle toggle : mergeModeToggleGroup.getToggles()) {
            RadioButton radioButton = (RadioButton) toggle;
            if (value.equals(radioButton.getText())) {
                toggle.setSelected(true);
                return;
            }
        }

        throw new IllegalStateException();
    }

    private void setMarkCheckBoxVisibility() {
        GuiSettings.MergeMode mergeMode = context.getSettings().getMergeMode();
        setMarkStreamCheckBoxVisible(
                mergeMode == GuiSettings.MergeMode.ORIGINAL_VIDEOS || mergeMode == GuiSettings.MergeMode.VIDEO_COPIES
        );
    }

    private void mergeModeChanged(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
        RadioButton radioButton = (RadioButton) newValue;

        GuiSettings.MergeMode mergeMode;
        switch (radioButton.getText()) {
            case MERGE_MODE_ORIGINAL_VIDEOS:
                mergeMode = GuiSettings.MergeMode.ORIGINAL_VIDEOS;
                break;
            case MERGE_MODE_VIDEO_COPIES:
                mergeMode = GuiSettings.MergeMode.VIDEO_COPIES;
                break;
            case MERGE_MODE_SEPARATE_SUBTITLE_FILES:
                mergeMode = GuiSettings.MergeMode.SEPARATE_SUBTITLE_FILES;
                break;
            default:
                throw new IllegalStateException();
        }

        try {
            context.getSettings().saveMergeMode(mergeMode.toString());
            setMarkCheckBoxVisibility();

            showSuccessMessage("merge mode has been saved successfully");
        } catch (GuiSettings.ConfigException e) {
            log.error("merge mode hasn't been saved: " + ExceptionUtils.getStackTrace(e));

            showErrorMessage("something bad has happened, merge mode hasn't been saved");
        }
    }

    @FXML
    private void ffprobeFileButtonClicked() {
        File ffprobeFile = getFfprobeFile(stage, settings).orElse(null);
        if (ffprobeFile == null) {
            clearResult();
            return;
        }

        if (Objects.equals(ffprobeFile, settings.getFfprobeFile())) {
            showSuccessMessage("path to ffprobe has stayed the same");
            return;
        }

        boolean hadValueBefore = settings.getFfmpegFile() != null;

        try {
            settings.saveFfprobeFile(ffprobeFile.getAbsolutePath());
            context.setFfprobe(new Ffprobe(settings.getFfprobeFile()));
            ffprobeField.setText(ffprobeFile.getAbsolutePath());
            ffprobeSetButton.setText(UPDATE_FFPROBE_BUTTON_TEXT);

            if (hadValueBefore) {
                showSuccessMessage("path to ffprobe has been updated successfully");
            } else {
                showSuccessMessage("path to ffprobe has been saved successfully");
            }
        } catch (GuiSettings.ConfigException | FfmpegException e) {
            showErrorMessage("incorrect path to ffprobe");
        }
    }

    private static Optional<File> getFfprobeFile(Stage stage, GuiSettings settings) {
        FileChooser fileChooser = new FileChooser();

        String title;
        File initialDirectory;
        if (settings.getFfprobeFile() != null) {
            title = "update path to ffprobe";
            initialDirectory = settings.getFfprobeFile().getParentFile();
        } else {
            title = "choose path to ffprobe";
            initialDirectory = settings.getFfmpegFile() != null ? settings.getFfmpegFile().getParentFile() : null;
        }

        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(initialDirectory);

        return Optional.ofNullable(fileChooser.showOpenDialog(stage));
    }

    private void clearResult() {
        resultLabel.setText("");
        resultLabel.getStyleClass().removeAll(GuiConstants.LABEL_SUCCESS_CLASS, GuiConstants.LABEL_ERROR_CLASS);
    }

    private void showSuccessMessage(String text) {
        resultLabel.setText(text);

        resultLabel.getStyleClass().remove(GuiConstants.LABEL_ERROR_CLASS);
        if (!resultLabel.getStyleClass().contains(GuiConstants.LABEL_SUCCESS_CLASS)) {
            resultLabel.getStyleClass().add(GuiConstants.LABEL_SUCCESS_CLASS);
        }
    }

    private void showErrorMessage(String text) {
        resultLabel.setText(text);

        resultLabel.getStyleClass().remove(GuiConstants.LABEL_SUCCESS_CLASS);
        if (!resultLabel.getStyleClass().contains(GuiConstants.LABEL_ERROR_CLASS)) {
            resultLabel.getStyleClass().add(GuiConstants.LABEL_ERROR_CLASS);
        }
    }

    @FXML
    private void ffmpegFileButtonClicked() {
        File ffmpegFile = getFfmpegFile(stage, settings).orElse(null);
        if (ffmpegFile == null) {
            clearResult();
            return;
        }

        if (Objects.equals(ffmpegFile, settings.getFfmpegFile())) {
            showSuccessMessage("path to ffmpeg has stayed the same");
            return;
        }

        boolean hadValueBefore = settings.getFfmpegFile() != null;

        try {
            settings.saveFfmpegFile(ffmpegFile.getAbsolutePath());
            context.setFfmpeg(new Ffmpeg(settings.getFfmpegFile()));
            ffmpegField.setText(ffmpegFile.getAbsolutePath());
            ffmpegSetButton.setText(UPDATE_FFMPEG_BUTTON_TEXT);

            if (hadValueBefore) {
                showSuccessMessage("path to ffmpeg has been updated successfully");
            } else {
                showSuccessMessage("path to ffmpeg has been saved successfully");
            }
        } catch (GuiSettings.ConfigException | FfmpegException e) {
            showErrorMessage("incorrect path to ffmpeg");
        }
    }

    private static Optional<File> getFfmpegFile(Stage stage, GuiSettings settings) {
        FileChooser fileChooser = new FileChooser();

        String title;
        File initialDirectory;
        if (settings.getFfmpegFile() != null) {
            title = "update path to ffmpeg";
            initialDirectory = settings.getFfmpegFile().getParentFile();
        } else {
            title = "choose path to ffmpeg";
            initialDirectory = settings.getFfprobeFile() != null ? settings.getFfprobeFile().getParentFile() : null;
        }

        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(initialDirectory);

        return Optional.ofNullable(fileChooser.showOpenDialog(stage));
    }

    @FXML
    private void upperLanguageChanged() {
        LanguageAlpha3Code value = upperLanguageComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(value, settings.getUpperLanguage())) {
            return;
        }

        if (Objects.equals(value, settings.getLowerLanguage())) {
            showErrorMessage("languages have to be different, please choose another one");
            return;
        }

        boolean hadValueBefore = settings.getUpperLanguage() != null;

        try {
            settings.saveUpperLanguage(value.toString());
            setSwapLanguagesButtonVisibility();

            if (hadValueBefore) {
                showSuccessMessage("language for upper subtitles has been updated successfully");
            } else {
                showSuccessMessage("language for upper subtitles has been saved successfully");
            }
        } catch (GuiSettings.ConfigException e) {
            log.error("language for upper subtitles has not been saved: " + ExceptionUtils.getStackTrace(e));

            showErrorMessage("something bad has happened, language hasn't been saved");
        }
    }

    @FXML
    private void swapLanguagesButtonClicked() {
        LanguageAlpha3Code oldUpperLanguage = settings.getUpperLanguage();
        LanguageAlpha3Code oldLowerLanguage = settings.getLowerLanguage();

        try {
            settings.saveUpperLanguage(oldLowerLanguage.toString());
            upperLanguageComboBox.getSelectionModel().select(oldLowerLanguage);

            settings.saveLowerLanguage(oldUpperLanguage.toString());
            lowerLanguageComboBox.getSelectionModel().select(oldUpperLanguage);

            showSuccessMessage("languages have been swapped successfully");
        } catch (GuiSettings.ConfigException e) {
            log.error("languages haven't been swapped: " + ExceptionUtils.getStackTrace(e));

            showErrorMessage("something bad has happened, languages haven't been swapped");
        }
    }

    @FXML
    private void lowerLanguageChanged() {
        LanguageAlpha3Code value = lowerLanguageComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(value, settings.getLowerLanguage())) {
            return;
        }

        if (Objects.equals(value, settings.getUpperLanguage())) {
            showErrorMessage("languages have to be different, please choose another one");
            return;
        }

        boolean hadValueBefore = settings.getLowerLanguage() != null;

        try {
            settings.saveLowerLanguage(value.toString());
            setSwapLanguagesButtonVisibility();

            if (hadValueBefore) {
                showSuccessMessage("language for lower subtitles has been updated successfully");
            } else {
                showSuccessMessage("language for lower subtitles has been saved successfully");
            }
        } catch (GuiSettings.ConfigException e) {
            log.error("language for lower subtitles has not been saved: " + ExceptionUtils.getStackTrace(e));

            showErrorMessage("something bad has happened, language hasn't been saved");
        }
    }

    private static class LanguageCodeStringConverter extends StringConverter<LanguageAlpha3Code> {
        private static final String LANGUAGE_NOT_SET = "language code is not set";

        @Override
        public String toString(LanguageAlpha3Code languageCode) {
            if (languageCode == null) {
                return LANGUAGE_NOT_SET;
            }

            return languageCode.getName() + " (" + languageCode.toString() + ")";
        }

        @Override
        public LanguageAlpha3Code fromString(String rawCode) {
            if (Objects.equals(rawCode, LANGUAGE_NOT_SET)) {
                return null;
            }

            int leftBracketIndex = rawCode.indexOf("(");

            /* + 4 because every code is 3 symbol long. */
            return LanguageAlpha3Code.getByCode(rawCode.substring(leftBracketIndex + 1, leftBracketIndex + 4));
        }
    }
}
