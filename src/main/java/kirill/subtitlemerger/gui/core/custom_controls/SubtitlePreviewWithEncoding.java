package kirill.subtitlemerger.gui.core.custom_controls;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;

@CommonsLog
public class SubtitlePreviewWithEncoding extends StackPane {
    public SubtitlePreviewWithEncoding() {
        this(null);
    }

    public SubtitlePreviewWithEncoding(Object controller) {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/gui/custom_controls/subtitlePreviewWithEncoding.fxml")
        );
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(controller);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }
    }
}
