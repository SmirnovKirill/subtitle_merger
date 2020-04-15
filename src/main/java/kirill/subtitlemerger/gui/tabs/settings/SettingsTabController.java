package kirill.subtitlemerger.gui.tabs.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.forms_and_controls.ActionResultLabels;
import kirill.subtitlemerger.gui.utils.entities.FileOrigin;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.settings.MergeMode;
import kirill.subtitlemerger.logic.settings.SettingException;
import kirill.subtitlemerger.logic.settings.SettingType;
import kirill.subtitlemerger.logic.settings.Settings;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

@CommonsLog
public class SettingsTabController {
    public static final String MERGE_MODE_ORIGINAL_VIDEOS = "Modify original videos";

    public static final String MERGE_MODE_SEPARATE_SUBTITLE_FILES = "Create separate subtitle files";

    private static final LanguageCodeStringConverter LANGUAGE_CODE_STRING_CONVERTER = new LanguageCodeStringConverter();

    private Stage stage;

    private GuiContext context;

    private Settings settings;

    @FXML
    private Pane unavailablePane;

    @FXML
    private Pane settingsPane;

    @FXML
    private TextField ffprobePathField;

    @FXML
    private Button ffprobeSetButton;

    @FXML
    private TextField ffmpegPathField;

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
    private CheckBox makeMergedStreamsDefaultCheckBox;

    private BooleanProperty markStreamCheckBoxVisible;

    @FXML
    private CheckBox plainTextCheckBox;

    @FXML
    private ActionResultLabels actionResultLabels;

    private String ffprobeCurrentPath;

    private String ffmpegCurrentPath;

    public SettingsTabController() {
        markStreamCheckBoxVisible = new SimpleBooleanProperty(false);
    }

    public boolean isMarkStreamCheckBoxVisible() {
        return markStreamCheckBoxVisible.get();
    }

    public BooleanProperty markStreamCheckBoxVisibleProperty() {
        return markStreamCheckBoxVisible;
    }

    public void setMarkStreamCheckBoxVisible(boolean markStreamCheckBoxVisible) {
        this.markStreamCheckBoxVisible.set(markStreamCheckBoxVisible);
    }

    public void initialize(Stage stage, GuiContext context) {
        this.stage = stage;
        this.context = context;
        this.settings = context.getSettings();

        GuiUtils.setTextFieldChangeListeners(
                ffprobePathField,
                (path) -> processFfprobePath(path, FileOrigin.TEXT_FIELD)
        );
        GuiUtils.setTextFieldChangeListeners(
                ffmpegPathField,
                (path) -> processFfmpegPath(path, FileOrigin.TEXT_FIELD)
        );
        upperLanguageComboBox.getItems().setAll(LogicConstants.ALLOWED_LANGUAGE_CODES);
        upperLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);
        lowerLanguageComboBox.getItems().setAll(LogicConstants.ALLOWED_LANGUAGE_CODES);
        lowerLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);

        setInitialValues();
        mergeModeToggleGroup.selectedToggleProperty().addListener(this::mergeModeChanged);
        makeMergedStreamsDefaultCheckBox.selectedProperty().addListener(this::makeMergedStreamsDefaultChanged);
        plainTextCheckBox.selectedProperty().addListener(this::plainTextChanged);
        context.videosInProgressProperty().addListener(this::videosInProgressChanged);
    }

    private void processFfprobePath(String path, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.FILE_CHOOSER && Objects.equals(path, ffprobeCurrentPath)) {
            return;
        }

        if (fileOrigin == FileOrigin.FILE_CHOOSER) {
            ffprobePathField.setText(path);
        }

        clearState();
        ffprobePathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        ffprobeSetButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);

        if (StringUtils.isBlank(path)) {
            settings.clearFfprobeFile();
            context.setFfprobe(null);
            context.getMissingSettings().add(SettingType.FFPROBE_PATH);
        } else {
            try {
                File previousValue = settings.getFfprobeFile();

                settings.saveFfprobeFile(path);
                context.setFfprobe(new Ffprobe(settings.getFfprobeFile()));
                context.getMissingSettings().remove(SettingType.FFPROBE_PATH);

                if (previousValue != null) {
                    if (Objects.equals(settings.getFfprobeFile(), previousValue)) {
                        actionResultLabels.setOnlySuccess("Path to ffprobe has stayed the same");
                    } else {
                        actionResultLabels.setOnlySuccess("Path to ffprobe has been updated successfully");
                    }
                } else {
                    actionResultLabels.setOnlySuccess("Path to ffprobe has been saved successfully");
                }
            } catch (SettingException | FfmpegException e) {
                settings.clearFfprobeFile();
                context.setFfprobe(null);

                ffprobePathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                ffprobeSetButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);

                actionResultLabels.setOnlyError("Incorrect path to ffprobe");
            } catch (InterruptedException e) {
                log.error("something's not right, process can't be interrupted");
                throw new IllegalStateException();
            }
        }

        ffprobeCurrentPath = path;
    }

    private void processFfmpegPath(String path, FileOrigin fileOrigin) {
        if (fileOrigin == FileOrigin.FILE_CHOOSER && Objects.equals(path, ffmpegCurrentPath)) {
            return;
        }

        if (fileOrigin == FileOrigin.FILE_CHOOSER) {
            ffmpegPathField.setText(path);
        }

        clearState();
        ffmpegPathField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);
        ffmpegSetButton.getStyleClass().remove(GuiConstants.BUTTON_ERROR_CLASS);

        if (StringUtils.isBlank(path)) {
            settings.clearFfmpegFile();
            context.setFfmpeg(null);
            context.getMissingSettings().add(SettingType.FFMPEG_PATH);
        } else {
            try {
                File previousValue = settings.getFfmpegFile();

                settings.saveFfmpegFile(path);
                context.setFfmpeg(new Ffmpeg(settings.getFfmpegFile()));
                context.getMissingSettings().remove(SettingType.FFMPEG_PATH);

                if (previousValue != null) {
                    if (Objects.equals(settings.getFfmpegFile(), previousValue)) {
                        actionResultLabels.setOnlySuccess("Path to ffprobe has stayed the same");
                    } else {
                        actionResultLabels.setOnlySuccess("Path to ffmpeg has been updated successfully");
                    }
                } else {
                    actionResultLabels.setOnlySuccess("Path to ffmpeg has been saved successfully");
                }
            } catch (SettingException | FfmpegException e) {
                settings.clearFfmpegFile();
                context.setFfmpeg(null);

                ffmpegPathField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
                ffmpegSetButton.getStyleClass().add(GuiConstants.BUTTON_ERROR_CLASS);

                actionResultLabels.setOnlyError("Incorrect path to ffmpeg");
            } catch (InterruptedException e) {
                log.error("something's not right, process can't be interrupted");
                throw new IllegalStateException();
            }
        }

        ffmpegCurrentPath = path;
    }

    private void setInitialValues() {
        setFfprobeInitialValue();
        setFfmpegInitialValue();
        setUpperSubtitlesInitialValue();
        setSwapLanguagesButtonVisibility();
        setLowerSubtitlesInitialValue();
        setMergeModeInitialValue();
        setMarkCheckBoxVisibility();

        makeMergedStreamsDefaultCheckBox.setSelected(settings.isMakeMergedStreamsDefault());
        plainTextCheckBox.setSelected(settings.isPlainTextSubtitles());
    }

    private void setFfprobeInitialValue() {
        File ffprobeFile = settings.getFfprobeFile();

        if (ffprobeFile != null) {
            ffprobePathField.setText(ffprobeFile.getAbsolutePath());
        }

        ffprobeCurrentPath = ObjectUtils.firstNonNull(ffprobePathField.getText(), "");
    }

    private void setFfmpegInitialValue() {
        File ffmpegFile = settings.getFfmpegFile();
        if (ffmpegFile != null) {
            ffmpegPathField.setText(ffmpegFile.getAbsolutePath());
        }

        ffmpegCurrentPath = ObjectUtils.firstNonNull(ffmpegPathField.getText(), "");
    }

    private void setUpperSubtitlesInitialValue() {
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
        LanguageAlpha3Code lowerLanguage = settings.getLowerLanguage();
        if (lowerLanguage != null) {
            lowerLanguageComboBox.getSelectionModel().select(lowerLanguage);
        }
    }

    private void setMergeModeInitialValue() {
        MergeMode mergeMode = context.getSettings().getMergeMode();
        if (mergeMode == null) {
            return;
        }

        String value;
        switch (mergeMode) {
            case ORIGINAL_VIDEOS:
                value = MERGE_MODE_ORIGINAL_VIDEOS;
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
        MergeMode mergeMode = context.getSettings().getMergeMode();
        setMarkStreamCheckBoxVisible(mergeMode == MergeMode.ORIGINAL_VIDEOS);
    }

    private void mergeModeChanged(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
        RadioButton radioButton = (RadioButton) newValue;

        MergeMode mergeMode;
        switch (radioButton.getText()) {
            case MERGE_MODE_ORIGINAL_VIDEOS:
                mergeMode = MergeMode.ORIGINAL_VIDEOS;
                break;
            case MERGE_MODE_SEPARATE_SUBTITLE_FILES:
                mergeMode = MergeMode.SEPARATE_SUBTITLE_FILES;
                break;
            default:
                throw new IllegalStateException();
        }

        try {
            settings.saveMergeMode(mergeMode.toString());
            context.getMissingSettings().remove(SettingType.MERGE_MODE);
            setMarkCheckBoxVisibility();

            actionResultLabels.setOnlySuccess("Merge mode has been saved successfully");
        } catch (SettingException e) {
            log.error("merge mode hasn't been saved: " + ExceptionUtils.getStackTrace(e));

            actionResultLabels.setOnlyError("Something bad has happened, merge mode hasn't been saved");
        }
    }

    private void makeMergedStreamsDefaultChanged(
            ObservableValue<? extends Boolean> observable,
            Boolean oldValue,
            Boolean newValue
    ) {
        try {
            context.getSettings().saveMakeMergedStreamsDefault(newValue.toString());

            if (newValue) {
                actionResultLabels.setOnlySuccess("Merged subtitles will be selected as default from now on");
            } else {
                actionResultLabels.setOnlySuccess("Merged subtitles will not be selected as default from now on");
            }
        } catch (SettingException e) {
            log.error("failed to save mark stream as default flag: " + ExceptionUtils.getStackTrace(e));

            actionResultLabels.setOnlyError("Something bad has happened, flag value hasn't been saved");
        }
    }

    private void plainTextChanged(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        try {
            context.getSettings().savePlainTextSubtitles(newValue.toString());

            if (newValue) {
                actionResultLabels.setOnlySuccess("Merged subtitles will be in plain text from now on");
            } else {
                actionResultLabels.setOnlySuccess("Subtitles will have formatting as is from now on");
            }
        } catch (SettingException e) {
            log.error("failed to save plane text flag: " + ExceptionUtils.getStackTrace(e));

            actionResultLabels.setOnlyError("Something bad has happened, plane text flag value hasn't been saved");
        }
    }

    private void videosInProgressChanged(
            ObservableValue<? extends Boolean> observable,
            Boolean oldValue,
            Boolean newValue
    ) {
        if (newValue) {
            settingsPane.setDisable(true);
            unavailablePane.setVisible(true);
        } else {
            settingsPane.setDisable(false);
            unavailablePane.setVisible(false);
        }
    }

    @FXML
    private void ffprobeFileButtonClicked() {
        File ffprobeFile = getFfprobeFile(ffprobeCurrentPath, stage, settings).orElse(null);
        if (ffprobeFile == null) {
            clearState();
            return;
        }

        processFfprobePath(ffprobeFile.getAbsolutePath(), FileOrigin.FILE_CHOOSER);
    }

    private static Optional<File> getFfprobeFile(String currentPath, Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        String title;
        File initialDirectory;
        if (settings.getFfprobeFile() != null) {
            title = "update path to ffprobe";
            initialDirectory = settings.getFfprobeFile().getParentFile();
        } else {
            title = "choose path to ffprobe";

            if (settings.getFfmpegFile() != null){
                initialDirectory = settings.getFfmpegFile().getParentFile();
            } else {
                initialDirectory = extractParentDirectoryIfPossible(currentPath).orElse(null);
            }
        }

        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(initialDirectory);

        return Optional.ofNullable(fileChooser.showOpenDialog(stage));
    }

    private static Optional<File> extractParentDirectoryIfPossible(String path) {
        if (StringUtils.isBlank(path)) {
            return Optional.empty();
        }

        File parent = new File(path).getParentFile();
        if (parent == null) {
            return Optional.empty();
        }

        if (parent.isDirectory()) {
            return Optional.of(parent);
        }

        return Optional.empty();
    }

    private void clearState() {
        actionResultLabels.clear();
    }

    @FXML
    private void ffmpegFileButtonClicked() {
        File ffmpegFile = getFfmpegFile(ffmpegCurrentPath, stage, settings).orElse(null);
        if (ffmpegFile == null) {
            clearState();
            return;
        }

        processFfmpegPath(ffmpegFile.getAbsolutePath(), FileOrigin.FILE_CHOOSER);
    }

    private static Optional<File> getFfmpegFile(String currentPath, Stage stage, Settings settings) {
        FileChooser fileChooser = new FileChooser();

        String title;
        File initialDirectory;
        if (settings.getFfmpegFile() != null) {
            title = "update path to ffmpeg";
            initialDirectory = settings.getFfmpegFile().getParentFile();
        } else {
            title = "choose path to ffmpeg";

            if (settings.getFfprobeFile() != null) {
                initialDirectory = settings.getFfprobeFile().getParentFile();
            } else {
                initialDirectory = extractParentDirectoryIfPossible(currentPath).orElse(null);
            }
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
            actionResultLabels.setOnlyError("Languages have to be different, please choose another one");
            return;
        }

        boolean hadValueBefore = settings.getUpperLanguage() != null;

        try {
            settings.saveUpperLanguage(value.toString());
            context.getMissingSettings().remove(SettingType.UPPER_LANGUAGE);
            setSwapLanguagesButtonVisibility();

            if (hadValueBefore) {
                actionResultLabels.setOnlySuccess("Language for upper subtitles has been updated successfully");
            } else {
                actionResultLabels.setOnlySuccess("Language for upper subtitles has been saved successfully");
            }
        } catch (SettingException e) {
            log.error("language for upper subtitles has not been saved: " + ExceptionUtils.getStackTrace(e));

            actionResultLabels.setOnlyError("Something bad has happened, language hasn't been saved");
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

            actionResultLabels.setOnlySuccess("Languages have been swapped successfully");
        } catch (SettingException e) {
            log.error("languages haven't been swapped: " + ExceptionUtils.getStackTrace(e));

            actionResultLabels.setOnlyError("Something bad has happened, languages haven't been swapped");
        }
    }

    @FXML
    private void lowerLanguageChanged() {
        LanguageAlpha3Code value = lowerLanguageComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(value, settings.getLowerLanguage())) {
            return;
        }

        if (Objects.equals(value, settings.getUpperLanguage())) {
            actionResultLabels.setOnlyError("Languages have to be different, please choose another one");
            return;
        }

        boolean hadValueBefore = settings.getLowerLanguage() != null;

        try {
            settings.saveLowerLanguage(value.toString());
            context.getMissingSettings().remove(SettingType.LOWER_LANGUAGE);
            setSwapLanguagesButtonVisibility();

            if (hadValueBefore) {
                actionResultLabels.setOnlySuccess("Language for lower subtitles has been updated successfully");
            } else {
                actionResultLabels.setOnlySuccess("Language for lower subtitles has been saved successfully");
            }
        } catch (SettingException e) {
            log.error("language for lower subtitles has not been saved: " + ExceptionUtils.getStackTrace(e));

            actionResultLabels.setOnlyError("Something bad has happened, language hasn't been saved");
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
