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

        listView.setSelectionModel(new NoSelectionModel<>());
        encodingComboBox.getItems().setAll(GuiConstants.SUPPORTED_ENCODINGS);
    }

    public void initialize(byte[] data, Charset originalEncoding, String title, Stage dialogStage) {
        this.data = data;
        this.originalEncoding = originalEncoding;
        currentEncoding = originalEncoding;
        encodingToReturn = originalEncoding;

        encodingComboBox.getSelectionModel().select(originalEncoding);
        splitAndSetListView(new String(data, originalEncoding));
        titleLabel.setText(title);
    }

    private void splitAndSetListView(String text) {
        listView.getItems().clear();
        listView.setItems(FXCollections.observableArrayList(LogicConstants.LINE_SEPARATOR_PATTERN.split(text)));
    }

    private void setButtonActions(Stage dialogStage) {
        cancelButton.setOnAction(event -> {
            encodingToReturn = originalEncoding;
            dialogStage.close();
        });
        saveButton.setOnAction(event -> {
            encodingToReturn = currentEncoding;
            dialogStage.close();
        });
    }

    @FXML
    private void encodingChanged() {
        Charset encoding = encodingComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(encoding, currentEncoding)) {
            return;
        }

        currentEncoding = encoding;
        saveButton.setDisable(Objects.equals(currentEncoding, originalEncoding));
        splitAndSetListView(new String(data, currentEncoding));
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
