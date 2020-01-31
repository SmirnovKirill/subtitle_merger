package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
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

import java.util.Arrays;
import java.util.Random;

public class TableWithFiles extends TableView<GuiFileInfo> {
    private static final String ROW_UNAVAILABLE_CLASS = "row-unavailable";

    private static final String ROW_ERROR_PARTIAL_BORDER_CLASS = "row-error-partial-border";

    private static final String ROW_ERROR_FULL_BORDER_CLASS = "row-error-full-border";

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

                getStyleClass().removeAll(
                        Arrays.asList(
                                ROW_UNAVAILABLE_CLASS,
                                ROW_ERROR_PARTIAL_BORDER_CLASS,
                                ROW_ERROR_FULL_BORDER_CLASS
                        )
                );

                if (!StringUtils.isBlank(fileInfo.getUnavailabilityReason())) {
                    getStyleClass().add(ROW_UNAVAILABLE_CLASS);
                } else if (fileInfo.getErrorBorder()) {
                    if (getIndex() > 0 && getItems().get(getIndex() - 1).getErrorBorder()) {
                        getStyleClass().add(ROW_ERROR_PARTIAL_BORDER_CLASS);
                    } else {
                        getStyleClass().add(ROW_ERROR_FULL_BORDER_CLASS);
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

        result.setHgap(15);
        result.setPadding(new Insets(3, 3, 3, 5));

        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHgrow(Priority.ALWAYS);
        result.getColumnConstraints().add(columnConstraints);

        columnConstraints = new ColumnConstraints();
        columnConstraints.setMinWidth(Region.USE_PREF_SIZE);
        result.getColumnConstraints().add(columnConstraints);

        columnConstraints = new ColumnConstraints();
        columnConstraints.setMinWidth(Region.USE_PREF_SIZE);
        result.getColumnConstraints().add(columnConstraints);

        columnConstraints = new ColumnConstraints();
        columnConstraints.setMinWidth(Region.USE_PREF_SIZE);
        result.getColumnConstraints().add(columnConstraints);

        if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
            return result;
        }

        Pane hiddenPane = generateHiddenPane(fileInfo);

        HBox getAllSizesPane = new HBox();
        getAllSizesPane.setAlignment(Pos.CENTER);

        Hyperlink getAllSizes = new Hyperlink("get all sizes");
        getAllSizes.setOnAction(event -> allSizesLoader.load(fileInfo));
        getAllSizes.visibleProperty().bind(fileInfo.haveSubtitleSizesToLoadProperty());
        getAllSizes.managedProperty().bind(fileInfo.haveSubtitleSizesToLoadProperty());

        getAllSizesPane.getChildren().add(getAllSizes);

        result.add(hiddenPane, 0, 0);
        result.add(getAllSizesPane, 1, 0);
        result.add(new Region(), 2, 0);

        GridPane.setMargin(hiddenPane, new Insets(0, 0, 10, 0));
        GridPane.setMargin(getAllSizesPane, new Insets(0, 0, 10, 0));
        GridPane.setMargin(result.getChildren().get(2), new Insets(0, 0, 10, 0));

        int streamIndex = 0;
        for (GuiSubtitleStream stream : fileInfo.getSubtitleStreams()) {
            HBox titlePane = new HBox();
            titlePane.setAlignment(Pos.CENTER_LEFT);

            Label language = new Label(stream.getLanguage().toUpperCase());
            titlePane.getChildren().add(language);

            if (!StringUtils.isBlank(stream.getTitle())) {
                titlePane.getChildren().add(new Label(" (" + stream.getTitle() + ")"));
            }

            HBox sizePane = new HBox();
            sizePane.setAlignment(Pos.CENTER_LEFT);
            sizePane.setSpacing(5);

            Label errorImageLabel = new Label();
            errorImageLabel.setAlignment(Pos.CENTER);

            Image errorImage = new Image("/gui/icons/error.png");
            ImageView errorImageView = new ImageView(errorImage);
            errorImageView.setFitWidth(12);
            errorImageView.setFitHeight(errorImageView.getFitWidth());
            errorImageLabel.setGraphic(errorImageView);

            errorImageLabel.setTooltip(GuiUtils.generateTooltip(stream.failedToLoadReasonProperty()));

            errorImageLabel.visibleProperty().bind(Bindings.isNotEmpty(stream.failedToLoadReasonProperty()));
            errorImageLabel.managedProperty().bind(Bindings.isNotEmpty(stream.failedToLoadReasonProperty()));

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

            sizePane.getChildren().addAll(sizeLabel, getSizeLink, errorImageLabel);

            HBox radios = new HBox();
            radios.setSpacing(5);
            radios.setAlignment(Pos.CENTER);

            RadioButton upper = new RadioButton("upper");
            upper.selectedProperty().bindBidirectional(stream.selectedAsUpperProperty());

            RadioButton lower = new RadioButton("lower");
            lower.selectedProperty().bindBidirectional(stream.selectedAsLowerProperty());

            radios.getChildren().addAll(upper, lower);

            result.add(titlePane, 0, 1 + streamIndex);
            result.add(sizePane, 1, 1 + streamIndex);
            result.add(radios, 2, 1 + streamIndex);

            int bottomMargin = 2;

            if (stream.isExtra()) {
                titlePane.visibleProperty().bind(Bindings.not(fileInfo.someSubtitlesHiddenProperty()));
                //todo refactor, ugly
                titlePane.managedProperty().bind(Bindings.not(fileInfo.someSubtitlesHiddenProperty()));
                sizeLabel.visibleProperty().bind(Bindings.not(fileInfo.someSubtitlesHiddenProperty()));
                sizeLabel.managedProperty().bind(sizeLabel.visibleProperty());
                sizePane.visibleProperty().bind(Bindings.not(fileInfo.someSubtitlesHiddenProperty()));
                sizePane.managedProperty().bind(sizePane.visibleProperty());
                radios.visibleProperty().bind(Bindings.not(fileInfo.someSubtitlesHiddenProperty()));
                radios.managedProperty().bind(radios.visibleProperty());
            }

            getSizeLink.visibleProperty().bind(stream.sizeProperty().isEqualTo(-1));
            getSizeLink.managedProperty().bind(stream.sizeProperty().isEqualTo(-1));

            GridPane.setMargin(titlePane, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(sizePane, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(radios, new Insets(0, 0, bottomMargin, 0));

            streamIndex++;
        }

        int externalSubtitleFileIndex = 0;
        for (GuiExternalSubtitleFile externalSubtitleFile : fileInfo.getExternalSubtitleFiles()) {
            HBox fileNameAndRemove = new HBox();
            fileNameAndRemove.setSpacing(10);
            fileNameAndRemove.setAlignment(Pos.CENTER_LEFT);

            Label fileName = new Label();
            fileName.textProperty().bind(externalSubtitleFile.fileNameProperty());

            StringBinding sizeBinding = Bindings.createStringBinding(
                    () -> "Size: " + GuiUtils.getFileSizeTextual(externalSubtitleFile.getSize()),
                    externalSubtitleFile.sizeProperty()
            );

            Button removeButton = new Button();
            removeButton.getStyleClass().add("image-button");
            Image image = new Image("/gui/icons/remove.png");
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(8);
            imageView.setFitHeight(imageView.getFitWidth());
            removeButton.setGraphic(imageView);

            removeButton.setOnAction(event -> {
                externalSubtitleFile.setFileName(null);
                externalSubtitleFile.setSize(-1);
                externalSubtitleFile.setSelectedAsUpper(false);
                externalSubtitleFile.setSelectedAsLower(false);
            });

            fileNameAndRemove.getChildren().addAll(fileName, new Region(), removeButton);
            HBox.setHgrow(fileNameAndRemove.getChildren().get(1), Priority.ALWAYS);

            Label sizeLabel = new Label();
            sizeLabel.textProperty().bind(sizeBinding);

            HBox radios = new HBox();
            radios.setSpacing(5);
            radios.setAlignment(Pos.CENTER);

            RadioButton upper = new RadioButton("upper");
            upper.selectedProperty().bindBidirectional(externalSubtitleFile.selectedAsUpperProperty());

            RadioButton lower = new RadioButton("lower");
            lower.selectedProperty().bindBidirectional(externalSubtitleFile.selectedAsLowerProperty());

            radios.getChildren().addAll(upper, lower);

            int rowIndex = 1 + fileInfo.getSubtitleStreams().size() + externalSubtitleFileIndex;
            result.add(fileNameAndRemove, 0, rowIndex);
            result.add(sizeLabel, 1, rowIndex);
            result.add(radios, 2, rowIndex);

            int bottomMargin = 2;

            GridPane.setMargin(fileNameAndRemove, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(sizeLabel, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(radios, new Insets(0, 0, bottomMargin, 0));

            //todo refactor, ugly
            fileNameAndRemove.visibleProperty().bind(Bindings.isNotEmpty(externalSubtitleFile.fileNameProperty()));
            fileNameAndRemove.managedProperty().bind(Bindings.isNotEmpty(externalSubtitleFile.fileNameProperty()));
            sizeLabel.visibleProperty().bind(Bindings.isNotEmpty(externalSubtitleFile.fileNameProperty()));
            sizeLabel.managedProperty().bind(Bindings.isNotEmpty(externalSubtitleFile.fileNameProperty()));
            radios.visibleProperty().bind(Bindings.isNotEmpty(externalSubtitleFile.fileNameProperty()));
            radios.managedProperty().bind(Bindings.isNotEmpty(externalSubtitleFile.fileNameProperty()));

            externalSubtitleFileIndex++;
        }

        BooleanBinding allExternalFilesSelected = Bindings.isNotEmpty(fileInfo.getExternalSubtitleFiles().get(0).fileNameProperty())
                .and(Bindings.isNotEmpty(fileInfo.getExternalSubtitleFiles().get(1).fileNameProperty()));

        Button button = new Button("Add subtitle file");
        button.getStyleClass().add("add-subtitle-file");
        button.visibleProperty().bind(Bindings.not(allExternalFilesSelected));
        button.managedProperty().bind(Bindings.not(allExternalFilesSelected));
        Image image = new Image("/gui/icons/add.png");
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(8);
        imageView.setFitHeight(imageView.getFitWidth());
        button.setGraphic(imageView);
        button.setOnAction((event -> {
            int i = Math.abs(new Random().nextInt()) % 2;
            fileInfo.getExternalSubtitleFiles().get(i).setFileName(i + "sub.srt");
            fileInfo.getExternalSubtitleFiles().get(i).setSize(38482);
        }));

        result.addRow(
                1 + fileInfo.getExternalSubtitleFiles().size() + fileInfo.getSubtitleStreams().size(),
                button
        );

        GridPane.setColumnSpan(button, 3);
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
