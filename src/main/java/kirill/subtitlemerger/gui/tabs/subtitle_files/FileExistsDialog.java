package kirill.subtitlemerger.gui.tabs.subtitle_files;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
            EventHandler<ActionEvent> onCancelPressed,
            EventHandler<ActionEvent> onReplacePressed
    ) {
        firstLineLabel.setText("A file named " + fileName + " already exists. Do you want to replace it?");
        secondLineLabel.setText("The file already exists in " + parentName + ". Replacing will overwrite its contents");
        cancelButton.setOnAction(onCancelPressed);
        replaceButton.setOnAction(onReplacePressed);
    }
}
