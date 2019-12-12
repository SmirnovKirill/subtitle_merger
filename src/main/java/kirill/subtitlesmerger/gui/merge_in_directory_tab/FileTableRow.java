package kirill.subtitlesmerger.gui.merge_in_directory_tab;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiLauncher;
import kirill.subtitlesmerger.logic.AppContext;
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

    private AppContext appContext;

    private SubtitleStreamChooserCell upperStreamChooserCell;

    private SubtitleStreamChooserCell lowerStreamChooserCell;

    public FileTableRow(FileInfo fileInfo, Stage stage, AppContext appContext) {
        this.fileInfo = fileInfo;
        this.stage = stage;
        this.appContext = appContext;
        this.upperStreamChooserCell = new SubtitleStreamChooserCell(
                fileInfo,
                SubtitleStreamChooserCell.SubtitleType.UPPER,
                stage,
                appContext
        );
        this.lowerStreamChooserCell = new SubtitleStreamChooserCell(
                fileInfo,
                SubtitleStreamChooserCell.SubtitleType.LOWER,
                stage,
                appContext
        );
    }

    public Pane generateBasicInfoCellPane(boolean lowestRow) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(10);

        result.getStyleClass().add(GuiLauncher.FIRST_TABLE_CELL_CLASS);
        if (lowestRow) {
            result.getStyleClass().add(GuiLauncher.FIRST_LOWEST_TABLE_CELL_CLASS);
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
