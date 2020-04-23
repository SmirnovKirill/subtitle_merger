package kirill.subtitlemerger.gui.utils.forms_and_controls;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ErrorPopupController {
    @FXML
    private Label messageLabel;

    @FXML
    private Button okButton;

    public void initialize(String message, Stage stage) {
        messageLabel.setText(message);

        okButton.setOnAction(event -> stage.close());
    }
}
