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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiUtils;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;

public class TableWithFiles extends TableView<GuiFileInfo> {
    private static final String ROW_UNAVAILABLE_CLASS = "row-unavailable";

    private AllFileSubtitleSizesLoader allSizesLoader;

    private SingleFileSubtitleSizeLoader singleSizeLoader;

    private BooleanProperty allSelected;

    private LongProperty selected;

    @Setter
    private int allAvailableCount;

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
                selected.setValue(allAvailableCount);
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
            allSelected.set(selected.getValue() == allAvailableCount);
        });

        result.getChildren().add(checkBox);

        return result;
    }

    private <T> TableWithFilesCell<T> generateFileDescriptionCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(FileDescriptionCell::new);
    }

    private <T> TableWithFilesCell<T> generateSubtitlesCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(this::generateSubtitlesCellPane);
    }

    private Pane generateSubtitlesCellPane(GuiFileInfo fileInfo) {
        GridPane result = new GridPane();

        result.setGridLinesVisible(GuiConstants.DEBUG);

        result.setPadding(new Insets(3, 3, 3, 5));

        if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
            return result;
        }

        Pane hiddenPane = generateHiddenPane(fileInfo);

        Hyperlink getAllSizes = new Hyperlink("get all sizes");
        getAllSizes.setOnAction(event -> allSizesLoader.load(fileInfo));
        getAllSizes.visibleProperty().bind(fileInfo.haveSubtitleSizesToLoadProperty());
        getAllSizes.managedProperty().bind(fileInfo.haveSubtitleSizesToLoadProperty());

        result.add(hiddenPane, 0, 0);
        result.add(getAllSizes, 1, 0);
        GridPane.setColumnSpan(getAllSizes, 2);
        result.add(new Region(), 3, 0);

        GridPane.setHalignment(getAllSizes, HPos.CENTER);

        GridPane.setMargin(hiddenPane, new Insets(0, 0, 10, 0));
        GridPane.setMargin(getAllSizes, new Insets(0, 5, 10, 0));
        GridPane.setMargin(result.getChildren().get(2), new Insets(0, 0, 10, 0));

        int streamIndex = 0;
        for (GuiSubtitleStream stream : fileInfo.getSubtitleStreams()) {
            HBox titlePane = new HBox();

            Label language = new Label(stream.getLanguage().toUpperCase());
            titlePane.getChildren().add(language);

            if (!StringUtils.isBlank(stream.getTitle())) {
                titlePane.getChildren().add(new Label(" (" + stream.getTitle() + ")"));
            }

            Label sizeLabel = new Label();

            StringBinding unknownSizeBinding = Bindings.createStringBinding(
                    () -> "Size: ? KB ", stream.sizeProperty()
            );
            StringBinding knownSizeBinding = Bindings.createStringBinding(
                    () -> "Size: " + GuiUtils.getFileSizeTextual(stream.getSize()), stream.sizeProperty()
            );

            sizeLabel.textProperty().bind(
                    Bindings.when(stream.sizeProperty().isEqualTo(-1))
                            .then(unknownSizeBinding)
                            .otherwise(knownSizeBinding)
            );

            Hyperlink getSizeLink = new Hyperlink("get size");
            getSizeLink.setOnAction(event -> singleSizeLoader.load(fileInfo, stream.getFfmpegIndex()));

            HBox radios = new HBox();
            radios.setSpacing(10);
            radios.setAlignment(Pos.CENTER);

            RadioButton upper = new RadioButton("upper");
            upper.selectedProperty().bindBidirectional(stream.selectedAsUpperProperty());

            RadioButton lower = new RadioButton("lower");
            lower.selectedProperty().bindBidirectional(stream.selectedAsLowerProperty());

            radios.getChildren().addAll(upper, lower);

            result.add(titlePane, 0, 1 + streamIndex);
            result.add(sizeLabel, 1, 1 + streamIndex);
            result.add(getSizeLink, 2, 1 + streamIndex);
            result.add(radios, 3, 1 + streamIndex);
            GridPane.setHgrow(titlePane, Priority.ALWAYS);

            int bottomMargin = 2;

            if (stream.isExtra()) {
                titlePane.visibleProperty().bind(Bindings.not(fileInfo.someSubtitlesHiddenProperty()));
                //todo refactor, ugly
                titlePane.managedProperty().bind(Bindings.not(fileInfo.someSubtitlesHiddenProperty()));
                sizeLabel.visibleProperty().bind(Bindings.not(fileInfo.someSubtitlesHiddenProperty()));
                sizeLabel.managedProperty().bind(sizeLabel.visibleProperty());
                getSizeLink.visibleProperty().bind(Bindings.not(fileInfo.someSubtitlesHiddenProperty()).and(stream.sizeProperty().isEqualTo(-1)));
                getSizeLink.managedProperty().bind(getSizeLink.visibleProperty());
                radios.visibleProperty().bind(Bindings.not(fileInfo.someSubtitlesHiddenProperty()));
                radios.managedProperty().bind(radios.visibleProperty());
            } else {
                getSizeLink.visibleProperty().bind(stream.sizeProperty().isEqualTo(-1));
                getSizeLink.managedProperty().bind(stream.sizeProperty().isEqualTo(-1));
            }

            GridPane.setMargin(titlePane, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(sizeLabel, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(getSizeLink, new Insets(0, 5, bottomMargin, 0));
            GridPane.setMargin(radios, new Insets(0, 0, bottomMargin, 0));

            streamIndex++;
        }

        Button button = new Button("Add subtitle file");
        button.getStyleClass().add("add-subtitle-file");
        Image image = new Image("/gui/icons/add.png");
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(8);
        imageView.setFitHeight(imageView.getFitWidth());
        button.setGraphic(imageView);

        result.addRow(2 + fileInfo.getSubtitleStreams().size(), button);

        GridPane.setColumnSpan(button, 4);
        GridPane.setMargin(button, new Insets(5, 0, 0, 0));

        return result;
    }

    private static Pane generateHiddenPane(GuiFileInfo fileInfo) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);

        StringBinding hiddenBinding = Bindings.createStringBinding(
                () -> fileInfo.getSubtitleToHideCount() + " hidden ", fileInfo.subtitleToHideCountProperty()
        );

        Label hiddenLabel = new Label();
        hiddenLabel.visibleProperty().bind(fileInfo.subtitleToHideCountProperty().greaterThan(0));
        hiddenLabel.managedProperty().bind(fileInfo.subtitleToHideCountProperty().greaterThan(0));
        hiddenLabel.textProperty().bind(
                Bindings.when(fileInfo.someSubtitlesHiddenProperty())
                        .then(hiddenBinding)
                        .otherwise("")
        );

        Hyperlink showAllLink = new Hyperlink();
        showAllLink.visibleProperty().bind(fileInfo.subtitleToHideCountProperty().greaterThan(0));
        showAllLink.managedProperty().bind(fileInfo.subtitleToHideCountProperty().greaterThan(0));
        showAllLink.textProperty().bind(
                Bindings.when(fileInfo.someSubtitlesHiddenProperty())
                        .then("show all")
                        .otherwise("hide extra subtitles")
        );
        showAllLink.setOnAction(event -> fileInfo.setSomeSubtitlesHidden(!fileInfo.isSomeSubtitlesHidden()));

        result.getChildren().addAll(hiddenLabel, showAllLink);

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
        void load(GuiFileInfo guiFileInfo, int ffmpegIndex);
    }

    @AllArgsConstructor
    public static class TableWithFilesCell<T> extends TableCell<GuiFileInfo, T> {
        private CellNodeGenerator cellNodeGenerator;

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            GuiFileInfo fileInfo = getTableRow().getItem();

            if (empty || fileInfo == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            setGraphic(cellNodeGenerator.generateNode(fileInfo));
            setText(null);
        }

        @FunctionalInterface
        interface CellNodeGenerator {
            Node generateNode(GuiFileInfo fileInfo);
        }
    }
}
