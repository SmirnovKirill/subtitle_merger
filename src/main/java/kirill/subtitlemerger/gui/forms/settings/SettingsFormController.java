package kirill.subtitlemerger.gui.forms.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.common_controls.ActionResultLabel;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.settings.MergeMode;
import kirill.subtitlemerger.logic.settings.SettingType;
import kirill.subtitlemerger.logic.settings.Settings;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CommonsLog
public class SettingsFormController {
    public static final String MERGE_MODE_ORIGINAL_VIDEOS = "Modify original videos";

    public static final String MERGE_MODE_SEPARATE_SUBTITLE_FILES = "Create separate subtitle files";

    private static final List<LanguageAlpha3Code> ALLOWED_LANGUAGES = LogicConstants.ALLOWED_LANGUAGES.stream()
            .sorted(Comparator.comparing(LanguageAlpha3Code::getName))
            .collect(Collectors.toList());

    private static final LanguageCodeStringConverter LANGUAGE_CODE_STRING_CONVERTER = new LanguageCodeStringConverter();

    private BooleanProperty makeDefaultVisible;

    @FXML
    private ToggleGroup mergeModeToggleGroup;

    @FXML
    private Pane settingsPane;

    @FXML
    //todo make editable with drop-down
    private ComboBox<LanguageAlpha3Code> upperLanguageComboBox;

    @FXML
    private Button swapButton;

    @FXML
    //todo make editable with drop-down
    private ComboBox<LanguageAlpha3Code> lowerLanguageComboBox;

    @FXML
    private CheckBox makeDefaultCheckBox;

    @FXML
    private CheckBox plainTextCheckBox;

    @FXML
    private ActionResultLabel actionResultLabel;

    @FXML
    private Pane unavailablePane;

    private Settings settings;

    private GuiContext context;

    public void initialize(GuiContext context) {
        settings = context.getSettings();
        this.context = context;

        context.videosInProgressProperty().addListener(this::videosInProgressChanged);

        setUpperLanguage();
        swapButton.setDisable(settings.getUpperLanguage() == null || settings.getLowerLanguage() == null);
        setLowerLanguage();
        setMergeMode();
        setMakeDefaultVisible(settings.getMergeMode() == MergeMode.ORIGINAL_VIDEOS);
        makeDefaultCheckBox.setSelected(settings.isMakeMergedStreamsDefault());
        plainTextCheckBox.setSelected(settings.isPlainTextSubtitles());

        mergeModeToggleGroup.selectedToggleProperty().addListener(this::mergeModeChanged);
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

    private void setUpperLanguage() {
        upperLanguageComboBox.getItems().setAll(ALLOWED_LANGUAGES);
        upperLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);

        LanguageAlpha3Code upperLanguage = settings.getUpperLanguage();
        if (upperLanguage != null) {
            upperLanguageComboBox.getSelectionModel().select(upperLanguage);
        }
    }

    private void setLowerLanguage() {
        lowerLanguageComboBox.getItems().setAll(ALLOWED_LANGUAGES);
        lowerLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);

        LanguageAlpha3Code lowerLanguage = settings.getLowerLanguage();
        if (lowerLanguage != null) {
            lowerLanguageComboBox.getSelectionModel().select(lowerLanguage);
        }
    }

    private void setMergeMode() {
        MergeMode mergeMode = settings.getMergeMode();
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
                log.error("unknown merge mode: " + mergeMode + ", most likely a bug");
                throw new IllegalStateException();
        }

        for (Toggle toggle : mergeModeToggleGroup.getToggles()) {
            RadioButton radioButton = (RadioButton) toggle;
            if (value.equals(radioButton.getText())) {
                toggle.setSelected(true);
                return;
            }
        }

        log.error("no radio button for merge mode " + mergeMode + ", most likely a bug");
        throw new IllegalStateException();
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
                log.error("unexpected radio button value: " + radioButton.getText() + ", most likely a bug");
                throw new IllegalStateException();
        }

        settings.saveCorrect(mergeMode, SettingType.MERGE_MODE);
        context.getMissingSettings().remove(SettingType.MERGE_MODE);
        setMakeDefaultVisible(settings.getMergeMode() == MergeMode.ORIGINAL_VIDEOS);

        actionResultLabel.setSuccess("The merge mode has been saved successfully");
    }

    @FXML
    private void upperLanguageChanged() {
        LanguageAlpha3Code value = upperLanguageComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(value, settings.getUpperLanguage())) {
            return;
        }

        if (Objects.equals(value, settings.getLowerLanguage())) {
            actionResultLabel.setError("Languages have to be different, please choose another one");
            return;
        }

        boolean hadValueBefore = settings.getUpperLanguage() != null;

        settings.saveCorrect(value, SettingType.UPPER_LANGUAGE);
        context.getMissingSettings().remove(SettingType.UPPER_LANGUAGE);
        swapButton.setDisable(settings.getUpperLanguage() == null || settings.getLowerLanguage() == null);

        if (hadValueBefore) {
            actionResultLabel.setSuccess("The language for upper subtitles has been updated successfully");
        } else {
            actionResultLabel.setSuccess("The language for upper subtitles has been saved successfully");
        }
    }

    @FXML
    private void swapClicked() {
        LanguageAlpha3Code oldUpperLanguage = settings.getUpperLanguage();
        LanguageAlpha3Code oldLowerLanguage = settings.getLowerLanguage();

        settings.saveCorrect(oldLowerLanguage, SettingType.UPPER_LANGUAGE);
        upperLanguageComboBox.getSelectionModel().select(oldLowerLanguage);

        settings.saveCorrect(oldUpperLanguage, SettingType.LOWER_LANGUAGE);
        lowerLanguageComboBox.getSelectionModel().select(oldUpperLanguage);

        actionResultLabel.setSuccess("The languages have been swapped successfully");
    }

    @FXML
    private void lowerLanguageChanged() {
        LanguageAlpha3Code value = lowerLanguageComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(value, settings.getLowerLanguage())) {
            return;
        }

        if (Objects.equals(value, settings.getUpperLanguage())) {
            actionResultLabel.setError("Languages have to be different, please choose another one");
            return;
        }

        boolean hadValueBefore = settings.getLowerLanguage() != null;

        settings.saveCorrect(value, SettingType.LOWER_LANGUAGE);
        context.getMissingSettings().remove(SettingType.LOWER_LANGUAGE);
        swapButton.setDisable(settings.getUpperLanguage() == null || settings.getLowerLanguage() == null);

        if (hadValueBefore) {
            actionResultLabel.setSuccess("The language for lower subtitles has been updated successfully");
        } else {
            actionResultLabel.setSuccess("The language for lower subtitles has been saved successfully");
        }
    }

    @FXML
    private void makeDefaultClicked() {
        boolean makeDefault = makeDefaultCheckBox.isSelected();
        settings.saveCorrect(makeDefault, SettingType.MAKE_MERGED_STREAMS_DEFAULT);

        if (makeDefault) {
            actionResultLabel.setSuccess("Merged subtitles will be selected as default from now on");
        } else {
            actionResultLabel.setSuccess("Merged subtitles will not be selected as default from now on");
        }
    }

    @FXML
    private void plainTextClicked() {
        boolean plainText = plainTextCheckBox.isSelected();
        settings.saveCorrect(plainText, SettingType.PLAIN_TEXT_SUBTITLES);

        if (plainText) {
            actionResultLabel.setSuccess("Merged subtitles will be in plain text from now on");
        } else {
            actionResultLabel.setSuccess("Merged subtitles will have an original format from now on");
        }
    }

    public SettingsFormController() {
        makeDefaultVisible = new SimpleBooleanProperty(false);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean getMakeDefaultVisible() {
        return makeDefaultVisible.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty makeDefaultVisibleProperty() {
        return makeDefaultVisible;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setMakeDefaultVisible(boolean makeDefaultVisible) {
        this.makeDefaultVisible.set(makeDefaultVisible);
    }

    private static class LanguageCodeStringConverter extends StringConverter<LanguageAlpha3Code> {
        private static final String LANGUAGE_NOT_SET = "The language code is not set";

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
