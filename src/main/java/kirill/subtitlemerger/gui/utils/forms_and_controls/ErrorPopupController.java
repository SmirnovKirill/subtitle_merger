package kirill.subtitlemerger.gui.utils.forms_and_controls;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ErrorPopupController {
    @FXML
    private Label textLabel;

    @FXML
    private Button okButton;

    public void initialize(String text, Stage stage) {
        textLabel.setText(text);

        okButton.setOnAction(event -> stage.close());
    }
}
