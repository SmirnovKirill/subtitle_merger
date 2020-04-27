package kirill.subtitlemerger.gui.forms.common.subtitle_preview;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.common_controls.ActionResultPane;
import kirill.subtitlemerger.gui.utils.background.BackgroundCallback;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;
import java.util.Objects;

public class EncodingPreviewFormController extends AbstractPreviewFormController {
    private static final CharsetStringConverter CHARSET_STRING_CONVERTER = new CharsetStringConverter();

    @FXML
    private ComboBox<Charset> encodingComboBox;

    @FXML
    private ActionResultPane actionResultPane;

    @FXML
    private Button saveButton;

    private boolean saveButtonPressed;

    private SubtitlesAndInput originalSubtitlesAndInput;

    private SubtitlesAndInput subtitlesAndInput;

    private Stage dialogStage;

    public void initialize(String title, SubtitlesAndInput subtitlesAndInput, Stage dialogStage) {
        this.title.setText(getShortenedTitle(title));
        encodingComboBox.setConverter(CHARSET_STRING_CONVERTER);
        encodingComboBox.getItems().setAll(LogicConstants.SUPPORTED_ENCODINGS);
        encodingComboBox.getSelectionModel().select(subtitlesAndInput.getEncoding());
        listView.setSelectionModel(new NoSelectionModel<>());
        this.dialogStage = dialogStage;

        originalSubtitlesAndInput = subtitlesAndInput;
        this.subtitlesAndInput = subtitlesAndInput;

        displayText(originalSubtitlesAndInput.getEncoding(), true);
    }

    private void displayText(Charset encoding, boolean firstRun) {
        BackgroundRunner<PreviewInfo> backgroundRunner = backgroundManager -> {
            backgroundManager.setCancellationPossible(false);
            backgroundManager.setIndeterminateProgress();
            backgroundManager.updateMessage("Preparing the text...");

            SubtitlesAndInput subtitlesAndInput;
            if (!Objects.equals(this.subtitlesAndInput.getEncoding(), encoding)) {
                subtitlesAndInput = this.subtitlesAndInput.changeEncoding(encoding);
            } else {
                subtitlesAndInput = this.subtitlesAndInput;
            }

            return new PreviewInfo(
                    subtitlesAndInput,
                    getSplitText(new String(subtitlesAndInput.getRawData(), subtitlesAndInput.getEncoding()))
            );
        };

        BackgroundCallback<PreviewInfo> callback = previewInfo -> {
            subtitlesAndInput = previewInfo.getSubtitlesAndInput();
            boolean correctFormat = previewInfo.getSubtitlesAndInput().isCorrectFormat();

            setLinesTruncated(previewInfo.getSplitText().isLinesTruncated());
            listView.setDisable(!correctFormat);
            listView.setItems(FXCollections.observableArrayList(previewInfo.getSplitText().getLines()));

            if (!correctFormat) {
                actionResultPane.setOnlyError(
                        String.format(
                                "This encoding (%s) doesn't fit or the file has an incorrect format",
                                encoding.name()
                        )
                );
            } else if (!firstRun) {
                if (Objects.equals(encoding, originalSubtitlesAndInput.getEncoding())) {
                    actionResultPane.setOnlySuccess("Encoding has been restored to the original value successfully");
                } else {
                    actionResultPane.setOnlySuccess("Encoding has been changed successfully");
                }
            }

            saveButton.setDisable(Objects.equals(encoding, originalSubtitlesAndInput.getEncoding()) || !correctFormat);
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void encodingChanged() {
        Charset encoding = encodingComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(encoding, subtitlesAndInput.getEncoding())) {
            return;
        }

        displayText(encoding, false);
    }

    @FXML
    private void cancelButtonClicked() {
        dialogStage.close();
    }

    @FXML
    private void saveButtonClicked() {
        saveButtonPressed = true;
        dialogStage.close();
    }

    public SubtitlesAndInput getSelection() {
        if (saveButtonPressed) {
            return subtitlesAndInput;
        } else {
            return originalSubtitlesAndInput;
        }
    }

    private static class CharsetStringConverter extends StringConverter<Charset> {
        @Override
        public String toString(Charset charset) {
            return charset.name();
        }

        @Override
        public Charset fromString(String name) {
            return Charset.forName(name);
        }
    }

    @AllArgsConstructor
    @Getter
    private static class PreviewInfo {
        private SubtitlesAndInput subtitlesAndInput;

        private SplitText splitText;
    }
}