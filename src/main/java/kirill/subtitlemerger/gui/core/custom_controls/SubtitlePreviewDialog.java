package kirill.subtitlemerger.gui.core.custom_controls;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.Charset;

@CommonsLog
public class SubtitlePreviewDialog extends VBox {
    @FXML
    private TextArea textArea;

    private byte[] data;

    private Charset charset;

    public SubtitlePreviewDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/gui/custom_controls/subtitlePreviewDialog.fxml")
        );
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }
    }

    public void initialize(byte[] data, Charset charset) {
        this.data = data;
        this.charset = charset;

        textArea.setText(new String(data, charset));
    }
}
