package kirill.subtitlesmerger.gui;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import kirill.subtitlesmerger.logic.data.BriefFileInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TableWithFiles {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm:ss");

    private GridPane headerPane;

    private GridPane contentPane;

    private ScrollPane contentScrollPane;

    private VBox mainNode;

    private ObservableList<BriefFileInfo> files;

    TableWithFiles(boolean debug) {
        this.headerPane = generateHeaderPane(debug);
        this.contentPane = generateContentPane(debug);
        this.contentScrollPane = generateContentScrollPane(this.contentPane);
        this.mainNode = generateMainNode(this.headerPane, this.contentScrollPane);

        for (int i = 0; i < 3; i++) {
            this.contentPane.getColumnConstraints().get(i).prefWidthProperty().bind(((HBox) headerPane.getChildren().get(i)).widthProperty());
            this.contentPane.getColumnConstraints().get(i).minWidthProperty().bind(((HBox) headerPane.getChildren().get(i)).widthProperty());
            this.contentPane.getColumnConstraints().get(i).maxWidthProperty().bind(((HBox) headerPane.getChildren().get(i)).widthProperty());
        }

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

    private static Node generateHeaderNode(
            String title,
            boolean last
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.getChildren().add(new Label(title));
        result.getStyleClass().add("file-table-header");
        if (last) {
            result.getStyleClass().add("last-file-table-header");
        }

        return result;
    }

    private static GridPane generateContentPane(boolean debug) {
        GridPane result = new GridPane();

        result.setGridLinesVisible(debug);
        result.getColumnConstraints().addAll(generateContentColumnConstraints());

        return result;
    }

    private static List<ColumnConstraints> generateContentColumnConstraints() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints fileNameColumn = new ColumnConstraints();
        fileNameColumn.setHgrow(Priority.ALWAYS);
        result.add(fileNameColumn);

        ColumnConstraints upperSubtitlesColumn = new ColumnConstraints();
        upperSubtitlesColumn.setHgrow(Priority.ALWAYS);
        result.add(upperSubtitlesColumn);

        ColumnConstraints lowerSubtitlesColumn = new ColumnConstraints();
        lowerSubtitlesColumn.setHgrow(Priority.ALWAYS);
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
                    generateFilenameCellNode(file, lowest),
                    generateSubtitlesCellNode(file, lowest),
                    generateSubtitlesCellNode(file, lowest),
                    generateActionsCellNode(lowest)
            );

            i++;
        }
    }

    private static Node generateFilenameCellNode(BriefFileInfo fileInfo, boolean lowest) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add("first-file-table-cell");
        if (lowest) {
            result.getStyleClass().add("first-lowest-file-table-cell");
        }
        result.setSpacing(10);

        result.getChildren().addAll(
                new Label(fileInfo.getFile().getAbsolutePath()),
                new Label(FORMATTER.print(new LocalDateTime(fileInfo.getFile().lastModified())))
        );

        return result;
    }

    private static Node generateSubtitlesCellNode(BriefFileInfo fileInfo, boolean lowest) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add("file-table-cell");
        if (lowest) {
            result.getStyleClass().add("lowest-file-table-cell");
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

    private static Node generateActionsCellNode(boolean lowest) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add("file-table-cell");
        if (lowest) {
            result.getStyleClass().add("lowest-file-table-cell");
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
