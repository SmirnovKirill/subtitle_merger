package kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import kirill.subtitlesmerger.gui.GuiUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TableWithFiles extends TableView<GuiFileInfo> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");

    public TableWithFiles() {
        super();

        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setPlaceholder(new Label("there are no files to display"));
    }

    /*
     * Had to make this method because table is initialized with fxml and it happens after the constructor is called so
     * in the constructor columns aren't initialized yet.
     */
    public void initialize() {
        TableColumn<GuiFileInfo, ?> column = getColumns().get(0);
        column.setCellFactory(TableWithFiles::generateFileNameCell);
    }

    private static <T> TableCell<GuiFileInfo, T> generateFileNameCell(TableColumn<GuiFileInfo, T> column) {
        return new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                GuiFileInfo fileInfo = getTableRow().getItem();

                if (empty || fileInfo == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                setGraphic(generateFileNameCellPane(fileInfo));
                setText(null);
            }
        };
    }

    private static Pane generateFileNameCellPane(GuiFileInfo fileInfo) {
        VBox result = new VBox();

        result.setPadding(new Insets(3));
        result.setSpacing(10);

        Label pathLabel = new Label(fileInfo.getPath());
        Pane paneWithSizeAndLastModifiedTime = generatePaneWithSizeAndLastModifiedTime(fileInfo);

        result.getChildren().addAll(pathLabel, paneWithSizeAndLastModifiedTime);

        return result;
    }

    private static Pane generatePaneWithSizeAndLastModifiedTime(GuiFileInfo fileInfo) {
        GridPane result = new GridPane();

        Label sizeTitle = new Label("size");
        sizeTitle.getStyleClass().add("size-title");

        Label lastModifiedTitle = new Label("last modified");
        lastModifiedTitle.getStyleClass().add("last-modified-title");

        Label size = new Label(GuiUtils.getFileSizeTextual(fileInfo.getSize()));
        size.getStyleClass().add("size");

        Label lastModified = new Label(FORMATTER.print(fileInfo.getLastModified()));
        lastModified.getStyleClass().add("last-modified");

        result.addRow(0, sizeTitle, new Region(), size);
        result.addRow(1, lastModifiedTitle, new Region(), lastModified);

        /* Regions in the middle. */
        GridPane.setHgrow(result.getChildren().get(1), Priority.ALWAYS);
        GridPane.setHgrow(result.getChildren().get(4), Priority.ALWAYS);

        /* Labels of the first row. */
        GridPane.setMargin(result.getChildren().get(0), new Insets(0, 0, 3, 0));
        GridPane.setMargin(result.getChildren().get(2), new Insets(0, 0, 3, 0));

        return result;
    }
}
