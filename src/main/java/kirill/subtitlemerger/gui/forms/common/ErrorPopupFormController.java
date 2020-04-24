package kirill.subtitlemerger.gui.forms.common;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ErrorPopupFormController {
    @FXML
    private Label messageLabel;

    @FXML
    private Button okButton;

    public void initialize(String message, Stage stage) {
        messageLabel.setText(message);

        okButton.setOnAction(event -> stage.close());
    }
}
