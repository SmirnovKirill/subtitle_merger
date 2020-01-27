package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import kirill.subtitlemerger.gui.GuiUtils;
import lombok.extern.apachecommons.CommonsLog;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@CommonsLog
class FileDescriptionCell extends VBox {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");

    @FXML
    private Label pathLabel;

    @FXML
    private Label sizeLabel;

    @FXML
    private Label lastModifiedLabel;

    FileDescriptionCell(GuiFileInfo fileInfo) {
        String fxmlPath = "/gui/tabs/videos/regular_content/table_with_files/fileDescriptionCell.fxml";
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        GuiUtils.loadWithUncheckedException(fxmlLoader);

        pathLabel.setText(fileInfo.getPathToDisplay());
        sizeLabel.setText(GuiUtils.getFileSizeTextual(fileInfo.getSize()));
        lastModifiedLabel.setText(FORMATTER.print(fileInfo.getLastModified()));
    }
}
