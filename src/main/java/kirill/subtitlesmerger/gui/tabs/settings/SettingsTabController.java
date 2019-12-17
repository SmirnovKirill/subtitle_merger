package kirill.subtitlesmerger.gui.tabs.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import kirill.subtitlesmerger.gui.GuiConstants;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.GuiPreferences;
import kirill.subtitlesmerger.logic.LogicConstants;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.FfmpegException;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.Objects;

@CommonsLog
public class SettingsTabController {
    private static final LanguageCodeStringConverter LANGUAGE_CODE_STRING_CONVERTER = new LanguageCodeStringConverter();

    private Stage stage;

    private GuiContext guiContext;

    @FXML
    private TextField ffprobeField;

    @FXML
    private Button ffprobeSetButton;

    private FileChooser ffprobeChooser;

    @FXML
    private TextField ffmpegField;

    @FXML
    private Button ffmpegSetButton;

    private FileChooser ffmpegChooser;

    @FXML
    private Node upperSubtitlesQuestionWrapper;

    @FXML
    //todo make editable with drop-down
    private ComboBox<LanguageAlpha3Code> upperLanguageComboBox;

    @FXML
    private Node lowerSubtitlesQuestionWrapper;

    @FXML
    //todo make editable with drop-down
    private ComboBox<LanguageAlpha3Code> lowerLanguageComboBox;

    @FXML
    private Button swapLanguagesButton;

    @FXML
    private Label resultLabel;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.stage = stage;
        this.guiContext = guiContext;

        this.ffprobeChooser = new FileChooser();
        this.ffmpegChooser = new FileChooser();
        initComboBoxes();
        Tooltip.install(upperSubtitlesQuestionWrapper, generateLanguageTooltip());
        Tooltip.install(lowerSubtitlesQuestionWrapper, generateLanguageTooltip());
        updateFileChoosersAndFields();
    }

    private void initComboBoxes() {
        upperLanguageComboBox.getItems().setAll(LogicConstants.ALLOWED_LANGUAGE_CODES);
        upperLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);

        lowerLanguageComboBox.getItems().setAll(LogicConstants.ALLOWED_LANGUAGE_CODES);
        lowerLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);
    }

    private static Tooltip generateLanguageTooltip() {
        Tooltip result = new Tooltip(
                "this setting will be used to auto-detect subtitles\n"
                        + "for merging when working with videos"
        );

        result.setShowDelay(Duration.ZERO);
        result.setShowDuration(Duration.INDEFINITE);

        return result;
    }

    private void updateFileChoosersAndFields() {
        File ffprobeFile = guiContext.getConfig().getFfprobeFile();
        File ffmpegFile = guiContext.getConfig().getFfmpegFile();

        if (ffprobeFile != null) {
            updateFfprobeInfo(
                    ffprobeFile.getAbsolutePath(),
                    "update path to ffprobe",
                    "update path to ffprobe",
                    ffprobeFile.getParentFile()
            );
        } else {
            updateFfprobeInfo(
                    "",
                    "choose path to ffprobe",
                    "choose path to ffprobe",
                    ffmpegFile != null ? ffmpegFile.getParentFile() : null
            );
        }

        if (ffmpegFile != null) {
            updateFfmpegInfo(
                    ffmpegFile.getAbsolutePath(),
                    "update path to ffmpeg",
                    "update path to ffmpeg",
                    ffmpegFile.getParentFile()
            );
        } else {
            updateFfmpegInfo(
                    "",
                    "choose path to ffmpeg",
                    "choose path to ffmpeg",
                    ffprobeFile != null ? ffprobeFile.getParentFile() : null
            );
        }

        LanguageAlpha3Code upperLanguage = guiContext.getConfig().getUpperLanguage();
        if (upperLanguage != null) {
            upperLanguageComboBox.getSelectionModel().select(upperLanguage);
        }

        LanguageAlpha3Code lowerLanguage = guiContext.getConfig().getLowerLanguage();
        if (lowerLanguage != null) {
            lowerLanguageComboBox.getSelectionModel().select(lowerLanguage);
        }

        boolean swapButtonDisable = guiContext.getConfig().getUpperLanguage() == null
                || guiContext.getConfig().getLowerLanguage() == null;

        swapLanguagesButton.setDisable(swapButtonDisable);
    }

    private void updateFfprobeInfo(
            String fieldText,
            String buttonText,
            String fileChooserTitle,
            File fileChooserInitialDirectory
    ) {
        ffprobeField.setText(fieldText);
        ffprobeSetButton.setText(buttonText);
        ffprobeChooser.setTitle(fileChooserTitle);
        ffprobeChooser.setInitialDirectory(fileChooserInitialDirectory);
    }

    private void updateFfmpegInfo(
            String fieldText,
            String buttonText,
            String fileChooserTitle,
            File fileChooserInitialDirectory
    ) {
        ffmpegField.setText(fieldText);
        ffmpegSetButton.setText(buttonText);
        ffmpegChooser.setTitle(fileChooserTitle);
        ffmpegChooser.setInitialDirectory(fileChooserInitialDirectory);
    }

    @FXML
    private void ffprobeFileButtonClicked() {
        File ffprobeFile = ffprobeChooser.showOpenDialog(stage);
        if (ffprobeFile == null) {
            clearResult();
            return;
        }

        if (Objects.equals(ffprobeFile, guiContext.getConfig().getFfprobeFile())) {
            showSuccessMessage("path to ffprobe has stayed the same");
            return;
        }

        boolean hadValueBefore = guiContext.getConfig().getFfmpegFile() != null;

        try {
            guiContext.getConfig().saveFfprobeFile(ffprobeFile.getAbsolutePath());
            guiContext.setFfprobe(new Ffprobe(guiContext.getConfig().getFfprobeFile()));
            updateFileChoosersAndFields();

            if (hadValueBefore) {
                showSuccessMessage("path to ffprobe has been updated successfully");
            } else {
                showSuccessMessage("path to ffprobe has been saved successfully");
            }
        } catch (GuiPreferences.ConfigException | FfmpegException e) {
            showErrorMessage("incorrect path to ffprobe");
        }
    }

    private void clearResult() {
        resultLabel.setText("");
        resultLabel.getStyleClass().remove(GuiConstants.LABEL_SUCCESS_CLASS);
        resultLabel.getStyleClass().remove(GuiConstants.LABEL_ERROR_CLASS);
    }

    private void showSuccessMessage(String text) {
        clearResult();
        resultLabel.getStyleClass().add(GuiConstants.LABEL_SUCCESS_CLASS);
        resultLabel.setText(text);
    }

    private void showErrorMessage(String text) {
        clearResult();
        resultLabel.getStyleClass().add(GuiConstants.LABEL_ERROR_CLASS);
        resultLabel.setText(text);
    }

    @FXML
    private void ffmpegFileButtonClicked() {
        File ffmpegFile = ffmpegChooser.showOpenDialog(stage);
        if (ffmpegFile == null) {
            clearResult();
            return;
        }

        if (Objects.equals(ffmpegFile, guiContext.getConfig().getFfmpegFile())) {
            showSuccessMessage("path to ffmpeg has stayed the same");
            return;
        }

        boolean hadValueBefore = guiContext.getConfig().getFfmpegFile() != null;

        try {
            guiContext.getConfig().saveFfmpegFile(ffmpegFile.getAbsolutePath());
            guiContext.setFfmpeg(new Ffmpeg(guiContext.getConfig().getFfmpegFile()));
            updateFileChoosersAndFields();

            if (hadValueBefore) {
                showSuccessMessage("path to ffmpeg has been updated successfully");
            } else {
                showSuccessMessage("path to ffmpeg has been saved successfully");
            }
        } catch (GuiPreferences.ConfigException | FfmpegException e) {
            showErrorMessage("incorrect path to ffmpeg");
        }
    }

    @FXML
    private void upperLanguageChanged() {
        LanguageAlpha3Code value = upperLanguageComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(value, guiContext.getConfig().getUpperLanguage())) {
            return;
        }

        if (Objects.equals(value, guiContext.getConfig().getLowerLanguage())) {
            updateFileChoosersAndFields();
            showErrorMessage("languages have to be different, please select another one");
            return;
        }

        boolean hadValueBefore = guiContext.getConfig().getUpperLanguage() != null;

        try {
            guiContext.getConfig().saveUpperLanguage(value.toString());
            updateFileChoosersAndFields();

            if (hadValueBefore) {
                showSuccessMessage("language for upper subtitles has been updated successfully");
            } else {
                showSuccessMessage("language for upper subtitles has been saved successfully");
            }
        } catch (GuiPreferences.ConfigException e) {
            log.error("language for upper subtitles has not been saved: " + ExceptionUtils.getStackTrace(e));

            showErrorMessage("something bad has happened, language hasn't been saved");
        }
    }

    @FXML
    private void swapLanguagesButtonClicked() {
        LanguageAlpha3Code oldUpperLanguage = guiContext.getConfig().getUpperLanguage();
        LanguageAlpha3Code oldLowerLanguage = guiContext.getConfig().getLowerLanguage();

        try {
            guiContext.getConfig().saveUpperLanguage(oldLowerLanguage.toString());
            guiContext.getConfig().saveLowerLanguage(oldUpperLanguage.toString());
            updateFileChoosersAndFields();

            showSuccessMessage("languages have been swapped successfully");
        } catch (GuiPreferences.ConfigException e) {
            log.error("languages haven't been swapped: " + ExceptionUtils.getStackTrace(e));

            showErrorMessage("something bad has happened, languages haven't been swapped");
        }
    }

    @FXML
    private void lowerLanguageChanged() {
        LanguageAlpha3Code value = lowerLanguageComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(value, guiContext.getConfig().getLowerLanguage())) {
            return;
        }

        if (Objects.equals(value, guiContext.getConfig().getUpperLanguage())) {
            updateFileChoosersAndFields();
            showErrorMessage("languages have to be different, please select another one");
            return;
        }

        boolean hadValueBefore = guiContext.getConfig().getLowerLanguage() != null;

        try {
            guiContext.getConfig().saveLowerLanguage(value.toString());
            updateFileChoosersAndFields();

            if (hadValueBefore) {
                showSuccessMessage("language for lower subtitles has been updated successfully");
            } else {
                showSuccessMessage("language for lower subtitles has been saved successfully");
            }
        } catch (GuiPreferences.ConfigException e) {
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
