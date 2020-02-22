package kirill.subtitlemerger.gui.core.custom_controls;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.Objects;

@CommonsLog
public class SimpleSubtitlePreview extends StackPane {
    @Getter
    private SimpleSubtitlePreviewController controller;

    public SimpleSubtitlePreview() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/gui/custom_controls/simpleSubtitlePreview.fxml")
        );
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }

        try {
            controller = Objects.requireNonNull(fxmlLoader.getController());
        } catch (NullPointerException e) {
            log.error("controller is not set");
            throw new IllegalStateException();
        } catch (ClassCastException e) {
            log.error("controller has an incorrect class");
            throw new IllegalStateException();
        }
    }
}
