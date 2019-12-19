package kirill.subtitlesmerger.gui.tabs.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kirill.subtitlesmerger.gui.GuiConstants;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.GuiSettings;
import kirill.subtitlesmerger.logic.LogicConstants;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.FfmpegException;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

@CommonsLog
public class SettingsTabController {
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
    private Label resultLabel;

    public void initialize(Stage stage, GuiContext context) {
        this.stage = stage;
        this.context = context;
        this.settings = context.getSettings();

        initializeComboBoxes();
        initializeTextFields();
        setSwapLanguagesButtonVisibility();
    }

    private void initializeComboBoxes() {
        upperLanguageComboBox.getItems().setAll(LogicConstants.ALLOWED_LANGUAGE_CODES);
        upperLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);

        lowerLanguageComboBox.getItems().setAll(LogicConstants.ALLOWED_LANGUAGE_CODES);
        lowerLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);
    }

    private void initializeTextFields() {
        File ffprobeFile = settings.getFfprobeFile();
        File ffmpegFile = settings.getFfmpegFile();

        if (ffprobeFile != null) {
            ffprobeField.setText(ffprobeFile.getAbsolutePath());
            ffprobeSetButton.setText(UPDATE_FFPROBE_BUTTON_TEXT);
        } else {
            ffprobeSetButton.setText("choose path to ffprobe");
        }

        if (ffmpegFile != null) {
            ffmpegField.setText(ffmpegFile.getAbsolutePath());
            ffmpegSetButton.setText(UPDATE_FFMPEG_BUTTON_TEXT);
        } else {
            ffmpegSetButton.setText("choose path to ffmpeg");
        }

        LanguageAlpha3Code upperLanguage = settings.getUpperLanguage();
        if (upperLanguage != null) {
            upperLanguageComboBox.getSelectionModel().select(upperLanguage);
        }

        LanguageAlpha3Code lowerLanguage = settings.getLowerLanguage();
        if (lowerLanguage != null) {
            lowerLanguageComboBox.getSelectionModel().select(lowerLanguage);
        }
    }

    private void setSwapLanguagesButtonVisibility() {
        boolean swapButtonDisable = settings.getUpperLanguage() == null || settings.getLowerLanguage() == null;

        swapLanguagesButton.setDisable(swapButtonDisable);
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
