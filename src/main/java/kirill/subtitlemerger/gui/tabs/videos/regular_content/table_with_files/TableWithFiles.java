package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.FilePanes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;

import java.util.HashMap;
import java.util.Map;

@CommonsLog
public class TableWithFiles extends TableView<GuiFileInfo> {
    @Getter
    private final Map<String, FilePanes> filePanes;

    public TableWithFiles() {
        super();

        this.filePanes = new HashMap<>();

        setSelectionModel(null);
        setPlaceholder(new Label("there are no files to display"));
    }

    /*
     * Had to make this method because table is initialized with fxml and it happens after the constructor is called so
     * in the constructor columns aren't initialized yet.
     */
    //todo move everything to the constructor
    public void initialize(BooleanProperty allSelected, IntegerProperty selected, IntegerProperty allAvailableCount) {
        TableColumn<GuiFileInfo, ?> selectedColumn = getColumns().get(0);
        CheckBox selectAllCheckBox = new CheckBox();
        selectAllCheckBox.selectedProperty().bindBidirectional(allSelected);
        selectAllCheckBox.setOnAction(event -> {
            getItems().forEach(fileInfo -> fileInfo.setSelected(selectAllCheckBox.isSelected()));
            if (selectAllCheckBox.isSelected()) {
                selected.setValue(allAvailableCount.getValue());
            } else {
                selected.setValue(0);
            }
        });
        selectedColumn.setGraphic(selectAllCheckBox);
        selectedColumn.setCellFactory(this::generateSelectedCell);

        TableColumn<GuiFileInfo, ?> fileDescriptionColumn = getColumns().get(1);
        fileDescriptionColumn.setCellFactory(this::generateFileDescriptionCell);

        TableColumn<GuiFileInfo, ?> subtitlesColumn = getColumns().get(2);
        subtitlesColumn.setCellFactory(this::generateSubtitlesCell);
    }

    private <T> TableWithFilesCell<T> generateSelectedCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(FilePanes::getSelectPane, filePanes);
    }

    private <T> TableWithFilesCell<T> generateFileDescriptionCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(FilePanes::getFileDescriptionPane, filePanes);
    }

    private <T> TableWithFilesCell<T> generateSubtitlesCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(FilePanes::getSubtitlePane, filePanes);
    }


    @AllArgsConstructor
    public static class TableWithFilesCell<T> extends TableCell<GuiFileInfo, T> {
        private CellNodeGenerator cellNodeGenerator;

        private Map<String, FilePanes> filePanes;

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            GuiFileInfo fileInfo = getTableRow().getItem();

            if (empty || fileInfo == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            setGraphic(cellNodeGenerator.generateNode(filePanes.get(fileInfo.getFullPath())));
            setText(null);
        }

        @FunctionalInterface
        interface CellNodeGenerator {
            Node generateNode(FilePanes filePanes);
        }
    }
}
