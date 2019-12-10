package kirill.subtitlesmerger.gui.merge_in_videos_tab;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import kirill.subtitlesmerger.logic.merge_in_files.entities.BriefFileInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class TableWithFiles {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm:ss");

    private static final String TABLE_HEADER_CLASS = "file-table-header";

    private static final String LAST_TABLE_HEADER_CLASS = "last-file-table-header";

    private static final String TABLE_CELL_CLASS = "file-table-cell";

    private static final String FIRST_TABLE_CELL_CLASS = "first-file-table-cell";

    private static final String LOWEST_TABLE_CELL_CLASS = "lowest-file-table-cell";

    private static final String FIRST_LOWEST_TABLE_CELL_CLASS = "first-lowest-file-table-cell";

    private static final String SCROLL_CLASS = "file-table-scroll";

    private GridPane headerPane;

    private GridPane contentPane;

    private ScrollPane contentScrollPane;

    private VBox mainNode;

    private ObservableList<BriefFileInfo> files;

    TableWithFiles(boolean debug) {
        this.headerPane = generateHeaderPane(debug);
        this.contentPane = generateContentPane(debug, this.headerPane);
        this.contentScrollPane = generateContentScrollPane(this.contentPane);
        this.mainNode = generateMainNode(this.headerPane, this.contentScrollPane);

        this.files = FXCollections.observableArrayList();
        this.files.addListener(this::filesChanged);
    }

    private static GridPane generateHeaderPane(boolean debug) {
        GridPane result = new GridPane();

        result.setGridLinesVisible(debug);
        result.getColumnConstraints().addAll(generateHeaderColumnConstraints());

        result.addRow(
                result.getRowCount(),
                generateHeaderNode("filename", false),
                generateHeaderNode("upper subtitles", false),
                generateHeaderNode("lower subtitles", false),
                generateHeaderNode("action", true)
        );

        return result;
    }

    private static List<ColumnConstraints> generateHeaderColumnConstraints() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints fileNameColumn = new ColumnConstraints();
        fileNameColumn.setPrefWidth(150);
        fileNameColumn.setMinWidth(fileNameColumn.getPrefWidth());
        fileNameColumn.setHgrow(Priority.ALWAYS);
        result.add(fileNameColumn);

        ColumnConstraints upperSubtitlesColumn = new ColumnConstraints();
        upperSubtitlesColumn.setPrefWidth(200);
        upperSubtitlesColumn.setMinWidth(upperSubtitlesColumn.getPrefWidth());
        upperSubtitlesColumn.setHgrow(Priority.ALWAYS);
        result.add(upperSubtitlesColumn);

        ColumnConstraints lowerSubtitlesColumn = new ColumnConstraints();
        lowerSubtitlesColumn.setPrefWidth(200);
        lowerSubtitlesColumn.setMinWidth(lowerSubtitlesColumn.getPrefWidth());
        lowerSubtitlesColumn.setHgrow(Priority.ALWAYS);
        result.add(lowerSubtitlesColumn);

        ColumnConstraints actionColumn = new ColumnConstraints();
        actionColumn.setPrefWidth(100);
        actionColumn.setMinWidth(upperSubtitlesColumn.getPrefWidth());
        result.add(actionColumn);

        return result;
    }

    private static Node generateHeaderNode(String title, boolean last) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER);
        result.getChildren().add(new Label(title));
        result.getStyleClass().add(TABLE_HEADER_CLASS);
        if (last) {
            result.getStyleClass().add(LAST_TABLE_HEADER_CLASS);
        }

        return result;
    }

    private static GridPane generateContentPane(boolean debug, GridPane headerPane) {
        GridPane result = new GridPane();

        result.setGridLinesVisible(debug);
        result.getColumnConstraints().addAll(generateContentColumnConstraints(headerPane));

        return result;
    }

    private static List<ColumnConstraints> generateContentColumnConstraints(GridPane headerPane) {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints fileNameColumn = new ColumnConstraints();
        HBox fileNameHeader = (HBox) headerPane.getChildren().get(0);
        /* Minus 1 because of the scroll pane outer border. */
        fileNameColumn.prefWidthProperty().bind(fileNameHeader.widthProperty().subtract(1));
        fileNameColumn.minWidthProperty().bind(fileNameHeader.widthProperty().subtract(1));
        fileNameColumn.maxWidthProperty().bind(fileNameHeader.widthProperty().subtract(1));
        result.add(fileNameColumn);

        ColumnConstraints upperSubtitlesColumn = new ColumnConstraints();
        HBox upperSubtitlesHeader = (HBox) headerPane.getChildren().get(1);
        upperSubtitlesColumn.prefWidthProperty().bind(upperSubtitlesHeader.widthProperty());
        upperSubtitlesColumn.minWidthProperty().bind(upperSubtitlesHeader.widthProperty());
        upperSubtitlesColumn.maxWidthProperty().bind(upperSubtitlesHeader.widthProperty());
        result.add(upperSubtitlesColumn);

        ColumnConstraints lowerSubtitlesColumn = new ColumnConstraints();
        HBox lowerSubtitlesHeader = (HBox) headerPane.getChildren().get(1);
        lowerSubtitlesColumn.prefWidthProperty().bind(lowerSubtitlesHeader.widthProperty());
        lowerSubtitlesColumn.minWidthProperty().bind(lowerSubtitlesHeader.widthProperty());
        lowerSubtitlesColumn.maxWidthProperty().bind(lowerSubtitlesHeader.widthProperty());
        result.add(lowerSubtitlesColumn);

        ColumnConstraints actionColumn = new ColumnConstraints();
        actionColumn.setHgrow(Priority.ALWAYS);
        result.add(actionColumn);

        return result;
    }

    private static ScrollPane generateContentScrollPane(GridPane contentPane) {
        ScrollPane result = new ScrollPane();

        result.setContent(contentPane);
        result.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        result.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        result.setFitToWidth(true);
        result.getStyleClass().add(SCROLL_CLASS);

        return result;
    }

    private static VBox generateMainNode(GridPane headerPane, ScrollPane contentScrollPane) {
        VBox result = new VBox();

        result.setSpacing(0);

        result.getChildren().addAll(headerPane, contentScrollPane);

        return result;
    }

    private void filesChanged(ListChangeListener.Change<? extends BriefFileInfo> change) {
        contentPane.getChildren().clear();

        int i = 0;
        for (BriefFileInfo file : files) {
            boolean lowest = (i == (files.size() - 1));

            contentPane.addRow(
                    contentPane.getRowCount(),
                    generateFilenameCell(file, lowest),
                    generateSubtitlesCell(file, lowest),
                    generateSubtitlesCell(file, lowest),
                    generateActionsCell(lowest)
            );

            i++;
        }
    }

    private static Node generateFilenameCell(BriefFileInfo fileInfo, boolean lowest) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add(FIRST_TABLE_CELL_CLASS);
        if (lowest) {
            result.getStyleClass().add(FIRST_LOWEST_TABLE_CELL_CLASS);
        }
        result.setSpacing(10);

        result.getChildren().addAll(
                new Label(fileInfo.getFile().getAbsolutePath()),
                new Label(FORMATTER.print(new LocalDateTime(fileInfo.getFile().lastModified())))
        );

        return result;
    }

    private static Node generateSubtitlesCell(BriefFileInfo fileInfo, boolean lowest) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add(TABLE_CELL_CLASS);
        if (lowest) {
            result.getStyleClass().add(LOWEST_TABLE_CELL_CLASS);
        }
        result.setSpacing(10);

        ToggleGroup toggleGroup = new ToggleGroup();

        List<String> languages = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fileInfo.getSubtitlesStreams())) {
            languages = fileInfo.getSubtitlesStreams().stream()
                    .map(stream -> stream.getLanguage().toString())
                    .collect(Collectors.toList());
        }
        for (String lang : languages) {
            RadioButton radio = new RadioButton(lang);
            radio.setToggleGroup(toggleGroup);

            result.getChildren().add(radio);
        }

        RadioButton radio = new RadioButton("from file");
        radio.setToggleGroup(toggleGroup);

        HBox hboxFromFile = new HBox();
        hboxFromFile.setSpacing(10);
        hboxFromFile.setAlignment(Pos.CENTER_LEFT);
        hboxFromFile.getChildren().addAll(
                radio,
                new Button("select")
        );

        result.getChildren().add(hboxFromFile);

        return result;
    }

    private static Node generateActionsCell(boolean lowest) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add(TABLE_CELL_CLASS);
        if (lowest) {
            result.getStyleClass().add(LOWEST_TABLE_CELL_CLASS);
        }
        result.setSpacing(10);

        return result;
    }

    Node getMainNode() {
        return mainNode;
    }

    public void hide() {
        headerPane.setVisible(false);
        contentScrollPane.setVisible(false);
    }

    public void show() {
        headerPane.setVisible(true);
        contentScrollPane.setVisible(true);
    }

    void setFiles(Collection<BriefFileInfo> files) {
        this.files.clear();
        this.files.addAll(files);
    }
}
