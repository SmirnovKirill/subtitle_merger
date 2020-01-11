package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import kirill.subtitlemerger.gui.GuiUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Collections;

public class TableWithFiles extends TableView<GuiFileInfo> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");

    private static final String ROW_UNAVAILABLE_CLASS = "row-unavailable";

    public TableWithFiles() {
        super();

        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setPlaceholder(new Label("there are no files to display"));

        setRowFactory(this::generateRow);
    }

    private TableRow<GuiFileInfo> generateRow(TableView<GuiFileInfo> tableView) {
        return new TableRow<>() {
            @Override
            protected void updateItem(GuiFileInfo fileInfo, boolean empty){
                super.updateItem(fileInfo, empty);

                if (empty || fileInfo == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                if (StringUtils.isBlank(fileInfo.getUnavailabilityReason())) {
                    getStyleClass().removeAll(Collections.singleton(ROW_UNAVAILABLE_CLASS));
                } else {
                    if (!getStyleClass().contains(ROW_UNAVAILABLE_CLASS)) {
                        getStyleClass().add(ROW_UNAVAILABLE_CLASS);
                    }
                }
            }
        };
    }

    /*
     * Had to make this method because table is initialized with fxml and it happens after the constructor is called so
     * in the constructor columns aren't initialized yet.
     */
    public void initialize() {
        TableColumn<GuiFileInfo, ?> selectedColumn = getColumns().get(0);
        selectedColumn.setGraphic(new CheckBox());
        selectedColumn.setCellFactory(this::generateSelectedCell);

        TableColumn<GuiFileInfo, ?> fileDescriptionColumn = getColumns().get(1);
        fileDescriptionColumn.setCellFactory(this::generateFileDescriptionCell);

        TableColumn<GuiFileInfo, ?> upperSubtitlesColumn = getColumns().get(2);
        upperSubtitlesColumn.setCellFactory(this::generateUpperSubtitlesCell);
    }

    private <T> TableWithFilesCell<T> generateSelectedCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(TableWithFiles::generateSelectedCellPane);
    }

    private static Pane generateSelectedCellPane(GuiFileInfo fileInfo) {
        HBox result = new HBox();

        result.setAlignment(Pos.TOP_LEFT);

        result.getChildren().add(new CheckBox());

        return result;
    }

    private <T> TableWithFilesCell<T> generateFileDescriptionCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(TableWithFiles::generateFileDescriptionCellPane);
    }

    private static Pane generateFileDescriptionCellPane(GuiFileInfo fileInfo) {
        VBox result = new VBox();

        result.setPadding(new Insets(3));
        result.setSpacing(10);

        Label pathLabel = new Label(fileInfo.getPath());
        pathLabel.getStyleClass().add("path-label");
        if (!StringUtils.isBlank(fileInfo.getUnavailabilityReason())) {
            pathLabel.setTooltip(generateUnavailableTooltip(fileInfo.getUnavailabilityReason()));
        }

        Pane paneWithSizeAndLastModifiedTime = generatePaneWithSizeAndLastModifiedTime(fileInfo);

        result.getChildren().addAll(pathLabel, paneWithSizeAndLastModifiedTime);

        return result;
    }

    private static Tooltip generateUnavailableTooltip(String text) {
        Tooltip result = new Tooltip(text);

        result.setShowDelay(Duration.ZERO);
        result.setShowDuration(Duration.INDEFINITE);

        return result;
    }

    private static Pane generatePaneWithSizeAndLastModifiedTime(GuiFileInfo fileInfo) {
        GridPane result = new GridPane();

        Label sizeTitle = new Label("size");

        Label lastModifiedTitle = new Label("last modified");

        Label size = new Label(GuiUtils.getFileSizeTextual(fileInfo.getSize()));

        Label lastModified = new Label(FORMATTER.print(fileInfo.getLastModified()));

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

    private <T> TableWithFilesCell<T> generateUpperSubtitlesCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(TableWithFiles::generateUpperSubtitlesCellPane);
    }

    private static Pane generateUpperSubtitlesCellPane(GuiFileInfo fileInfo) {
        VBox result = new VBox();

        result.setSpacing(2);

        if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
            return result;
        }

        for (GuiSubtitleStreamInfo subtitleStreamInfo : fileInfo.getSubtitleStreamsInfo()) {
            HBox subtitleStreamPane = new HBox();

            RadioButton subtitleRadioButton = new RadioButton();

            subtitleRadioButton.getStyleClass().add("subtitle-radio");

            String text = subtitleStreamInfo.getLanguage();
            if (!StringUtils.isBlank(subtitleStreamInfo.getTitle())) {
                text += " (" + subtitleStreamInfo.getTitle() + ")";
            }

            subtitleRadioButton.setText(text);

            Region spacer = new Region();

            HBox sizePane = new HBox();
            HBox.setHgrow(sizePane, Priority.NEVER);
            sizePane.setSpacing(5);
            sizePane.setAlignment(Pos.CENTER);

            Label sizeLabel = new Label("Size: ? KB ");

            Hyperlink getSizeLink = new Hyperlink("get size");

            sizePane.getChildren().addAll(sizeLabel, getSizeLink);

            subtitleStreamPane.getChildren().addAll(subtitleRadioButton, spacer, sizePane);
            HBox.setHgrow(spacer, Priority.ALWAYS);

            result.getChildren().add(subtitleStreamPane);
        }

        return result;
    }
}
