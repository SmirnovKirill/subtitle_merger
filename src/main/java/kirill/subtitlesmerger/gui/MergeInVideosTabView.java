package kirill.subtitlesmerger.gui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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

import static kirill.subtitlesmerger.logic.data.BriefFileInfo.UnavailabilityReason.*;

class MergeInVideosTabView implements TabView {
    private static final String TAB_NAME = "Merge subtitles in videos";

    private boolean debug;

    private Tab tab;

    private VBox missingSettingsBox;

    private VBox missingSettingLabelsBox;

    private Hyperlink goToSettingsLink;

    MergeInVideosTabView(boolean debug) {
        this.debug = debug;
        this.tab = generateTab();
    }

    private Tab generateTab() {
        Tab result = new Tab(TAB_NAME);

        result.setContent(generateContentPane());

        return result;
    }

    private GridPane generateContentPane() {
        GridPane contentPane = new GridPane();

        contentPane.setHgap(55);
        contentPane.setPadding(GuiLauncher.TAB_PADDING);
        contentPane.setGridLinesVisible(debug);

        contentPane.getColumnConstraints().addAll(generateColumnConstraints());

        addRowMissingSettings(contentPane);
        addRowFileTable(contentPane);

        return contentPane;
    }

    private static List<ColumnConstraints> generateColumnConstraints() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setHgrow(Priority.ALWAYS);
        result.add(firstColumn);

        return result;
    }

    private void addRowMissingSettings(GridPane contentPane) {
        missingSettingsBox = new VBox();
        missingSettingsBox.setPadding(GuiLauncher.TAB_PADDING);
        missingSettingsBox.setSpacing(10);

        missingSettingLabelsBox = new VBox();
        missingSettingLabelsBox.setSpacing(10);

        goToSettingsLink = new Hyperlink();
        goToSettingsLink.setText("open settings tab");

        missingSettingsBox.getChildren().addAll(missingSettingLabelsBox, goToSettingsLink);

        contentPane.addRow(contentPane.getRowCount(), missingSettingsBox);
        GridPane.setColumnSpan(missingSettingsBox, contentPane.getColumnCount());
    }

    private void addRowFileTable(GridPane contentPane) {
        TableView<TableFile> tableView = new TableView<>();

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

        tableView.getColumns().add(fileNameColumn);
        tableView.getColumns().add(modificationDateColumn);

        tableView.getItems().addAll(getData());

        contentPane.addRow(contentPane.getRowCount(), tableView);
        GridPane.setColumnSpan(tableView, contentPane.getColumnCount());
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

    void showMissingSettings(
            List<String> missingSettings
    ) {
        this.missingSettingsBox.setVisible(true);
        this.missingSettingLabelsBox.getChildren().clear();

        this.missingSettingLabelsBox.getChildren().add(new Label("The following settings are missing:"));
        for (String setting : missingSettings) {
            this.missingSettingLabelsBox.getChildren().add(new Label("\u2022 " + setting));
        }
    }

    void showRegularContent() {
        this.missingSettingsBox.setVisible(false);
    }

    @AllArgsConstructor
    @Getter
    private static class TableFile {
        private String name;

        private String unavailabilityReason;

        private DateTime modificationTime;
    }
}
