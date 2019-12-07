package kirill.subtitlesmerger.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.logic.data.BriefFileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class MergeInVideosTabView implements TabView {
    private static final String TAB_NAME = "Merge subtitles in videos";

    private Stage stage;

    private boolean debug;

    private Tab tab;

    private Node missingSettingsContent;

    private VBox missingSettingLabels;

    private Hyperlink goToSettingsLink;

    private Node regularContent;

    private Button directoryChooseButton;

    private Label directoryPathLabel;

    private DirectoryChooser directoryChooser;

    private Label directoryIncorrectLabel;

    private CheckBox showOnlyValidCheckBox;

    private TableView<TableFile> tableWithFiles;

    MergeInVideosTabView(Stage stage, boolean debug) {
        this.stage = stage;
        this.debug = debug;
        this.tab = new Tab(TAB_NAME);
        this.missingSettingsContent = generateMissingSettingsContent();
        this.regularContent = generateRegularContent();
    }

    private Node generateMissingSettingsContent() {
        VBox result = new VBox();

        result.setPadding(GuiLauncher.TAB_PADDING);
        result.setSpacing(10);

        missingSettingLabels = generateMissingSettingLabels();

        goToSettingsLink = new Hyperlink();
        goToSettingsLink.setText("open settings tab");

        result.getChildren().addAll(missingSettingLabels, goToSettingsLink);

        return result;
    }

    private VBox generateMissingSettingLabels() {
        VBox result = new VBox();

        result.setSpacing(10);

        return result;
    }

    private Node generateRegularContent() {
        VBox result = new VBox();

        result.setPadding(GuiLauncher.TAB_PADDING);
        result.setSpacing(10);

        Node controlsNode = generateControlsNode();
        directoryIncorrectLabel = generateDirectoryIncorrectLabel();
        tableWithFiles = generateTableWithFiles();

        result.getChildren().addAll(controlsNode, directoryIncorrectLabel, tableWithFiles);

        VBox.setVgrow(tableWithFiles, Priority.ALWAYS);

        return result;
    }

    private Node generateControlsNode() {
        GridPane result = new GridPane();

        result.setHgap(30);
        result.setVgap(40); //todo remove
        result.setGridLinesVisible(debug);

        result.getColumnConstraints().addAll(generateControlNodeColumns());

        addFirstControlsRow(result);
        addSecondControlsRow(result);

        return result;
    }

    private void addFirstControlsRow(GridPane pane) {
        Label descriptionLabel = new Label("Please choose the directory with videos");

        directoryChooseButton = new Button("Choose file");
        directoryPathLabel = new Label("not selected");
        directoryChooser = generateDirectoryChooser();

        pane.addRow(
                pane.getRowCount(),
                descriptionLabel,
                directoryChooseButton,
                directoryPathLabel
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(directoryChooseButton, HPos.RIGHT);
        GridPane.setHalignment(directoryPathLabel, HPos.LEFT);
    }

    private void addSecondControlsRow(GridPane pane) {
        HBox row = new HBox();

        row.setSpacing(20);
        row.setAlignment(Pos.CENTER_LEFT);

        showOnlyValidCheckBox = new CheckBox("Show only valid video files");

        Image image = new Image(SettingsTabView.class.getResourceAsStream("/refresh.png"));
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);
        imageView.setSmooth(true);

        Button refreshButton = new Button("Refresh", imageView);

        Button getSubtitleSizesButton = new Button("Get subtitle sizes");

        Button injectSubtitlesButton = new Button("Inject subtitles");

        row.getChildren().addAll(showOnlyValidCheckBox, refreshButton, getSubtitleSizesButton, injectSubtitlesButton);

        pane.addRow(
                pane.getRowCount(),
                row
        );

        GridPane.setHalignment(row, HPos.LEFT);
        GridPane.setColumnSpan(row, pane.getColumnCount());
    }

    private static List<ColumnConstraints> generateControlNodeColumns() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPrefWidth(400);
        firstColumn.setMinWidth(firstColumn.getPrefWidth());
        result.add(firstColumn);

        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPrefWidth(100);
        secondColumn.setMinWidth(secondColumn.getPrefWidth());
        result.add(secondColumn);

        ColumnConstraints thirdColumn = new ColumnConstraints();
        thirdColumn.setHgrow(Priority.ALWAYS);
        result.add(thirdColumn);

        return result;
    }

    private static DirectoryChooser generateDirectoryChooser() {
        DirectoryChooser result = new DirectoryChooser();

        result.setTitle("choose the directory with videos");

        return result;
    }

    private static Label generateDirectoryIncorrectLabel() {
        Label result = new Label();

        result.managedProperty().bind(result.visibleProperty());
        result.getStyleClass().add(GuiLauncher.LABEL_ERROR_CLASS);
        result.setVisible(false);

        return result;
    }

    private TableView<TableFile> generateTableWithFiles() {
        TableView<TableFile> result = new TableView<>();

        result.setPlaceholder(new Label("no files to display for this directory"));
        result.managedProperty().bind(result.visibleProperty());
        result.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        result.setVisible(false);

        TableColumn<TableFile, String> fileNameColumn = new TableColumn<>("filename");
        fileNameColumn.setCellValueFactory(cellDataFeatures -> cellDataFeatures.getValue().getFileNameProperty());
        fileNameColumn.setCellFactory(this::getFileNameCell);
        fileNameColumn.setReorderable(false);
        fileNameColumn.setResizable(true);
        fileNameColumn.setMinWidth(200);
        fileNameColumn.setMaxWidth(Double.MAX_VALUE);

        TableColumn<TableFile, String> modificationDateColumn = new TableColumn<>("modification date");
        modificationDateColumn.setCellValueFactory(cellDataFeatures -> cellDataFeatures.getValue().getModificationTimeProperty());
        modificationDateColumn.setComparator(Comparator.comparing(TableFile.FORMATTER::parseLocalDateTime));
        modificationDateColumn.setReorderable(false);
        modificationDateColumn.setResizable(false);
        modificationDateColumn.setMinWidth(150);
        modificationDateColumn.setMaxWidth(modificationDateColumn.getMinWidth());
        modificationDateColumn.setPrefWidth(modificationDateColumn.getMinWidth());

        TableColumn<TableFile, String> subtitlesColumn = new TableColumn<>("subtitles");
        subtitlesColumn.setReorderable(false);
        subtitlesColumn.setResizable(true);
        subtitlesColumn.setSortable(false);
        subtitlesColumn.setMinWidth(200);
        subtitlesColumn.setMaxWidth(Double.MAX_VALUE);

        result.getColumns().add(fileNameColumn);
        result.getColumns().add(modificationDateColumn);
        result.getColumns().add(subtitlesColumn);

        return result;
    }

    private TableCell<TableFile, String> getFileNameCell(TableColumn<TableFile, String> column) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(item);

                TableFile tableFile = getTableRow().getItem();
                if (tableFile != null) {
                    System.out.println("file is not null " + item);
                    if (!StringUtils.isBlank(tableFile.getUnavailabilityReason())) {
                        setStyle("-fx-text-fill: grey");
                        setTooltip(new Tooltip(tableFile.getUnavailabilityReason()));
                    } else {
                        setTooltip(null);
                        setStyle("-fx-text-fill: black");
                    }
                } else {
                    System.out.println("file is null " + item);
                }
            }
        };
    }

    @Override
    public String getTabName() {
        return TAB_NAME;
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    void setGoToSettingsLinkHandler(EventHandler<ActionEvent> handler) {
        goToSettingsLink.setOnAction(handler);
    }

    void setDirectoryChooseButtonHandler(EventHandler<ActionEvent> handler) {
        directoryChooseButton.setOnAction(handler);
    }

    void setDirectoryChooserInitialDirectory(File initialDirectory) {
        directoryChooser.setInitialDirectory(initialDirectory);
    }

    void setShowOnlyValidCheckBoxChangeListener(ChangeListener<Boolean> listener) {
        showOnlyValidCheckBox.selectedProperty().addListener(listener);
    }

    Optional<File> getChosenDirectory() {
        return Optional.ofNullable(directoryChooser.showDialog(stage));
    }

    void setDirectoryPathLabel(String text) {
        directoryPathLabel.setText(text);
    }

    void showDirectoryErrorMessage(String text) {
        tableWithFiles.setVisible(false);
        directoryIncorrectLabel.setVisible(true);
        directoryIncorrectLabel.setText(text);
    }

    void showTableWithFiles(List<BriefFileInfo> briefFilesInfo) {
        directoryIncorrectLabel.setVisible(false);
        tableWithFiles.setVisible(true);

        tableWithFiles.getItems().clear();
        for (BriefFileInfo briefFileInfo : briefFilesInfo) {
            tableWithFiles.getItems().add(
                    new TableFile(
                            new SimpleStringProperty(briefFileInfo.getFile().getName()),
                            new SimpleStringProperty(
                                    briefFileInfo.getUnavailabilityReason() != null
                                            ? briefFileInfo.getUnavailabilityReason().toString()
                                            : null
                            ),
                            new SimpleStringProperty(
                                    TableFile.FORMATTER.print(new LocalDateTime(briefFileInfo.getFile().lastModified()))
                            )
                    )
            );
        }
    }

    void showMissingSettings(
            List<String> missingSettings
    ) {
        missingSettingLabels.getChildren().clear();

        this.missingSettingLabels.getChildren().add(new Label("The following settings are missing:"));
        for (String setting : missingSettings) {
            this.missingSettingLabels.getChildren().add(new Label("\u2022 " + setting));
        }

        tab.setContent(missingSettingsContent);
    }

    void showRegularContent() {
        tab.setContent(regularContent);
    }

    @AllArgsConstructor
    @Getter
    private static class TableFile {
        static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm:ss");

        private StringProperty fileNameProperty;

        final void setFileName(String value) {
            fileNameProperty.set(value);
        }

        final String getFileName() {
            return fileNameProperty == null ? null : fileNameProperty.get();
        }

        private StringProperty unavailabilityReasonProperty;

        final void setUnavailabilityReason(String value) {
            unavailabilityReasonProperty.set(value);
        }

        final String getUnavailabilityReason() {
            return unavailabilityReasonProperty == null ? null : unavailabilityReasonProperty.get();
        }

        private StringProperty modificationTimeProperty;

        final void setModificationTime(String value) {
            modificationTimeProperty.set(value);
        }

        final String getModificationTime() {
            return modificationTimeProperty == null ? null : modificationTimeProperty.get();
        }
    }
}
