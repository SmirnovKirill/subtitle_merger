package kirill.subtitlemerger.gui.utils.forms_and_controls;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import org.apache.commons.lang3.StringUtils;

public class AgreementPopupController {
    @FXML
    private Label messageLabel;

    @FXML
    private CheckBox applyToAllCheckBox;

    @FXML
    private Button yesButton;

    @FXML
    private Button noButton;

    private Boolean agreed;

    public void initialize(String message, String applyToAllText, String yesText, String noText, Stage stage) {
        messageLabel.setText(message);

        if (StringUtils.isBlank(applyToAllText)) {
            GuiUtils.setVisibleAndManaged(applyToAllCheckBox, false);
        } else {
            applyToAllCheckBox.setText(applyToAllText);
        }

        yesButton.setText(yesText);
        yesButton.setOnAction(event -> {
            agreed = true;
            stage.close();
        });

        noButton.setText(noText);
        noButton.setOnAction(event -> {
            agreed = false;
            stage.close();
        });
    }

    public AgreementResult getResult() {
        if (agreed == null) {
            return AgreementResult.CANCELED;
        } else if (agreed) {
            if (applyToAllCheckBox.isSelected()) {
                return AgreementResult.YES_TO_ALL;
            } else {
                return AgreementResult.YES;
            }
        } else {
            if (applyToAllCheckBox.isSelected()) {
                return AgreementResult.NO_TO_ALL;
            } else {
                return AgreementResult.NO;
            }
        }
    }
}
