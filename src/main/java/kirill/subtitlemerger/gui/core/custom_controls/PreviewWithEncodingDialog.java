package kirill.subtitlemerger.gui.core.custom_controls;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.core.NoSelectionModel;
import kirill.subtitlemerger.logic.LogicConstants;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

@CommonsLog
public class PreviewWithEncodingDialog extends VBox {
    private static final CharsetStringConverter CHARSET_STRING_CONVERTER = new CharsetStringConverter();

    @FXML
    private Label titleLabel;

    @FXML
    private ComboBox<Charset> encodingComboBox;

    @FXML
    private MultiColorResultLabels resultLabels;

    @FXML
    private ListView<String> listView;

    @FXML
    private Button cancelButton;

    @FXML
    private Button saveButton;

    private byte[] data;

    private Charset originalEncoding;

    private Charset currentEncoding;

    @Getter
    private Charset encodingToReturn;

    private Stage dialogStage;

    public PreviewWithEncodingDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/gui/custom_controls/previewWithEncodingDialog.fxml")
        );
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }

        encodingComboBox.setConverter(CHARSET_STRING_CONVERTER);
        encodingComboBox.getItems().setAll(GuiConstants.SUPPORTED_ENCODINGS);
        listView.setSelectionModel(new NoSelectionModel<>());
    }

    public void initialize(byte[] data, Charset originalEncoding, String title, Stage dialogStage) {
        this.data = data;
        this.originalEncoding = originalEncoding;
        currentEncoding = originalEncoding;
        encodingToReturn = originalEncoding;
        this.dialogStage = dialogStage;

        titleLabel.setText(title);
        encodingComboBox.getSelectionModel().select(originalEncoding);
        showContent(false);
    }

    private void showContent(boolean showMessageIfSuccess) {
        listView.getItems().clear();

        String text = new String(data, currentEncoding);
        String[] lines = LogicConstants.LINE_SEPARATOR_PATTERN.split(text);
        if (Arrays.stream(lines).anyMatch(line -> line.length() > 1000)) {
            listView.setDisable(true);
            listView.setItems(
                    FXCollections.observableArrayList(
                            Collections.singletonList("Unfortunately, preview is unavailable")
                    )
            );

            resultLabels.setOnlyError(
                    String.format(
                            "This encoding (%s) doesn't fit or the file has an incorrect format",
                            currentEncoding.name()
                    )
            );
        } else {
            listView.setDisable(false);
            listView.setItems(FXCollections.observableArrayList(LogicConstants.LINE_SEPARATOR_PATTERN.split(text)));

            if (showMessageIfSuccess) {
                if (Objects.equals(currentEncoding, originalEncoding)) {
                    resultLabels.setOnlySuccess("Encoding has been restored to the original value successfully");
                } else {
                    resultLabels.setOnlySuccess("Encoding has been changed successfully");
                }
            }
        }
    }

    @FXML
    private void encodingChanged() {
        Charset encoding = encodingComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(encoding, currentEncoding)) {
            return;
        }

        currentEncoding = encoding;
        showContent(true);
        saveButton.setDisable(Objects.equals(currentEncoding, originalEncoding));
    }

    @FXML
    private void cancelButtonClicked() {
        encodingToReturn = originalEncoding;
        dialogStage.close();
    }

    @FXML
    private void saveButtonClicked() {
        encodingToReturn = currentEncoding;
        dialogStage.close();
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
}
