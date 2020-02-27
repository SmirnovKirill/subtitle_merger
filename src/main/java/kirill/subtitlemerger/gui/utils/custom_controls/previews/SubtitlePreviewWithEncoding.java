package kirill.subtitlemerger.gui.utils.custom_controls.previews;

import javafx.scene.layout.StackPane;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class SubtitlePreviewWithEncoding extends StackPane {
    @Getter
    SubtitlePreviewWithEncodingController controller;

    public SubtitlePreviewWithEncoding() {
        controller = GuiUtils.loadFxmlAndReturnController(
                "/gui/custom_controls/subtitlePreviewWithEncoding.fxml",
                this
        );
    }
}
