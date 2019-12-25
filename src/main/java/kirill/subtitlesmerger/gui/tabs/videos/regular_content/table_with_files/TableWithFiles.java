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
