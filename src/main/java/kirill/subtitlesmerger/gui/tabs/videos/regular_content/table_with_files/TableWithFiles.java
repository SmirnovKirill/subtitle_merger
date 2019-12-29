package kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.scene.control.*;
import javafx.util.Callback;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

public class TableWithFiles extends TableView<GuiFileInfo> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm:ss");

    public TableWithFiles() {
        super();

        setContextMenu(generateContextMenu());
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
                // calling super here is very important - don't skip this!
                super.updateItem(item, empty);

                GuiFileInfo fileInfo = getTableRow().getItem();
                if (fileInfo == null) {
                    setGraphic(null);
                    return;
                }

                setText(fileInfo.getPath());
            }
        };
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
