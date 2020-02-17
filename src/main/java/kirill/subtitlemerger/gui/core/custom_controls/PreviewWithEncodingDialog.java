package kirill.subtitlemerger.gui.core.custom_controls;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.core.NoSelectionModel;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;

@CommonsLog
public class PreviewWithEncodingDialog extends VBox {
    private static final CharsetStringConverter CHARSET_STRING_CONVERTER = new CharsetStringConverter();

    @FXML
    private Label descriptionLabel;

    @FXML
    private ComboBox<Charset> encodingComboBox;

    @FXML
    private ListView<String> listView;

    @FXML
    private MultiColorResultLabels resultLabels;

    private byte[] data;

    private Charset encoding;

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

    public void initializeInputFile(byte[] data, Charset encoding, String filePath) {
        this.data = data;
        this.encoding = encoding;
        encodingComboBox.getSelectionModel().select(encoding);

        splitAndSetListView(new String(data, encoding));
    }

    @FXML
    private void encodingChanged() {
        Charset encoding = encodingComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(encoding, this.encoding)) {
            return;
        }

        this.encoding = encoding;
        splitAndSetListView(new String(data, encoding));
    }

    public void initializeResult(String result, String upperSubtitleName, String lowerSubtitleName) {
        splitAndSetListView(result);

        descriptionLabel.setText(
                "This is the result of merging\n"
                        + "\u2022 " + upperSubtitleName
                        + "\u2022 " + lowerSubtitleName
        );
    }

    private void splitAndSetListView(String text) {
        listView.getItems().clear();
        listView.setItems(FXCollections.observableArrayList(text.split("\\r?\\n")));
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
