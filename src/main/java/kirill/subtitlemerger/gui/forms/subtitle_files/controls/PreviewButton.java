package kirill.subtitlemerger.gui.forms.subtitle_files.controls;

import javafx.scene.control.Button;
import kirill.subtitlemerger.gui.utils.GuiUtils;

public class PreviewButton extends Button {
    public PreviewButton() {
        GuiUtils.initializeControl(this, "/gui/javafx/forms/subtitle_files/controls/preview_button.fxml");
    }
}