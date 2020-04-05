package kirill.subtitlemerger.gui.util.custom_forms;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.util.GuiUtils;
import org.apache.commons.lang3.StringUtils;

public class AgreementPopupController {
    @FXML
    private Label textLabel;

    @FXML
    private CheckBox applyToAllCheckBox;

    @FXML
    private Button yesButton;

    @FXML
    private Button noButton;

    private Boolean agreed;

    public void initialize(
            String messageText,
            String yesText,
            String noText,
            String applyToAllText,
            Stage stage
    ) {
        textLabel.setText(messageText);
        yesButton.setText(yesText);
        noButton.setText(noText);

        if (StringUtils.isBlank(applyToAllText)) {
            GuiUtils.setVisibleAndManaged(applyToAllCheckBox, false);
        } else {
            applyToAllCheckBox.setText(applyToAllText);
        }

        yesButton.setOnAction(event -> {
            agreed = true;
            stage.close();
        });

        noButton.setOnAction(event -> {
            agreed = false;
            stage.close();
        });
    }

    public Result getResult() {
        if (agreed == null) {
            return Result.CANCELED;
        } else if (agreed) {
            if (applyToAllCheckBox.isSelected()) {
                return Result.YES_TO_ALL;
            } else {
                return Result.YES;
            }
        } else {
            if (applyToAllCheckBox.isSelected()) {
                return Result.NO_TO_ALL;
            } else {
                return Result.NO;
            }
        }
    }

    public enum Result {
        CANCELED,
        YES,
        YES_TO_ALL,
        NO,
        NO_TO_ALL
    }
}
