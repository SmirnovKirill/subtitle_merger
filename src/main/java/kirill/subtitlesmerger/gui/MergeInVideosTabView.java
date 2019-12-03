package kirill.subtitlesmerger.gui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.logic.Constants;
import kirill.subtitlesmerger.logic.data.BriefFileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static kirill.subtitlesmerger.logic.data.BriefFileInfo.UnavailabilityReason.*;

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
        result.setGridLinesVisible(debug);

        result.getColumnConstraints().addAll(generateControlNodeColumns());

        Label descriptionLabel = new Label("Please choose the directory with videos");

        directoryChooseButton = new Button("Choose file");
        directoryPathLabel = new Label("not selected");
        directoryChooser = generateDirectoryChooser();

        result.addRow(
                result.getRowCount(),
                descriptionLabel,
                directoryChooseButton,
                directoryPathLabel
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(directoryChooseButton, HPos.RIGHT);
        GridPane.setHalignment(directoryPathLabel, HPos.LEFT);

        return result;
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

        result.managedProperty().bind(result.visibleProperty());
        result.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TableFile, String> fileNameColumn = new TableColumn<>("filename");
        fileNameColumn.setCellValueFactory(cellDataFeatures -> new ReadOnlyStringWrapper(cellDataFeatures.getValue().getName()));
        fileNameColumn.setCellFactory(this::getFileNameCell);
        fileNameColumn.setReorderable(false);
        fileNameColumn.setResizable(true);
        fileNameColumn.setMinWidth(200);
        fileNameColumn.setMaxWidth(Double.MAX_VALUE);

        TableColumn<TableFile, String> modificationDateColumn = new TableColumn<>("modification date");
        modificationDateColumn.setCellValueFactory(cellDataFeatures -> new ReadOnlyStringWrapper(DateTimeFormat.forPattern("dd.MM.YYYY HH:mm").print(cellDataFeatures.getValue().getModificationTime())));
        modificationDateColumn.setReorderable(false);
        modificationDateColumn.setResizable(false);
        modificationDateColumn.setMinWidth(150);
        modificationDateColumn.setMaxWidth(modificationDateColumn.getMinWidth());
        modificationDateColumn.setPrefWidth(modificationDateColumn.getMinWidth());

        result.getColumns().add(fileNameColumn);
        result.getColumns().add(modificationDateColumn);

        result.getItems().addAll(getData());

        return result;
    }

    private TableCell<TableFile, String> getFileNameCell(TableColumn<TableFile, String> column) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                System.out.println("update called " + item);

                if (empty || item == null) {
                    System.out.println("empty");
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(item);

                TableFile tableFile = getTableRow().getItem();
                if (tableFile != null && !StringUtils.isBlank(tableFile.getUnavailabilityReason())) {
                    setStyle("-fx-text-fill: grey");
                    setTooltip(new Tooltip(tableFile.getUnavailabilityReason()));
                }
            }
        };
    }

    private List<TableFile> getData() {
        File directoryWithVideos = new File("/home/user");
        File[] directoryFiles = directoryWithVideos.listFiles();

        List<BriefFileInfo> briefFilesInfo = getBriefFilesInfo(directoryFiles);

        List<TableFile> result = new ArrayList<>();

        for (BriefFileInfo briefFileInfo : briefFilesInfo) {
            result.add(
                    new TableFile(
                            briefFileInfo.getFile().getName(),
                            briefFileInfo.getUnavailabilityReason() != null
                                    ? briefFileInfo.getUnavailabilityReason().toString()
                                    : null,
                            new DateTime(briefFileInfo.getFile().lastModified())
                    )
            );
        }

        return result;
    }

    private static List<BriefFileInfo> getBriefFilesInfo(File[] files) {
        List<BriefFileInfo> result = new ArrayList<>();

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String extension = FilenameUtils.getExtension(file.getName());
            if (StringUtils.isBlank(extension)) {
                result.add(new BriefFileInfo(file, NO_EXTENSION, null, null));
                continue;
            }

            if (!Constants.ALLOWED_VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
                result.add(new BriefFileInfo(file, NOT_ALLOWED_EXTENSION, null, null));
                continue;
            }

            String mimeType;
            try {
                mimeType = Files.probeContentType(file.toPath());
            } catch (IOException e) {
                result.add(new BriefFileInfo(file, FAILED_TO_GET_MIME_TYPE, null, null));
                continue;
            }

            if (!Constants.ALLOWED_VIDEO_MIME_TYPES.contains(mimeType)) {
                result.add(new BriefFileInfo(file, NOT_ALLOWED_MIME_TYPE, null, null));
                continue;
            }

            result.add(
                    new BriefFileInfo(file, null, null, null)
            );
        }

        return result;
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

    void showTableWithFiles() {
        directoryIncorrectLabel.setVisible(false);
        tableWithFiles.setVisible(true);
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
        private String name;

        private String unavailabilityReason;

        private DateTime modificationTime;
    }
}
