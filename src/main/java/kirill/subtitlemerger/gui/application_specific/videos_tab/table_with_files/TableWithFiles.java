package kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.custom_controls.MultiColorLabels;
import kirill.subtitlemerger.logic.utils.CacheMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;

@CommonsLog
public class TableWithFiles extends TableView<GuiFileInfo> {
    private static final String PANE_UNAVAILABLE_CLASS = "pane-unavailable";

    private static final String PANE_ERROR_CLASS = "pane-error";

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");

    private final Map<CellType, Map<String, Pane>> cellCache;

    private IntegerProperty selected;

    private BooleanProperty allSelected;

    private IntegerProperty allAvailableCount;

    private AllFileSubtitleSizesLoader allFileSubtitleSizesLoader;

    private SingleFileSubtitleSizeLoader singleFileSubtitleSizeLoader;

    private AddExternalSubtitleFileHandler addExternalSubtitleFileHandler;

    private RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler;

    private FfmpegStreamPreviewHandler ffmpegStreamPreviewHandler;

    private ExternalFilePreviewHandler externalFilePreviewHandler;

    private MergedPreviewHandler mergedPreviewHandler;

    @Getter
    @Setter
    private Mode mode;

    public TableWithFiles() {
        super();

        this.cellCache = new HashMap<>();
        for (CellType cellType : CellType.values()) {
            cellCache.put(cellType, new CacheMap<>(1000));
        }

        setSelectionModel(null);
        setPlaceholder(new Label("there are no files to display"));
    }

    /*
     * Had to make this method because table is initialized with fxml and it happens after the constructor is called so
     * in the constructor columns aren't initialized yet.
     */
    //todo move everything to the constructor
    public void initialize(
            BooleanProperty allSelected,
            IntegerProperty selected,
            IntegerProperty allAvailableCount,
            AllFileSubtitleSizesLoader allFileSubtitleSizesLoader,
            SingleFileSubtitleSizeLoader singleFileSubtitleSizeLoader,
            AddExternalSubtitleFileHandler addExternalSubtitleFileHandler,
            RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler,
            FfmpegStreamPreviewHandler ffmpegStreamPreviewHandler,
            ExternalFilePreviewHandler externalFilePreviewHandler,
            MergedPreviewHandler mergedPreviewHandler

    ) {
        this.allSelected = allSelected;
        this.selected = selected;
        this.allAvailableCount = allAvailableCount;
        this.allFileSubtitleSizesLoader = allFileSubtitleSizesLoader;
        this.singleFileSubtitleSizeLoader = singleFileSubtitleSizeLoader;
        this.addExternalSubtitleFileHandler = addExternalSubtitleFileHandler;
        this.removeExternalSubtitleFileHandler = removeExternalSubtitleFileHandler;
        this.ffmpegStreamPreviewHandler = ffmpegStreamPreviewHandler;
        this.externalFilePreviewHandler = externalFilePreviewHandler;
        this.mergedPreviewHandler = mergedPreviewHandler;

        TableColumn<GuiFileInfo, ?> selectedColumn = getColumns().get(0);
        CheckBox selectAllCheckBox = new CheckBox();
        selectAllCheckBox.selectedProperty().bindBidirectional(allSelected);
        selectAllCheckBox.setOnAction(event -> {
            getItems().forEach(fileInfo -> fileInfo.setSelected(selectAllCheckBox.isSelected()));
            if (selectAllCheckBox.isSelected()) {
                selected.setValue(allAvailableCount.getValue());
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

    public void clearCache() {
        for (CellType cellType : CellType.values()) {
            cellCache.get(cellType).clear();
        }
    }

    private <T> TableWithFilesCell<T> generateSelectedCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(CellType.SELECTED, this::generateSelectedCellPane);
    }

    private <T> TableWithFilesCell<T> generateFileDescriptionCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(CellType.FILE_DESCRIPTION, this::generateFileDescriptionCellPane);
    }

    private <T> TableWithFilesCell<T> generateSubtitlesCell(TableColumn<GuiFileInfo, T> column) {
        return new TableWithFilesCell<>(CellType.SUBTITLES, this::generateSubtitlesCellPane);
    }

    private Pane generateSelectedCellPane(GuiFileInfo fileInfo) {
        HBox result = new HBox();

        result.setPadding(new Insets(4, 0, 4, 0));
        result.setAlignment(Pos.TOP_CENTER);

        if (!StringUtils.isBlank(fileInfo.getUnavailabilityReason())) {
            result.getStyleClass().add(PANE_UNAVAILABLE_CLASS);

            /*
             * We should stop there if in the directory mode, checkbox isn't needed because there is no point in
             * selecting an unavailable file. On the contrary, in the files mode it's possible to select the unavailable
             * file to remove it. Because of the ability to remove the file the behaviour is different.
             */
            if (mode == Mode.DIRECTORY) {
                return result;
            }
        }

        CheckBox checkBox = new CheckBox();

        checkBox.selectedProperty().bindBidirectional(fileInfo.selectedProperty());
        checkBox.setOnAction((event) -> {
            if (checkBox.isSelected()) {
                selected.set(selected.getValue() + 1);
            } else {
                selected.set(selected.getValue() - 1);
            }
            allSelected.set(selected.getValue() == allAvailableCount.get());
        });

        result.getChildren().add(checkBox);

        return result;
    }

    private Pane generateFileDescriptionCellPane(GuiFileInfo fileInfo) {
        VBox result = new VBox();

        result.setPadding(new Insets(4, 5, 4, 4));
        result.setSpacing(10);
        if (!StringUtils.isBlank(fileInfo.getUnavailabilityReason())) {
            result.getStyleClass().add(PANE_UNAVAILABLE_CLASS);
        }

        Label pathLabel = new Label(fileInfo.getPathToDisplay());
        pathLabel.getStyleClass().add("path-label");

        Pane paneWithSizeAndLastModifiedTime = generatePaneWithSizeAndLastModifiedTime(fileInfo);

        result.getChildren().addAll(pathLabel, paneWithSizeAndLastModifiedTime);

        return result;
    }

    private Pane generatePaneWithSizeAndLastModifiedTime(GuiFileInfo fileInfo) {
        GridPane result = new GridPane();

        result.setHgap(30);
        result.setGridLinesVisible(GuiConstants.DEBUG);
        Label sizeTitle = new Label("size");

        Label lastModifiedTitle = new Label("last modified");

        Label size = new Label(GuiUtils.getFileSizeTextual(fileInfo.getSize()));

        Label lastModified = new Label(FORMATTER.print(fileInfo.getLastModified()));

        result.addRow(0, sizeTitle, size);
        result.addRow(1, lastModifiedTitle, lastModified);

        /* Labels of the first row. */
        GridPane.setMargin(result.getChildren().get(0), new Insets(0, 0, 3, 0));
        GridPane.setMargin(result.getChildren().get(2), new Insets(0, 0, 3, 0));

        return result;
    }

    private Pane generateSubtitlesCellPane(GuiFileInfo fileInfo) {
        if (!StringUtils.isBlank(fileInfo.getUnavailabilityReason())) {
            HBox result = new HBox();
            result.setPadding(new Insets(4, 4, 4, 5));
            result.setAlignment(Pos.CENTER_LEFT);
            result.getStyleClass().add(PANE_UNAVAILABLE_CLASS);

            Label reason = new Label(StringUtils.capitalize(fileInfo.getUnavailabilityReason()));
            result.getChildren().add(reason);

            return result;
        }

        GridPane result = new GridPane();

        result.setGridLinesVisible(GuiConstants.DEBUG);

        result.setHgap(15);
        result.setPadding(new Insets(4, 4, 4, 5));

        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHgrow(Priority.ALWAYS);
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

        HBox previewPane = new HBox();
        previewPane.setAlignment(Pos.CENTER);

        Button previewButton = GuiUtils.createImageButton(
                "result",
                "/gui/icons/eye.png",
                15,
                10
        );
        previewButton.setOnAction(event -> mergedPreviewHandler.showPreview(fileInfo));
        previewPane.getChildren().add(previewButton);

        BooleanBinding showExtra = Bindings.not(fileInfo.someSubtitlesHiddenProperty());

        List<Pane> radioPanes = new ArrayList<>();

        int bottomMargin = 2;

        int streamIndex = 0;
        for (GuiSubtitleStream stream : fileInfo.getSubtitleStreams()) {
            BooleanBinding bindingForRadioCellVisibility = null;

            if (stream instanceof GuiFfmpegSubtitleStream) {
                GuiFfmpegSubtitleStream ffmpegStream = (GuiFfmpegSubtitleStream) stream;

                HBox titlePane = new HBox();
                titlePane.setAlignment(Pos.CENTER_LEFT);

                Label language = new Label(ffmpegStream.getLanguage().toUpperCase());
                titlePane.getChildren().add(language);

                if (!StringUtils.isBlank(ffmpegStream.getTitle())) {
                    titlePane.getChildren().add(new Label(" (" + ffmpegStream.getTitle() + ")"));
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

                BooleanBinding failedToLoad = ffmpegStream.failedToLoadReasonProperty().isNotEmpty();

                errorImageLabel.setTooltip(GuiUtils.generateTooltip(ffmpegStream.failedToLoadReasonProperty()));
                errorImageLabel.visibleProperty().bind(failedToLoad);
                errorImageLabel.managedProperty().bind(failedToLoad);

                Label sizeLabel = new Label();

                StringBinding unknownSizeBinding = Bindings.createStringBinding(
                        () -> "Size: ? KB ", stream.sizeProperty()
                );
                StringBinding knownSizeBinding = Bindings.createStringBinding(
                        () -> "Size: " + GuiUtils.getFileSizeTextual(stream.getSize()), stream.sizeProperty()
                );

                sizeLabel.textProperty().bind(
                        Bindings.when(stream.sizeProperty().isEqualTo(GuiSubtitleStream.UNKNOWN_SIZE))
                                .then(unknownSizeBinding)
                                .otherwise(knownSizeBinding)
                );

                previewButton = GuiUtils.createImageButton(
                        "",
                        "/gui/icons/eye.png",
                        15,
                        10
                );
                previewButton.setOnAction(event -> ffmpegStreamPreviewHandler.showPreview(ffmpegStream.getId(), fileInfo));
                previewButton.visibleProperty().bind(stream.sizeProperty().isNotEqualTo(GuiSubtitleStream.UNKNOWN_SIZE));
                previewButton.managedProperty().bind(stream.sizeProperty().isNotEqualTo(GuiSubtitleStream.UNKNOWN_SIZE));

                Hyperlink loadSingleLink = new Hyperlink("load");
                loadSingleLink.setOnAction(event -> singleFileSubtitleSizeLoader.load(fileInfo, ffmpegStream.getId()));
                loadSingleLink.visibleProperty().bind(stream.sizeProperty().isEqualTo(GuiSubtitleStream.UNKNOWN_SIZE));
                loadSingleLink.managedProperty().bind(stream.sizeProperty().isEqualTo(GuiSubtitleStream.UNKNOWN_SIZE));

                Region spacer = new Region();
                sizePane.getChildren().addAll(sizeLabel, loadSingleLink, errorImageLabel, spacer, previewButton);
                HBox.setHgrow(spacer, Priority.ALWAYS);

                if (ffmpegStream.isExtra()) {
                    titlePane.visibleProperty().bind(showExtra);
                    titlePane.managedProperty().bind(showExtra);
                    sizeLabel.visibleProperty().bind(showExtra);
                    sizeLabel.managedProperty().bind(showExtra);
                    sizePane.visibleProperty().bind(showExtra);
                    sizePane.managedProperty().bind(showExtra);
                }

                result.add(titlePane, 0, streamIndex);
                result.add(sizePane, 1, streamIndex);

                GridPane.setMargin(titlePane, new Insets(0, 0, bottomMargin, 0));
                GridPane.setMargin(sizePane, new Insets(0, 0, bottomMargin, 0));

                if (ffmpegStream.isExtra()) {
                    bindingForRadioCellVisibility = showExtra;
                }
            } else if (stream instanceof GuiExternalSubtitleStream) {
                GuiExternalSubtitleStream externalStream = (GuiExternalSubtitleStream) stream;

                HBox fileNameAndRemove = new HBox();
                fileNameAndRemove.setSpacing(10);
                fileNameAndRemove.setAlignment(Pos.CENTER_LEFT);

                Label fileName = new Label();
                fileName.textProperty().bind(externalStream.fileNameProperty());

                StringBinding sizeBinding = Bindings.createStringBinding(
                        () -> "Size: " + GuiUtils.getFileSizeTextual(externalStream.getSize()),
                        externalStream.sizeProperty()
                );

                Button removeButton = new Button();
                removeButton.getStyleClass().add("image-button");
                Image image = new Image("/gui/icons/remove.png");
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(8);
                imageView.setFitHeight(imageView.getFitWidth());
                removeButton.setGraphic(imageView);

                removeButton.setOnAction(event -> removeExternalSubtitleFileHandler.buttonClicked(externalStream.getId(), fileInfo, () -> updateFileNode(fileInfo, radioPanes, previewPane)));

                fileNameAndRemove.getChildren().addAll(fileName, new Region(), removeButton);
                HBox.setHgrow(fileNameAndRemove.getChildren().get(1), Priority.ALWAYS);

                HBox sizePane = new HBox();
                sizePane.setAlignment(Pos.CENTER_LEFT);
                sizePane.setSpacing(5);

                Label sizeLabel = new Label();
                sizeLabel.textProperty().bind(sizeBinding);

                BooleanBinding externalFileUsed = Bindings.isNotEmpty(externalStream.fileNameProperty());

                fileNameAndRemove.visibleProperty().bind(externalFileUsed);
                fileNameAndRemove.managedProperty().bind(externalFileUsed);
                sizeLabel.visibleProperty().bind(externalFileUsed);
                sizeLabel.managedProperty().bind(externalFileUsed);

                previewButton = GuiUtils.createImageButton(
                        "",
                        "/gui/icons/eye.png",
                        15,
                        10
                );
                previewButton.setOnAction(event -> externalFilePreviewHandler.showPreview(stream.getId(), fileInfo));
                previewButton.visibleProperty().bind(externalFileUsed);
                previewButton.managedProperty().bind(externalFileUsed);

                Region spacer = new Region();
                sizePane.getChildren().addAll(sizeLabel, spacer, previewButton);
                HBox.setHgrow(spacer, Priority.ALWAYS);

                result.add(fileNameAndRemove, 0, streamIndex);
                result.add(sizePane, 1, streamIndex);

                GridPane.setMargin(fileNameAndRemove, new Insets(0, 0, bottomMargin, 0));
                GridPane.setMargin(sizePane, new Insets(0, 0, bottomMargin, 0));

                bindingForRadioCellVisibility = externalFileUsed;
            } else {
                throw new IllegalStateException();
            }

            HBox radios = new HBox();
            radios.setSpacing(5);
            radios.setAlignment(Pos.CENTER);

            RadioButton upper = new RadioButton("upper");
            upper.setOnAction(event -> upperStreamClicked(upper, stream.getId(), fileInfo, radioPanes, previewPane));

            RadioButton lower = new RadioButton("lower");
            lower.setOnAction(event -> lowerStreamClicked(lower, stream.getId(), fileInfo, radioPanes, previewPane));

            radios.getChildren().addAll(upper, lower);

            radioPanes.add(radios);

            if (bindingForRadioCellVisibility != null) {
                radios.visibleProperty().bind(bindingForRadioCellVisibility);
                radios.managedProperty().bind(bindingForRadioCellVisibility);
            }

            if (stream instanceof GuiExternalSubtitleStream) {
                GuiExternalSubtitleStream externalStream = (GuiExternalSubtitleStream) stream;

                setRadiosVisibility(radios, externalStream.isCorrectFormat());
                externalStream.correctFormatProperty().addListener((observable, oldValue, newValue) -> setRadiosVisibility(radios, newValue));
            }

            result.add(radios, 2, streamIndex);

            GridPane.setMargin(radios, new Insets(0, 0, bottomMargin, 0));

            streamIndex++;
        }

        Pane hiddenAndAddPane = generateHiddenAndAddPane(fileInfo, radioPanes, previewPane);

        HBox loadAllSizesPane = new HBox();
        loadAllSizesPane.setAlignment(Pos.CENTER);

        Hyperlink loadAllLink = new Hyperlink("load all subtitles");
        loadAllLink.setOnAction(event -> allFileSubtitleSizesLoader.load(fileInfo));

        loadAllLink.visibleProperty().bind(fileInfo.haveSubtitleSizesToLoadProperty());
        loadAllLink.managedProperty().bind(fileInfo.haveSubtitleSizesToLoadProperty());

        loadAllSizesPane.getChildren().add(loadAllLink);

        result.add(hiddenAndAddPane, 0, fileInfo.getSubtitleStreams().size());
        result.add(loadAllSizesPane, 1, fileInfo.getSubtitleStreams().size());
        result.add(previewPane, 2, fileInfo.getSubtitleStreams().size());

        GridPane.setMargin(hiddenAndAddPane, new Insets(3, 0, 0, 0));
        GridPane.setMargin(loadAllSizesPane, new Insets(3, 0, 0, 0));
        GridPane.setMargin(previewPane, new Insets(3, 0, 0, 0));

        MultiColorLabels resultLabels = new MultiColorLabels();
        resultLabels.setWrapText(true);
        resultLabels.setAlignment(Pos.CENTER);
        fileInfo.resultProperty().addListener(observable -> setResultLabels(fileInfo, resultLabels));
        setResultLabels(fileInfo, resultLabels);

        result.addRow(
                1 + fileInfo.getSubtitleStreams().size(),
                resultLabels
        );

        GridPane.setColumnSpan(resultLabels, 3);
        GridPane.setMargin(resultLabels, new Insets(10, 0, 0, 0));

        updateFileNode(fileInfo, radioPanes, previewPane);

        return result;
    }

    private static void upperStreamClicked(RadioButton radioButton, String streamId, GuiFileInfo fileInfo, List<Pane> radioPanes, HBox previewPane) {
        if (radioButton.isSelected()) {
            for (GuiSubtitleStream currentStream : fileInfo.getSubtitleStreams()) {
                if (Objects.equals(currentStream.getId(), streamId)) {
                    currentStream.setSelectedAsUpper(true);
                    currentStream.setSelectedAsLower(false);
                } else {
                    currentStream.setSelectedAsUpper(false);
                }
            }
        } else {
            GuiSubtitleStream stream = GuiSubtitleStream.getById(streamId, fileInfo.getSubtitleStreams());
            stream.setSelectedAsUpper(false);
        }

        updateFileNode(fileInfo, radioPanes, previewPane);
    }

    private static void lowerStreamClicked(RadioButton radioButton, String streamId, GuiFileInfo fileInfo, List<Pane> radioPanes, HBox previewPane) {
        if (radioButton.isSelected()) {
            for (GuiSubtitleStream currentStream : fileInfo.getSubtitleStreams()) {
                if (Objects.equals(currentStream.getId(), streamId)) {
                    currentStream.setSelectedAsLower(true);
                    currentStream.setSelectedAsUpper(false);
                } else {
                    currentStream.setSelectedAsLower(false);
                }
            }
        } else {
            GuiSubtitleStream stream = GuiSubtitleStream.getById(streamId, fileInfo.getSubtitleStreams());
            stream.setSelectedAsLower(false);
        }

        updateFileNode(fileInfo, radioPanes, previewPane);
    }

    private static void updateFileNode(GuiFileInfo fileInfo, List<Pane> radioPanes, HBox previewPane) {
        setPreviewPaneState(fileInfo, previewPane);

        int i = 0;
        for (Pane radioPane : radioPanes) {
            RadioButton upper = (RadioButton) radioPane.getChildren().get(0);
            RadioButton lower = (RadioButton) radioPane.getChildren().get(1);

            upper.setSelected(fileInfo.getSubtitleStreams().get(i).isSelectedAsUpper());
            lower.setSelected(fileInfo.getSubtitleStreams().get(i).isSelectedAsLower());

            i++;
        }
    }

    private static void setPreviewPaneState(
            GuiFileInfo fileInfo,
            HBox previewPane
    ) {
        int selectFromCount = fileInfo.getFfmpegSubtitleStreams().size();
        for (GuiExternalSubtitleStream stream : fileInfo.getExternalSubtitleStreams()) {
            if (!StringUtils.isBlank(stream.getFileName())) {
                selectFromCount += 1;
            }
        }

        GuiSubtitleStream upperStream = fileInfo.getSubtitleStreams().stream()
                .filter(GuiSubtitleStream::isSelectedAsUpper)
                .findFirst().orElse(null);
        GuiSubtitleStream lowerStream = fileInfo.getSubtitleStreams().stream()
                .filter(GuiSubtitleStream::isSelectedAsLower)
                .findFirst().orElse(null);

        if (selectFromCount >= 2) {
            previewPane.setVisible(true);
            previewPane.setManaged(true);

            if (upperStream != null && lowerStream != null) {
                if (upperStream.getSize() == GuiSubtitleStream.UNKNOWN_SIZE || lowerStream.getSize() == GuiSubtitleStream.UNKNOWN_SIZE) {
                    previewPane.getChildren().get(0).setDisable(true);
                    Tooltip.install(previewPane, GuiUtils.generateTooltip("Please load selected subtitles first"));
                } else {
                    previewPane.getChildren().get(0).setDisable(false);
                    Tooltip.install(previewPane, null);
                }
            } else {
                previewPane.getChildren().get(0).setDisable(true);
                Tooltip.install(previewPane, GuiUtils.generateTooltip("Please select subtitles to merge first"));
            }
        } else {
            previewPane.setVisible(false);
            previewPane.setManaged(false);
        }
    }

    private void setRadiosVisibility(HBox radios, boolean formatCorrect) {
        for (Node node : radios.getChildren()) {
            node.setDisable(!formatCorrect);
        }

        if (formatCorrect) {
            Tooltip.install(radios, null);
        } else {
            Tooltip.install(radios, GuiUtils.generateTooltip("Subtitle file has an incorrect format"));
        }
    }

    private Pane generateHiddenAndAddPane(GuiFileInfo fileInfo, List<Pane> radioPanes, HBox previewPane) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);

        if (fileInfo.getSubtitleToHideCount() > 0) {
            Hyperlink showAllLink = new Hyperlink();
            showAllLink.textProperty().bind(
                    Bindings.when(fileInfo.someSubtitlesHiddenProperty())
                            .then("show " + fileInfo.getSubtitleToHideCount() + " hidden")
                            .otherwise("hide extra")
            );
            showAllLink.setOnAction(event -> fileInfo.setSomeSubtitlesHidden(!fileInfo.isSomeSubtitlesHidden()));

            result.getChildren().add(showAllLink);

            Region spacer = new Region();
            spacer.setMinWidth(25);
            spacer.setMaxWidth(25);
            result.getChildren().add(spacer);
        }

        Button button = GuiUtils.createImageButton(
                "Add subtitles",
                "/gui/icons/add.png",
                9,
                9
        );
        button.setOnAction((event -> addExternalSubtitleFileHandler.buttonClicked(fileInfo, () -> updateFileNode(fileInfo, radioPanes, previewPane))));

        BooleanBinding canAddMoreFiles = fileInfo.getExternalSubtitleStreams().get(0).fileNameProperty().isEmpty()
                .or(fileInfo.getExternalSubtitleStreams().get(1).fileNameProperty().isEmpty());

        button.visibleProperty().bind(canAddMoreFiles);
        button.managedProperty().bind(canAddMoreFiles);

        result.getChildren().add(button);

        return result;
    }

    private static void setResultLabels(GuiFileInfo fileInfo, MultiColorLabels resultLabels) {
        if (fileInfo.getResult() == null) {
            return;
        }

        resultLabels.set(fileInfo.getResult());
    }

    @FunctionalInterface
    public interface AllFileSubtitleSizesLoader {
        void load(GuiFileInfo guiFileInfo);
    }

    @FunctionalInterface
    public interface SingleFileSubtitleSizeLoader {
        void load(GuiFileInfo guiFileInfo, String streamId);
    }

    @FunctionalInterface
    public interface AddExternalSubtitleFileHandler {
        void buttonClicked(GuiFileInfo guiFileInfo, Runnable onFinish);
    }

    @FunctionalInterface
    public interface RemoveExternalSubtitleFileHandler {
        void buttonClicked(String streamId, GuiFileInfo guiFileInfo, Runnable onFinish);
    }

    @FunctionalInterface
    public interface FfmpegStreamPreviewHandler {
        void showPreview(String streamId, GuiFileInfo guiFileInfo);
    }

    @FunctionalInterface
    public interface ExternalFilePreviewHandler {
        void showPreview(String streamId, GuiFileInfo guiFileInfo);
    }

    @FunctionalInterface
    public interface MergedPreviewHandler {
        void showPreview(GuiFileInfo guiFileInfo);
    }

    @AllArgsConstructor
    private class TableWithFilesCell<T> extends TableCell<GuiFileInfo, T> {
        private CellType cellType;

        private CellPaneGenerator cellPaneGenerator;

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            GuiFileInfo fileInfo = getTableRow().getItem();

            if (empty || fileInfo == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Pane pane = cellCache.get(cellType).get(fileInfo.getFullPath());
            if (pane == null) {
                Pane newPane = cellPaneGenerator.generateNode(fileInfo);

                if (StringUtils.isBlank(fileInfo.getUnavailabilityReason())) {
                    fileInfo.resultProperty().addListener(observable -> setOrRemoveErrorClass(fileInfo, newPane));
                    setOrRemoveErrorClass(fileInfo, newPane);
                }

                pane = newPane;
                cellCache.get(cellType).put(fileInfo.getFullPath(), pane);
            }

            setGraphic(pane);
            setText(null);
        }

        private void setOrRemoveErrorClass(GuiFileInfo fileInfo, Pane pane) {
            if (fileInfo.getResult() != null && !StringUtils.isBlank(fileInfo.getResult().getError())) {
                pane.getStyleClass().add(PANE_ERROR_CLASS);
            } else {
                pane.getStyleClass().remove(PANE_ERROR_CLASS);
            }
        }
    }

    @FunctionalInterface
    interface CellPaneGenerator {
        Pane generateNode(GuiFileInfo fileInfo);
    }

    private enum CellType {
        SELECTED,
        FILE_DESCRIPTION,
        SUBTITLES
    }

    public enum Mode {
        SEPARATE_FILES,
        DIRECTORY
    }
}
