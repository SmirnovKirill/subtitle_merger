package kirill.subtitlemerger.gui.util.custom_forms;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.Getter;

public class AgreementPopupController {
    @FXML
    private Label textLabel;

    @FXML
    private Button yesButton;

    @FXML
    private Button noButton;

    @Getter
    private Boolean agreed;

    public void initialize(String messageText, String yesText, String noText, Stage stage) {
        textLabel.setText(messageText);
        yesButton.setText(yesText);
        noButton.setText(noText);

        yesButton.setOnAction(event -> {
            agreed = true;
            stage.close();
        });

        noButton.setOnAction(event -> {
            agreed = false;
            stage.close();
        });
    }
}
