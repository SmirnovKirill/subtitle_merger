package kirill.subtitlemerger.gui.application_specific.videos_tab;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;

public class OverwriteFileDialogController extends VBox {
    @FXML
    private Label firstLineLabel;

    @FXML
    private Label secondLineLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private Button replaceButton;

    @Getter
    private boolean agreeToOverwrite;

    public void initialize(
            String fileName,
            String parentName,
            Stage dialogStage
    ) {
        firstLineLabel.setText("A file named " + fileName + " already exists. Do you want to replace it?");
        secondLineLabel.setText("The file already exists in " + parentName + ". Replacing will overwrite its contents");

        cancelButton.setOnAction(event -> {
            agreeToOverwrite = false;
            dialogStage.close();
        });
        replaceButton.setOnAction(event -> {
            agreeToOverwrite = true;
            dialogStage.close();
        });
    }
}
