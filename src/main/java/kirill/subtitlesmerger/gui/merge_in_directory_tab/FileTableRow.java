package kirill.subtitlesmerger.gui.merge_in_directory_tab;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import kirill.subtitlesmerger.logic.AppContext;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlesmerger.logic.work_with_files.entities.SubtitleStream;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileTableRow {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm:ss");

    private static final String TABLE_CELL_CLASS = "file-table-cell";

    private static final String FIRST_TABLE_CELL_CLASS = "first-file-table-cell";

    private static final String LOWEST_TABLE_CELL_CLASS = "lowest-file-table-cell";

    private static final String FIRST_LOWEST_TABLE_CELL_CLASS = "first-lowest-file-table-cell";

    private FileInfo fileInfo;

    private ToggleGroup upperSubtitleToggleGroup;

    @Getter
    private List<RadioButton> upperSubtitleStreamRadioButtons;

    @Getter
    private RadioButton upperSubtitleFileRadioButton;

    private ToggleGroup lowerSubtitleToggleGroup;

    @Getter
    private List<RadioButton> lowerSubtitleStreamRadioButtons;

    @Getter
    private RadioButton lowerSubtitleFileRadioButton;

    public FileTableRow(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
            this.upperSubtitleToggleGroup = new ToggleGroup();
            this.upperSubtitleStreamRadioButtons = generateSubtitleStreamRadioButtons(
                    fileInfo,
                    upperSubtitleToggleGroup
            );
            this.upperSubtitleFileRadioButton = generateSubtitleFileRadioButton(upperSubtitleToggleGroup);
            this.lowerSubtitleToggleGroup = new ToggleGroup();
            this.lowerSubtitleStreamRadioButtons = generateSubtitleStreamRadioButtons(
                    fileInfo,
                    lowerSubtitleToggleGroup
            );
            this.lowerSubtitleFileRadioButton = generateSubtitleFileRadioButton(lowerSubtitleToggleGroup);
        }
    }

    private static List<RadioButton> generateSubtitleStreamRadioButtons(
            FileInfo fileInfo,
            ToggleGroup toggleGroup
    ) {
        List<RadioButton> result = new ArrayList<>();

        for (SubtitleStream stream : fileInfo.getSubtitleStreams()) {
            RadioButton radioButton = new RadioButton(getRadioButtonText(stream));

            radioButton.setToggleGroup(toggleGroup);

            result.add(radioButton);
        }

        RadioButton radio = new RadioButton("from file");
        radio.setToggleGroup(toggleGroup);

        return result;
    }

    private static String getRadioButtonText(SubtitleStream subtitleStream) {
        StringBuilder result = new StringBuilder();

        if (subtitleStream.getLanguage() != null) {
            result.append(subtitleStream.getLanguage());
        } else {
            result.append("unknown");
        }

        if (!StringUtils.isBlank(subtitleStream.getTitle())) {
            result.append(" ").append(subtitleStream.getTitle());
        }

        return result.toString();
    }

    private static RadioButton generateSubtitleFileRadioButton(
            ToggleGroup toggleGroup
    ) {
        RadioButton result = new RadioButton("from file");

        result.setToggleGroup(toggleGroup);

        return result;
    }

    public static Pane generateBasicInfoPane(FileInfo fileInfo, boolean lowest) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add(FIRST_TABLE_CELL_CLASS);
        if (lowest) {
            result.getStyleClass().add(FIRST_LOWEST_TABLE_CELL_CLASS);
        }
        result.setSpacing(10);

        result.getChildren().addAll(
                new Label(fileInfo.getFile().getAbsolutePath()),
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

    public static Pane generateSubtitlePane(
            FileInfo fileInfo,
            List<RadioButton> subtitleStreamRadioButtons,
            RadioButton subtitleFileRadioButton,
            boolean lowest
    ) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add(TABLE_CELL_CLASS);
        if (lowest) {
            result.getStyleClass().add(LOWEST_TABLE_CELL_CLASS);
        }
        result.setSpacing(10);

        if (!CollectionUtils.isEmpty(subtitleStreamRadioButtons)) {
            result.getChildren().addAll(subtitleStreamRadioButtons);
        }

        if (subtitleFileRadioButton != null) {
            HBox hboxFromFile = new HBox();
            hboxFromFile.setSpacing(10);
            hboxFromFile.setAlignment(Pos.CENTER_LEFT);
            hboxFromFile.getChildren().addAll(
                    subtitleFileRadioButton,
                    new Button("select")
            );

            result.getChildren().add(hboxFromFile);
        }

        return result;
    }
}
