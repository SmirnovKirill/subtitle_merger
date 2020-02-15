package kirill.subtitlemerger.gui.tabs.subtitle_files;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;

@CommonsLog
public class FileExistsDialog extends VBox {
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

    FileExistsDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/gui/tabs/subtitle_files/fileExistsDialog.fxml")
        );
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }
    }

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
