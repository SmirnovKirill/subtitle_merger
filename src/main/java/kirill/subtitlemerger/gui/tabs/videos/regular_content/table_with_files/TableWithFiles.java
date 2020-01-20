package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import kirill.subtitlemerger.gui.GuiUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.Collections;

public class TableWithFiles extends TableView<GuiFileInfo> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");

    private static final String ROW_UNAVAILABLE_CLASS = "row-unavailable";

    private AllFileSubtitleSizesLoader allSizesLoader;

    private SingleFileSubtitleSizeLoader singleSizeLoader;

    private BooleanProperty allSelected;

    private LongProperty selected;

    public TableWithFiles() {
        super();

        this.allSelected = new SimpleBooleanProperty(false);
        this.selected = new SimpleLongProperty(0);

        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setPlaceholder(new Label("there are no files to display"));

        setRowFactory(this::generateRow);
    }

    private TableRow<GuiFileInfo> generateRow(TableView<GuiFileInfo> tableView) {
        return new TableRow<>() {
            @Override
            protected void updateItem(GuiFileInfo fileInfo, boolean empty){
                super.updateItem(fileInfo, empty);

                if (fileInfo == null) {
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
    //todo move everything to the constructor
    public void initialize(
            AllFileSubtitleSizesLoader allSizesLoader,
            SingleFileSubtitleSizeLoader singleSizeLoader
    ) {
        this.allSizesLoader = allSizesLoader;
        this.singleSizeLoader = singleSizeLoader;

        TableColumn<GuiFileInfo, ?> selectedColumn = getColumns().get(0);
        CheckBox selectAllCheckBox = new CheckBox();
        selectAllCheckBox.selectedProperty().bindBidirectional(allSelected);
        selectAllCheckBox.setOnAction(event -> {
            getItems().forEach(fileInfo -> fileInfo.setSelected(selectAllCheckBox.isSelected()));
            if (selectAllCheckBox.isSelected()) {
                selected.setValue(getItems().size());
            } else {
                selected.setValue(0);
            }
        });
        selectedColumn.setGraphic(selectAllCheckBox);
        selectedColumn.setCellFactory(this::generateSelectedCell);

        TableColumn<GuiFileInfo, ?> fileDescriptionColumn = getColumns().get(1);
        fileDescriptionColumn.setCellFactory(this::generateFileDescriptionCell);

        TableColumn<GuiFileInfo, ?> subtitlesColumn = getColumns().get(2);
        subtitlesColumn.setCellFactory(this::generateSubtitlesCell);
    }

    private <T> TableWithFilesCell<T> generateSelectedCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(this::generateSelectedCellPane);
    }

    private Pane generateSelectedCellPane(GuiFileInfo fileInfo) {
        HBox result = new HBox();

        result.setAlignment(Pos.TOP_LEFT);

        if (!StringUtils.isBlank(fileInfo.getUnavailabilityReason())) {
            return result;
        }

        CheckBox checkBox = new CheckBox();

        checkBox.selectedProperty().bindBidirectional(fileInfo.selectedProperty());
        checkBox.setOnAction((event) -> {
            if (checkBox.isSelected()) {
                selected.set(selected.getValue() + 1);
            } else {
                selected.set(selected.getValue() - 1);
            }
            allSelected.set(!CollectionUtils.isEmpty(getItems()) && selected.getValue() == getItems().size());
        });

        result.getChildren().add(checkBox);

        return result;
    }

    private <T> TableWithFilesCell<T> generateFileDescriptionCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(this::generateFileDescriptionCellPane);
    }

    private Pane generateFileDescriptionCellPane(GuiFileInfo fileInfo) {
        VBox result = new VBox();

        result.setPadding(new Insets(3, 5, 3, 3));
        result.setSpacing(10);

        Label pathLabel = new Label(fileInfo.getPathToDisplay());
        pathLabel.getStyleClass().add("path-label");
        if (!StringUtils.isBlank(fileInfo.getUnavailabilityReason())) {
            pathLabel.setTooltip(GuiUtils.generateTooltip(fileInfo.getUnavailabilityReason()));
        }

        Pane paneWithSizeAndLastModifiedTime = generatePaneWithSizeAndLastModifiedTime(fileInfo);

        result.getChildren().addAll(pathLabel, paneWithSizeAndLastModifiedTime);

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

    private <T> TableWithFilesCell<T> generateSubtitlesCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(this::generateSubtitlesCellPane);
    }

    private Pane generateSubtitlesCellPane(GuiFileInfo fileInfo) {
        GridPane result = new GridPane();

        result.setPadding(new Insets(3, 3, 3, 5));

        if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
            return result;
        }

        Pane hiddenPane = generateHiddenPane();

        Hyperlink getAllSizes = new Hyperlink("get all sizes");
        getAllSizes.setOnAction(event -> allSizesLoader.load(fileInfo));

        result.addRow(result.getRowCount(), hiddenPane, getAllSizes, new Region());

        GridPane.setColumnSpan(getAllSizes, 2);
        GridPane.setHalignment(getAllSizes, HPos.CENTER);

        GridPane.setMargin(hiddenPane, new Insets(0, 0, 10, 0));
        GridPane.setMargin(getAllSizes, new Insets(0, 5, 10, 0));
        GridPane.setMargin(result.getChildren().get(2), new Insets(0, 0, 10, 0));

        for (GuiSubtitleStreamInfo subtitleStreamInfo : fileInfo.getSubtitleStreamsInfo()) {
            if (!Arrays.asList("rus", "eng").contains(subtitleStreamInfo.getLanguage())) {
                continue;
            }

            HBox titlePane = new HBox();

            Label language = new Label(subtitleStreamInfo.getLanguage().toUpperCase());
            titlePane.getChildren().add(language);

            if (!StringUtils.isBlank(subtitleStreamInfo.getTitle())) {
                titlePane.getChildren().add(new Label(" (" + subtitleStreamInfo.getTitle() + ")"));
            }

            Label sizeLabel = new Label();

            StringBinding unknownSizeBinding = Bindings.createStringBinding(
                    () -> "Size: ? KB ", subtitleStreamInfo.sizeProperty()
            );
            StringBinding knownSizeBinding = Bindings.createStringBinding(
                    () -> "Size: " + GuiUtils.getFileSizeTextual(subtitleStreamInfo.getSize()), subtitleStreamInfo.sizeProperty()
            );

            sizeLabel.textProperty().bind(
                    Bindings.when(subtitleStreamInfo.sizeProperty().isEqualTo(0))
                            .then(unknownSizeBinding)
                            .otherwise(knownSizeBinding)
            );

            Hyperlink getSizeLink = new Hyperlink("get size");

            HBox radios = new HBox();
            radios.setSpacing(10);
            radios.setAlignment(Pos.CENTER);

            ToggleGroup toggleGroup = new ToggleGroup();

            RadioButton upper = new RadioButton("upper");
            upper.setToggleGroup(toggleGroup);
            RadioButton lower = new RadioButton("lower");
            lower.setToggleGroup(toggleGroup);

            radios.getChildren().addAll(upper, lower);

            result.addRow(result.getRowCount(), titlePane, sizeLabel, getSizeLink, radios);
            GridPane.setHgrow(titlePane, Priority.ALWAYS);

            int bottomMargin = 2;

            titlePane.getStyleClass().add("dott2ed");
            sizeLabel.getStyleClass().add("dott2ed");
            getSizeLink.getStyleClass().add("dott2ed");
            radios.getStyleClass().add("dot2ted");

            GridPane.setMargin(titlePane, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(sizeLabel, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(getSizeLink, new Insets(0, 5, bottomMargin, 0));
            GridPane.setMargin(radios, new Insets(0, 0, bottomMargin, 0));
        }

        Button button = new Button("Add subtitle file");
        button.getStyleClass().add("add-subtitle-file");
        Image image = new Image("/gui/icons/add.png");
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(8);
        imageView.setFitHeight(imageView.getFitWidth());
        button.setGraphic(imageView);

        result.addRow(result.getRowCount(), button);

        GridPane.setColumnSpan(button, 4);
        GridPane.setMargin(button, new Insets(5, 0, 0, 0));

        return result;
    }

    private static Pane generateHiddenPane() {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);

        result.getChildren().addAll(
                new Label("8 hidden "),
                new Hyperlink("show all")
        );

        return result;
    }

    public void setAllSelected(boolean allSelected) {
        this.allSelected.set(allSelected);
    }

    public long getSelected() {
        return selected.get();
    }

    public LongProperty selectedProperty() {
        return selected;
    }

    public void setSelected(long selected) {
        this.selected.set(selected);
    }

    @FunctionalInterface
    public interface AllFileSubtitleSizesLoader {
        void load(GuiFileInfo guiFileInfo);
    }

    @FunctionalInterface
    public interface SingleFileSubtitleSizeLoader {
        void load(GuiFileInfo guiFileInfo, int subtitleIndex);
    }
}
