package kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiConstants;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

public class FileTableRow {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm:ss");

    private FileInfo fileInfo;

    private Stage stage;

    private GuiContext guiContext;

    private SubtitleStreamChooserCell upperStreamChooserCell;

    private SubtitleStreamChooserCell lowerStreamChooserCell;

    public FileTableRow(FileInfo fileInfo, boolean lowestRow, Stage stage, GuiContext guiContext) {
        this.fileInfo = fileInfo;
        this.stage = stage;
        this.guiContext = guiContext;
        this.upperStreamChooserCell = new SubtitleStreamChooserCell(
                fileInfo,
                lowestRow,
                SubtitleStreamChooserCell.SubtitleType.UPPER,
                stage,
                guiContext
        );
        this.lowerStreamChooserCell = new SubtitleStreamChooserCell(
                fileInfo,
                lowestRow,
                SubtitleStreamChooserCell.SubtitleType.LOWER,
                stage,
                guiContext
        );
    }

    public Pane generateBasicInfoCellPane(boolean lowestRow) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(10);

        result.getStyleClass().add(GuiConstants.FIRST_TABLE_CELL_CLASS);
        if (lowestRow) {
            result.getStyleClass().add(GuiConstants.FIRST_LOWEST_TABLE_CELL_CLASS);
        }

        result.getChildren().addAll(
                new Label(fileInfo.getFile().getName()),
                new Label("last modified: " + FORMATTER.print(fileInfo.getLastModified())),
                new Label("file size: " + getFileSizeTextual(fileInfo.getSize()))
        );

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

    public Pane generateUpperSubtitleStreamChooserCellPane(boolean lowestRow) {
        return upperStreamChooserCell.generatePane(lowestRow);
    }

    public Pane generateLowerSubtitleStreamChooserCellPane(boolean lowestRow) {
        return lowerStreamChooserCell.generatePane(lowestRow);
    }
}
