package kirill.subtitlemerger.gui.forms.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.common_controls.ActionResultLabel;
import kirill.subtitlemerger.gui.common_controls.auto_complete.AutoCompleteTextField;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.settings.MergeMode;
import kirill.subtitlemerger.logic.settings.SettingType;
import kirill.subtitlemerger.logic.settings.Settings;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

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
    private AutoCompleteTextField<LanguageAlpha3Code> upperLanguageTextField;

    @FXML
    private Button swapButton;

    @FXML
    private AutoCompleteTextField<LanguageAlpha3Code> lowerLanguageTextField;

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

        context.videosInProgressProperty().addListener(
                observable -> videosInProgressChanged(context.getVideosInProgress())
        );

        setUpperLanguage();
        swapButton.setDisable(settings.getUpperLanguage() == null || settings.getLowerLanguage() == null);
        setLowerLanguage();
        setMergeMode();
        setMakeDefaultVisible(settings.getMergeMode() == MergeMode.ORIGINAL_VIDEOS);
        makeDefaultCheckBox.setSelected(settings.isMakeMergedStreamsDefault());
        plainTextCheckBox.setSelected(settings.isPlainTextSubtitles());

        mergeModeToggleGroup.selectedToggleProperty().addListener(
                observable -> mergeModeChanged(mergeModeToggleGroup.getSelectedToggle())
        );
    }

    private void videosInProgressChanged(boolean videosInProgress) {
        if (videosInProgress) {
            settingsPane.setDisable(true);
            unavailablePane.setVisible(true);
        } else {
            settingsPane.setDisable(false);
            unavailablePane.setVisible(false);
        }
    }

    private void setUpperLanguage() {
        upperLanguageTextField.setItems(ALLOWED_LANGUAGES);
        upperLanguageTextField.setConverter(LANGUAGE_CODE_STRING_CONVERTER);
        upperLanguageTextField.setText(LANGUAGE_CODE_STRING_CONVERTER.toString(settings.getUpperLanguage()));
        upperLanguageTextField.setValueSetHandler(this::handleUpperLanguageSet);
    }

    private void handleUpperLanguageSet(LanguageAlpha3Code value) {
        upperLanguageTextField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);

        if (Objects.equals(value, settings.getLowerLanguage())) {
            upperLanguageTextField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            actionResultLabel.setError("Languages have to be different, please choose another one");
            return;
        }

        boolean hadValueBefore = settings.getUpperLanguage() != null;

        settings.saveCorrect(value, SettingType.UPPER_LANGUAGE);
        context.getMissingSettings().remove(SettingType.UPPER_LANGUAGE);
        swapButton.setDisable(settings.getUpperLanguage() == null || settings.getLowerLanguage() == null);

        String prefix = "The language for upper subtitles (" + value.toString().toUpperCase() + ") has been";
        if (hadValueBefore) {
            actionResultLabel.setSuccess(prefix + " updated successfully");
        } else {
            actionResultLabel.setSuccess(prefix + " saved successfully");
        }
    }

    private void setLowerLanguage() {
        lowerLanguageTextField.setItems(ALLOWED_LANGUAGES);
        lowerLanguageTextField.setConverter(LANGUAGE_CODE_STRING_CONVERTER);
        lowerLanguageTextField.setText(LANGUAGE_CODE_STRING_CONVERTER.toString(settings.getLowerLanguage()));
        lowerLanguageTextField.setValueSetHandler(this::handleLowerLanguageSet);
    }

    private void handleLowerLanguageSet(LanguageAlpha3Code value) {
        lowerLanguageTextField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);

        if (Objects.equals(value, settings.getUpperLanguage())) {
            lowerLanguageTextField.getStyleClass().add(GuiConstants.TEXT_FIELD_ERROR_CLASS);
            actionResultLabel.setError("Languages have to be different, please choose another one");
            return;
        }

        boolean hadValueBefore = settings.getLowerLanguage() != null;

        settings.saveCorrect(value, SettingType.LOWER_LANGUAGE);
        context.getMissingSettings().remove(SettingType.LOWER_LANGUAGE);
        swapButton.setDisable(settings.getUpperLanguage() == null || settings.getLowerLanguage() == null);

        String prefix = "The language for lower subtitles (" + value.toString().toUpperCase() + ") has been";
        if (hadValueBefore) {
            actionResultLabel.setSuccess(prefix + " updated successfully");
        } else {
            actionResultLabel.setSuccess(prefix + " saved successfully");
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

    private void mergeModeChanged(Toggle mergeModeToggle) {
        RadioButton radioButton = (RadioButton) mergeModeToggle;
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
    private void swapClicked() {
        LanguageAlpha3Code oldUpperLanguage = settings.getUpperLanguage();
        LanguageAlpha3Code oldLowerLanguage = settings.getLowerLanguage();

        settings.saveCorrect(oldLowerLanguage, SettingType.UPPER_LANGUAGE);
        upperLanguageTextField.setText(LANGUAGE_CODE_STRING_CONVERTER.toString(oldLowerLanguage));

        settings.saveCorrect(oldUpperLanguage, SettingType.LOWER_LANGUAGE);
        lowerLanguageTextField.setText(LANGUAGE_CODE_STRING_CONVERTER.toString(oldUpperLanguage));

        /* It will definitely have this class because the value is considered to be duplicate during the swapping. */
        upperLanguageTextField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);

        /* Lower field could be marked as incorrect before the swapping and should also be cleared. */
        lowerLanguageTextField.getStyleClass().remove(GuiConstants.TEXT_FIELD_ERROR_CLASS);

        actionResultLabel.setSuccess("The languages have been swapped successfully");
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
        @Override
        public String toString(LanguageAlpha3Code languageCode) {
            if (languageCode == null) {
                return null;
            }

            return languageCode.getName() + " (" + languageCode.toString() + ")";
        }

        @Override
        public LanguageAlpha3Code fromString(String string) {
            if (StringUtils.isBlank(string)) {
                return null;
            }

            int leftBracketIndex = string.lastIndexOf("(");
            if (leftBracketIndex == -1) {
                return null;
            }

            /* + 4 because every code is 3 symbol long. */
            if (leftBracketIndex + 4 > string.length()) {
                return null;
            }

            LanguageAlpha3Code result = LanguageAlpha3Code.getByCode(
                    string.substring(leftBracketIndex + 1, leftBracketIndex + 4)
            );
            if (result == null) {
                return null;
            }

            if (!Objects.equals(toString(result), string)) {
                return null;
            }

            return result;
        }
    }
}
