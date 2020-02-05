package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

@CommonsLog
public class TableWithFiles extends TableView<GuiFileInfo> {
    private static final String ROW_UNAVAILABLE_CLASS = "row-unavailable";

    private BooleanProperty allSelected;

    private LongProperty selected;

    @Setter
    private int allAvailableCount;

    public TableWithFiles() {
        super();

        this.allSelected = new SimpleBooleanProperty(false);
        this.selected = new SimpleLongProperty(0);

        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setPlaceholder(new Label("there are no files to display"));

        setRowFactory(this::generateRow);
    }

    private TableRow<GuiFileInfo> generateRow(TableView<GuiFileInfo> tableView) {
        return new TableRow<>() {
            @Override
            protected void updateItem(GuiFileInfo fileInfo, boolean empty){
                super.updateItem(fileInfo, empty);

                if (fileInfo == null) {
                    return;
                }

                getStyleClass().remove(ROW_UNAVAILABLE_CLASS);
                if (!StringUtils.isBlank(fileInfo.getUnavailabilityReason())) {
                    getStyleClass().add(ROW_UNAVAILABLE_CLASS);
                }
            }
        };
    }

    /*
     * Had to make this method because table is initialized with fxml and it happens after the constructor is called so
     * in the constructor columns aren't initialized yet.
     */
    //todo move everything to the constructor
    public void initialize() {

        TableColumn<GuiFileInfo, ?> selectedColumn = getColumns().get(0);
        CheckBox selectAllCheckBox = new CheckBox();
        selectAllCheckBox.selectedProperty().bindBidirectional(allSelected);
        selectAllCheckBox.setOnAction(event -> {
            getItems().forEach(fileInfo -> fileInfo.setSelected(selectAllCheckBox.isSelected()));
            if (selectAllCheckBox.isSelected()) {
                selected.setValue(allAvailableCount);
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
        return new TableWithFilesCell<>(GuiFileInfo::getSelectPane);
    }

    private <T> TableWithFilesCell<T> generateFileDescriptionCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(GuiFileInfo::getFileDescriptionPane);
    }

    private <T> TableWithFilesCell<T> generateSubtitlesCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(GuiFileInfo::getSubtitlePane);
    }


    public void setAllSelected(boolean allSelected) {
        this.allSelected.set(allSelected);
    }

    public long getSelected() {
        return selected.get();
    }

    public LongProperty selectedProperty() {
        return selected;
    }

    public void setSelected(long selected) {
        this.selected.set(selected);
    }

    @FunctionalInterface
    public interface AllFileSubtitleSizesLoader {
        void load(GuiFileInfo guiFileInfo);
    }

    @FunctionalInterface
    public interface SingleFileSubtitleSizeLoader {
        void load(GuiFileInfo guiFileInfo, int ffmpegIndex);
    }

    @FunctionalInterface
    public interface AddExternalSubtitleFileHandler {
        void buttonClicked(GuiFileInfo guiFileInfo);
    }

    @FunctionalInterface
    public interface RemoveExternalSubtitleFileHandler {
        void buttonClicked(int index, GuiFileInfo guiFileInfo);
    }

    @AllArgsConstructor
    public static class TableWithFilesCell<T> extends TableCell<GuiFileInfo, T> {
        private CellNodeGenerator cellNodeGenerator;

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            GuiFileInfo fileInfo = getTableRow().getItem();

            if (empty || fileInfo == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            setGraphic(cellNodeGenerator.generateNode(fileInfo));
            setText(null);
        }

        @FunctionalInterface
        interface CellNodeGenerator {
            Node generateNode(GuiFileInfo fileInfo);
        }
    }
}
