package kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

public class TableWithFiles extends TableView<GuiFileInfo> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");

    public TableWithFiles() {
        super();

        setContextMenu(generateContextMenu());
    }

    private static ContextMenu generateContextMenu() {
        ContextMenu result = new ContextMenu();

        Menu menu = new Menu("_Sort files");

        menu.getItems().addAll(
                new MenuItem("By _Name"),
                new MenuItem("By _Modification Time"),
                new MenuItem("By _Size"),
                new SeparatorMenuItem(),
                new MenuItem("_Ascending"),
                new MenuItem("_Descending")
        );

        result.getItems().add(menu);

        return result;
    }

    /*
     * Had to make this method because table is initialized with fxml and it happens after the constructor is called so
     * in the constructor columns aren't initialized yet.
     */
    public void initialize() {
        TableColumn<GuiFileInfo, ?> column = getColumns().get(0);

        column.setCellFactory(this::generateFileNameCell);
    }

    private <T> TableCell<GuiFileInfo, T> generateFileNameCell(TableColumn<GuiFileInfo, T> column) {
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

        Label size = new Label(getFileSizeTextual(fileInfo.getSize()));
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

    private static String getFileSizeTextual(long size) {
        List<String> sizes = Arrays.asList("B", "KB", "MB", "GB", "TB");

        BigDecimal divisor = new BigDecimal(1024);
        BigDecimal sizeBigDecimal = new BigDecimal(size);

        int i = 0;
        do {
            if (sizeBigDecimal.compareTo(divisor) < 0) {
                return sizeBigDecimal + " " + sizes.get(i);
            }

            sizeBigDecimal = sizeBigDecimal.divide(divisor, 2, RoundingMode.HALF_UP);
            i++;
        } while (i < sizes.size());

        return sizeBigDecimal + " " + sizes.get(sizes.size() - 1);
    }
}
