package kirill.subtitlemerger.gui.tabs.videos.table_with_files;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.forms_and_controls.ActionResultLabels;
import kirill.subtitlemerger.gui.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.LogicConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;

import static kirill.subtitlemerger.gui.GuiConstants.PANE_ERROR_CLASS;
import static kirill.subtitlemerger.gui.GuiConstants.PANE_UNAVAILABLE_CLASS;
import static kirill.subtitlemerger.gui.tabs.videos.table_with_files.TableSubtitleOption.UNKNOWN_SIZE;

@CommonsLog
public class TableWithFiles extends TableView<TableFileInfo> {
    private static final int CELL_PADDING = 4;

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");

    private static final int OPTION_TITLE_PANE_MIN_WIDTH = 190;

    /*
     * On Windows default font is more compact than the Linux's one. So it's better to set the width smaller because
     * the gap between the unknown size label and load link looks pretty big anyway but on Windows it looks even bigger.
     */
    private static final int SIZE_AND_PREVIEW_PANE_WIDTH = SystemUtils.IS_OS_LINUX ? 90 : 82;

    private static final int SELECT_OPTION_PANE_WIDTH = 110;

    private final Map<String, Map<CellType, Pane>> cellCache;

    @Getter
    private Mode mode;

    private ReadOnlyIntegerWrapper allSelectedCount;

    private CheckBox allSelectedCheckBox;

    @Getter
    private int allSelectableCount;

    @Getter
    private int selectedAvailableCount;

    @Getter
    private int selectedUnavailableCount;

    private ObjectProperty<AllSelectedHandler> allSelectedHandler;

    private ToggleGroup sortByGroup;

    private ToggleGroup sortDirectionGroup;

    private ObjectProperty<SortByChangeHandler> sortByChangeHandler;

    private ObjectProperty<SortDirectionChangeHandler> sortDirectionChangeHandler;

    private ObjectProperty<RemoveSubtitleOptionHandler> removeSubtitleOptionHandler;

    private ObjectProperty<SingleSubtitleLoader> singleSubtitleLoader;

    private ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandler;

    private ObjectProperty<AddFileWithSubtitlesHandler> addFileWithSubtitlesHandler;

    private ObjectProperty<AllFileSubtitleLoader> allFileSubtitleLoader;

    private ObjectProperty<MergedSubtitlePreviewHandler> mergedSubtitlePreviewHandler;

    public TableWithFiles() {
        cellCache = new HashMap<>();

        allSelectedCount = new ReadOnlyIntegerWrapper();
        allSelectedCheckBox = generateAllSelectedCheckBox();
        allSelectedHandler = new SimpleObjectProperty<>();

        sortByGroup = new ToggleGroup();
        sortByGroup.selectedToggleProperty().addListener(this::sortByChanged);

        sortDirectionGroup = new ToggleGroup();
        sortDirectionGroup.selectedToggleProperty().addListener(this::sortDirectionChanged);

        sortByChangeHandler = new SimpleObjectProperty<>();
        sortDirectionChangeHandler = new SimpleObjectProperty<>();

        removeSubtitleOptionHandler = new SimpleObjectProperty<>();
        singleSubtitleLoader = new SimpleObjectProperty<>();
        subtitleOptionPreviewHandler = new SimpleObjectProperty<>();
        addFileWithSubtitlesHandler = new SimpleObjectProperty<>();
        allFileSubtitleLoader = new SimpleObjectProperty<>();
        mergedSubtitlePreviewHandler = new SimpleObjectProperty<>();

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addColumns(allSelectedCheckBox);

        setContextMenu(generateContextMenu(sortByGroup, sortDirectionGroup));
        setPlaceholder(new Label("there are no files to display"));
        setSelectionModel(null);
    }

    private CheckBox generateAllSelectedCheckBox() {
        CheckBox result = new CheckBox();

        result.setOnAction(event -> {
            AllSelectedHandler handler = allSelectedHandler.get();
            if (handler == null) {
                return;
            }

            handler.handle(result.isSelected());
        });

        return result;
    }

    private void sortByChanged(Observable observable, Toggle oldValue, Toggle newValue) {
        if (oldValue == null || newValue == null) {
            return;
        }

        getSortByChangeHandler().handle((SortBy) newValue.getUserData());
    }

    private void sortDirectionChanged(Observable observable, Toggle oldValue, Toggle newValue) {
        if (oldValue == null || newValue == null) {
            return;
        }

        getSortDirectionChangeHandler().handle((SortDirection) newValue.getUserData());
    }

    private void addColumns(CheckBox allSelectedCheckBox) {
        TableColumn<TableFileInfo, ?> selectedColumn = new TableColumn<>();
        selectedColumn.setCellFactory(
                column -> new TableWithFilesCell<>(CellType.SELECTED, this::generateSelectedCellPane)
        );
        selectedColumn.setGraphic(allSelectedCheckBox);
        selectedColumn.setMaxWidth(26);
        selectedColumn.setMinWidth(26);
        selectedColumn.setReorderable(false);
        selectedColumn.setResizable(false);
        selectedColumn.setSortable(false);

        TableColumn<TableFileInfo, ?> fileDescriptionColumn = new TableColumn<>("file");
        fileDescriptionColumn.setCellFactory(
                column -> new TableWithFilesCell<>(CellType.FILE_DESCRIPTION, this::generateFileDescriptionCellPane)
        );
        fileDescriptionColumn.setMinWidth(200);
        fileDescriptionColumn.setReorderable(false);
        fileDescriptionColumn.setSortable(false);

        TableColumn<TableFileInfo, ?> subtitleColumn = new TableColumn<>("subtitles");
        subtitleColumn.setCellFactory(
                column -> new TableWithFilesCell<>(CellType.SUBTITLES, this::generateSubtitleCellPane)
        );

        subtitleColumn.setMinWidth(
                CELL_PADDING + 1
                        + OPTION_TITLE_PANE_MIN_WIDTH + 15 + SIZE_AND_PREVIEW_PANE_WIDTH + 15 + SELECT_OPTION_PANE_WIDTH
                        + CELL_PADDING
        );
        subtitleColumn.setReorderable(false);
        subtitleColumn.setSortable(false);

        getColumns().addAll(Arrays.asList(selectedColumn, fileDescriptionColumn, subtitleColumn));
    }

    private Pane generateSelectedCellPane(TableFileInfo fileInfo) {
        HBox result = new HBox();

        result.setPadding(new Insets(CELL_PADDING, 0, CELL_PADDING, 0));
        result.setAlignment(Pos.TOP_CENTER);

        /*
         * We should stop here if in the directory mode, checkbox isn't needed because there is no point in selecting an
         * unavailable file. On the contrary, in the files mode it's possible to select the unavailable file to remove
         * it. Because of the ability to remove the file the behaviour is different.
         */
        if (fileInfo.getUnavailabilityReason() != null && mode == Mode.DIRECTORY) {
            return result;
        }

        CheckBox selectedCheckBox = new CheckBox();

        selectedCheckBox.selectedProperty().bindBidirectional(fileInfo.selectedProperty());
        selectedCheckBox.setOnAction(event -> handleFileSelectionChange(selectedCheckBox.isSelected(), fileInfo));

        result.getChildren().add(selectedCheckBox);

        return result;
    }

    private void handleFileSelectionChange(boolean selected, TableFileInfo fileInfo) {
        int addValue = selected ? 1 : -1;

        if (fileInfo.getUnavailabilityReason() == null) {
            selectedAvailableCount += addValue;
        } else {
            selectedUnavailableCount += addValue;
        }

        /*
         * It's very important that this line goes after the modification of the previous counters
         * (selectedAvailableCount and selectedUnavailableCount) because this property will have subscribers and they
         * need updated counter values there.
         */
        allSelectedCount.setValue(getAllSelectedCount() + addValue);

        allSelectedCheckBox.setSelected(getAllSelectedCount() > 0 && getAllSelectedCount() == allSelectableCount);
    }

    private Pane generateFileDescriptionCellPane(TableFileInfo fileInfo) {
        VBox result = new VBox();

        result.setPadding(new Insets(CELL_PADDING, CELL_PADDING + 1, CELL_PADDING, CELL_PADDING));
        result.setSpacing(10);

        Label pathLabel = new Label(fileInfo.getFilePath());
        pathLabel.getStyleClass().add("path-label");

        Pane sizeAndLastModifiedPane = generateSizeAndLastModifiedPane(fileInfo);

        result.getChildren().addAll(pathLabel, sizeAndLastModifiedPane);

        return result;
    }

    private static Pane generateSizeAndLastModifiedPane(TableFileInfo fileInfo) {
        GridPane result = new GridPane();

        result.setHgap(30);
        result.setGridLinesVisible(GuiConstants.GRID_LINES_VISIBLE);

        Label sizeTitle = new Label("size");
        Label lastModifiedTitle = new Label("last modified");

        Label size = new Label(GuiUtils.getFileSizeTextual(fileInfo.getSize(), false));
        Label lastModified = new Label(FORMATTER.print(fileInfo.getLastModified()));

        result.addRow(0, sizeTitle, size);
        result.addRow(1, lastModifiedTitle, lastModified);

        GridPane.setMargin(sizeTitle, new Insets(0, 0, 3, 0));
        GridPane.setMargin(size, new Insets(0, 0, 3, 0));

        return result;
    }

    private Pane generateSubtitleCellPane(TableFileInfo fileInfo) {
        if (fileInfo.getUnavailabilityReason() != null) {
            return generateSubtitleUnavailableCellPane(fileInfo);
        }

        VBox result = new VBox();

        result.setPadding(new Insets(CELL_PADDING, CELL_PADDING, CELL_PADDING, CELL_PADDING + 1));
        result.setSpacing(2);

        for (TableSubtitleOption subtitleOption : fileInfo.getSubtitleOptions()) {
            result.getChildren().addAll(
                    generateSubtitleOptionPane(
                            subtitleOption,
                            fileInfo,
                            removeSubtitleOptionHandler,
                            singleSubtitleLoader,
                            subtitleOptionPreviewHandler
                    )
            );
        }

        result.getChildren().addAll(
                GuiUtils.createFixedHeightSpacer(1),
                generateRowWithActionsPane(
                        fileInfo,
                        addFileWithSubtitlesHandler,
                        allFileSubtitleLoader,
                        mergedSubtitlePreviewHandler
                ),
                GuiUtils.createFixedHeightSpacer(6),
                generateActionResultLabels(fileInfo)
        );

        return result;
    }

    private static Pane generateSubtitleUnavailableCellPane(TableFileInfo fileInfo) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setPadding(new Insets(CELL_PADDING, CELL_PADDING, CELL_PADDING, CELL_PADDING + 1));

        result.getChildren().add(new Label(unavailabilityReasonToString(fileInfo.getUnavailabilityReason())));

        return result;
    }

    private static String unavailabilityReasonToString(TableFileInfo.UnavailabilityReason reason) {
        if (reason == null) {
            return "";
        }

        switch (reason) {
            case NO_EXTENSION:
                return "File has no extension";
            case NOT_ALLOWED_EXTENSION:
                return "File has a not allowed extension";
            case FAILED_TO_GET_MIME_TYPE:
                return "Failed to get the mime type";
            case NOT_ALLOWED_MIME_TYPE:
                return "File has a mime type that is not allowed";
            case FAILED_TO_GET_FFPROBE_INFO:
                return "Failed to get video info with the ffprobe";
            case NOT_ALLOWED_CONTAINER:
                return "Video has a format that is not allowed";
            default:
                throw new IllegalStateException();
        }
    }

    private static Pane generateSubtitleOptionPane(
            TableSubtitleOption subtitleOption,
            TableFileInfo fileInfo,
            ObjectProperty<RemoveSubtitleOptionHandler> removeSubtitleOptionHandler,
            ObjectProperty<SingleSubtitleLoader> singleSubtitleLoader,
            ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(15);

        if (subtitleOption.isHideable()) {
            GuiUtils.bindVisibleAndManaged(result, Bindings.not(fileInfo.someOptionsHiddenProperty()));
        } else if (subtitleOption.isRemovable()) {
            GuiUtils.bindVisibleAndManaged(result, subtitleOption.titleProperty().isNotEmpty());
        }

        result.getChildren().addAll(
                generateOptionTitleAndRemovePane(subtitleOption, fileInfo, removeSubtitleOptionHandler),
                generateSizeAndPreviewPane(
                        subtitleOption,
                        fileInfo,
                        subtitleOptionPreviewHandler,
                        singleSubtitleLoader
                ),
                generateSelectOptionPane(subtitleOption)
        );

        HBox.setHgrow(result.getChildren().get(0), Priority.ALWAYS);

        return result;
    }

    private static Pane generateOptionTitleAndRemovePane(
            TableSubtitleOption subtitleOption,
            TableFileInfo fileInfo,
            ObjectProperty<RemoveSubtitleOptionHandler> removeSubtitleOptionHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setMinWidth(OPTION_TITLE_PANE_MIN_WIDTH);
        result.setSpacing(10);

        result.getChildren().add(generateOptionTitleLabel(subtitleOption));

        @SuppressWarnings("SimplifyOptionalCallChains")
        Button removeButton = generateRemoveButton(
                subtitleOption,
                fileInfo,
                removeSubtitleOptionHandler
        ).orElse(null);
        if (removeButton != null) {
            result.getChildren().add(removeButton);
        }

        return result;
    }

    private static Label generateOptionTitleLabel(TableSubtitleOption subtitleOption) {
        Label result = new Label();

        result.textProperty().bind(subtitleOption.titleProperty());
        result.setMaxWidth(Double.MAX_VALUE);

        return result;
    }

    private static Optional<Button> generateRemoveButton(
            TableSubtitleOption subtitleOption,
            TableFileInfo fileInfo,
            ObjectProperty<RemoveSubtitleOptionHandler> removeSubtitleOptionHandler
    ) {
        if (!subtitleOption.isRemovable()) {
            return Optional.empty();
        }

        Button result = GuiUtils.createImageButton(
                null,
                "/gui/icons/remove.png",
                8,
                8
        );

        result.setOnAction(event -> {
            RemoveSubtitleOptionHandler handler = removeSubtitleOptionHandler.get();
            if (handler == null) {
                return;
            }

            handler.remove(subtitleOption, fileInfo);
        });

        return Optional.of(result);
    }

    private static Pane generateSizeAndPreviewPane(
            TableSubtitleOption subtitleOption,
            TableFileInfo fileInfo,
            ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandler,
            ObjectProperty<SingleSubtitleLoader> singleSubtitleLoader
    ) {
        StackPane result = new StackPane();

        GuiUtils.setFixedWidth(result, SIZE_AND_PREVIEW_PANE_WIDTH);

        Pane knownSizeAndPreviewPane = generateKnownSizeAndPreviewPane(
                subtitleOption,
                fileInfo,
                subtitleOptionPreviewHandler
        );
        knownSizeAndPreviewPane.visibleProperty().bind(subtitleOption.sizeProperty().isNotEqualTo(UNKNOWN_SIZE));

        Pane unknownSizePane = generateUnknownSizePane(subtitleOption, fileInfo, singleSubtitleLoader);
        unknownSizePane.visibleProperty().bind(subtitleOption.sizeProperty().isEqualTo(UNKNOWN_SIZE));

        result.getChildren().addAll(knownSizeAndPreviewPane, unknownSizePane);

        return result;
    }

    private static Pane generateKnownSizeAndPreviewPane(
            TableSubtitleOption subtitleOption,
            TableFileInfo fileInfo,
            ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);

        Label sizeLabel = new Label();
        sizeLabel.textProperty().bind(
                Bindings.createStringBinding(() ->
                        "Size: " + GuiUtils.getFileSizeTextual(subtitleOption.getSize(), true),
                        subtitleOption.sizeProperty()
                )
        );

        Region spacer = new Region();

        result.getChildren().addAll(
                sizeLabel,
                spacer,
                generatePreviewButton(subtitleOption, fileInfo, subtitleOptionPreviewHandler)
        );
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return result;
    }

    private static Button generatePreviewButton(
            TableSubtitleOption subtitleOption,
            TableFileInfo fileInfo,
            ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandler
    ) {
        Button result = GuiUtils.createImageButton("", "/gui/icons/eye.png", 15, 10);

        result.setOnAction(event -> {
            SubtitleOptionPreviewHandler handler = subtitleOptionPreviewHandler.get();
            if (handler == null) {
                return;
            }

            handler.showPreview(subtitleOption, fileInfo);
        });

        return result;
    }

    private static Pane generateUnknownSizePane(
            TableSubtitleOption subtitleOption,
            TableFileInfo fileInfo,
            ObjectProperty<SingleSubtitleLoader> singleSubtitleLoader
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();

        result.getChildren().addAll(
                generateSizeAndFailedToLoadPane(subtitleOption),
                spacer,
                generateLoadSubtitleLink(subtitleOption, fileInfo, singleSubtitleLoader)
        );
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return result;
    }

    private static Pane generateSizeAndFailedToLoadPane(TableSubtitleOption subtitleOption) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(5);

        result.getChildren().addAll(
                new Label("Size: ? KB"),
                generateFailedToLoadLabel(subtitleOption)
        );

        return result;
    }

    private static Label generateFailedToLoadLabel(TableSubtitleOption subtitleOption) {
        Label result = new Label();

        result.setAlignment(Pos.CENTER);
        result.setGraphic(GuiUtils.createImageView("/gui/icons/error.png", 12, 12));

        result.setTooltip(GuiUtils.generateTooltip(subtitleOption.failedToLoadReasonProperty()));
        GuiUtils.bindVisibleAndManaged(result, subtitleOption.failedToLoadReasonProperty().isNotNull());

        return result;
    }

    private static Hyperlink generateLoadSubtitleLink(
            TableSubtitleOption subtitleOption,
            TableFileInfo fileInfo,
            ObjectProperty<SingleSubtitleLoader> singleSubtitleLoader
    ) {
        Hyperlink result = new Hyperlink("load");

        result.visibleProperty().bind(subtitleOption.sizeProperty().isEqualTo(UNKNOWN_SIZE));

        result.setOnAction(event -> {
            SingleSubtitleLoader loader = singleSubtitleLoader.get();
            if (loader == null) {
                return;
            }

            loader.loadSubtitles(subtitleOption, fileInfo);
        });

        return result;
    }

    private static Pane generateSelectOptionPane(TableSubtitleOption subtitleOption) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER);
        result.setSpacing(5);

        GuiUtils.setFixedWidth(result, SELECT_OPTION_PANE_WIDTH);

        setSelectOptionPaneTooltip(result, subtitleOption.getUnavailabilityReason());
        subtitleOption.unavailabilityReasonProperty().addListener(
                observable -> setSelectOptionPaneTooltip(result, subtitleOption.getUnavailabilityReason())
        );

        RadioButton upper = new RadioButton("upper");
        upper.selectedProperty().bindBidirectional(subtitleOption.selectedAsUpperProperty());
        upper.disableProperty().bind(subtitleOption.unavailabilityReasonProperty().isNotNull());

        RadioButton lower = new RadioButton("lower");
        lower.selectedProperty().bindBidirectional(subtitleOption.selectedAsLowerProperty());
        lower.disableProperty().bind(subtitleOption.unavailabilityReasonProperty().isNotNull());

        result.getChildren().addAll(upper, lower);

        return result;
    }

    private static void setSelectOptionPaneTooltip(
            Pane selectOptionPane,
            TableSubtitleOption.UnavailabilityReason unavailabilityReason
    ) {
        if (unavailabilityReason == null) {
            Tooltip.install(selectOptionPane, null);
        } else {
            Tooltip tooltip = GuiUtils.generateTooltip(unavailabilityReasonToString(unavailabilityReason));
            Tooltip.install(selectOptionPane, tooltip);
        }
    }

    private static String unavailabilityReasonToString(TableSubtitleOption.UnavailabilityReason reason) {
        if (reason == null) {
            return "";
        }

        switch (reason) {
            case NOT_ALLOWED_CODEC:
                return "Subtitle has a not allowed type";
            case INCORRECT_FORMAT:
                return "Subtitles have an incorrect format";
            default:
                throw new IllegalStateException();
        }
    }

    private static Pane generateRowWithActionsPane(
            TableFileInfo fileInfo,
            ObjectProperty<AddFileWithSubtitlesHandler> addFileWithSubtitlesHandler,
            ObjectProperty<AllFileSubtitleLoader> allFileSubtitleLoader,
            ObjectProperty<MergedSubtitlePreviewHandler> mergedSubtitlePreviewHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(15);

        result.getChildren().addAll(
                generateShowHideAddFilePane(fileInfo, addFileWithSubtitlesHandler),
                generateLoadAllSubtitlesPane(fileInfo, allFileSubtitleLoader),
                generateMergedPreviewPane(fileInfo, mergedSubtitlePreviewHandler)
        );

        HBox.setHgrow(result.getChildren().get(0), Priority.ALWAYS);

        return result;
    }

    private static Pane generateShowHideAddFilePane(
            TableFileInfo fileInfo,
            ObjectProperty<AddFileWithSubtitlesHandler> addFileWithSubtitlesHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setMinWidth(OPTION_TITLE_PANE_MIN_WIDTH);
        result.setSpacing(25);

        @SuppressWarnings("SimplifyOptionalCallChains")
        Hyperlink showHideLink = generateShowHideLink(fileInfo).orElse(null);
        if (showHideLink != null) {
            result.getChildren().add(showHideLink);
        }

        result.getChildren().add(generateAddFileButton(fileInfo, addFileWithSubtitlesHandler));

        return result;
    }

    private static Optional<Hyperlink> generateShowHideLink(TableFileInfo fileInfo) {
        if (fileInfo.getHideableOptionCount() == 0) {
            return Optional.empty();
        }

        Hyperlink result = new Hyperlink();

        result.textProperty().bind(
                Bindings.when(fileInfo.someOptionsHiddenProperty())
                        .then("show " + fileInfo.getHideableOptionCount() + " hidden")
                        .otherwise("hide extra")
        );
        result.setOnAction(event -> fileInfo.setSomeOptionsHidden(!fileInfo.isSomeOptionsHidden()));

        return Optional.of(result);
    }

    private static Button generateAddFileButton(
            TableFileInfo fileInfo,
            ObjectProperty<AddFileWithSubtitlesHandler> addFileWithSubtitlesHandler
    ) {
        Button result = GuiUtils.createImageButton(
                "Add subtitles",
                "/gui/icons/add.png",
                9,
                9
        );

        int optionCount = fileInfo.getSubtitleOptions().size();
        TableSubtitleOption lastButOneOption = fileInfo.getSubtitleOptions().get(optionCount - 2);
        TableSubtitleOption lastOption = fileInfo.getSubtitleOptions().get(optionCount - 1);

        BooleanBinding canAddMoreFiles = lastButOneOption.titleProperty().isEmpty()
                .or(lastOption.titleProperty().isEmpty());
        result.visibleProperty().bind(canAddMoreFiles);

        result.setOnAction(event -> {
            AddFileWithSubtitlesHandler handler = addFileWithSubtitlesHandler.get();
            if (handler == null) {
                return;
            }

            handler.addFile(fileInfo);
        });

        return result;
    }

    private static Pane generateLoadAllSubtitlesPane(
            TableFileInfo fileInfo,
            ObjectProperty<AllFileSubtitleLoader> allFileSubtitleLoader
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER);
        GuiUtils.setFixedWidth(result, SIZE_AND_PREVIEW_PANE_WIDTH);

        Hyperlink loadAllLink = new Hyperlink("load all subtitles");
        loadAllLink.visibleProperty().bind(fileInfo.optionsWithUnknownSizeCountProperty().greaterThan(1));
        loadAllLink.setOnAction(event -> {
            AllFileSubtitleLoader loader = allFileSubtitleLoader.get();
            if (loader == null) {
                return;
            }

            loader.loadSubtitles(fileInfo);
        });

        result.getChildren().add(loadAllLink);

        return result;
    }

    private static Pane generateMergedPreviewPane(
            TableFileInfo fileInfo,
            ObjectProperty<MergedSubtitlePreviewHandler> mergedSubtitlePreviewHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER);
        GuiUtils.setFixedWidth(result, SELECT_OPTION_PANE_WIDTH);
        result.visibleProperty().bind(fileInfo.visibleOptionCountProperty().greaterThanOrEqualTo(2));

        Button previewButton = GuiUtils.createImageButton(
                "result preview",
                "/gui/icons/eye.png",
                15,
                10
        );

        previewButton.setOnAction(event -> {
            MergedSubtitlePreviewHandler handler = mergedSubtitlePreviewHandler.get();
            if (handler == null) {
                return;
            }

            handler.showPreview(fileInfo);
        });

        setMergedPreviewDisabledAndTooltip(previewButton, result, fileInfo);

        InvalidationListener listener = observable ->
                setMergedPreviewDisabledAndTooltip(previewButton, result, fileInfo);

        fileInfo.upperOptionProperty().addListener(listener);
        fileInfo.lowerOptionProperty().addListener(listener);

        result.getChildren().add(previewButton);

        return result;
    }

    private static void setMergedPreviewDisabledAndTooltip(
            Button previewButton,
            Pane previewPane,
            TableFileInfo fileInfo
    ) {
        TableSubtitleOption upperOption = fileInfo.getUpperOption();
        TableSubtitleOption lowerOption = fileInfo.getLowerOption();

        if (upperOption == null || lowerOption == null) {
            previewButton.setDisable(true);
            Tooltip tooltip = GuiUtils.generateTooltip("Please select subtitles to merge first");
            Tooltip.install(previewPane, tooltip);
        } else {
            previewButton.setDisable(false);
            Tooltip.install(previewPane, null);
        }
    }

    private static ActionResultLabels generateActionResultLabels(TableFileInfo fileInfo) {
        ActionResultLabels result = new ActionResultLabels();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setWrapText(true);

        setActionResultLabels(result, fileInfo);
        fileInfo.actionResultProperty().addListener(observable -> setActionResultLabels(result, fileInfo));

        return result;
    }

    private static void setActionResultLabels(ActionResultLabels actionResultLabels, TableFileInfo fileInfo) {
        if (fileInfo.getActionResult() == null) {
            return;
        }

        actionResultLabels.set(fileInfo.getActionResult());
    }

    public void setSelectedAsUpper(TableSubtitleOption subtitleOption) {
        subtitleOption.setSelectedAsUpper(true);
    }

    public void setSelectedAsLower(TableSubtitleOption subtitleOption) {
        subtitleOption.setSelectedAsLower(true);
    }

    private static ContextMenu generateContextMenu(ToggleGroup sortByGroup, ToggleGroup sortDirectionGroup) {
        ContextMenu result = new ContextMenu();

        Menu menu = new Menu("_Sort files");

        RadioMenuItem byName = new RadioMenuItem("By _Name");
        byName.setToggleGroup(sortByGroup);
        byName.setUserData(SortBy.NAME);

        RadioMenuItem byModificationTime = new RadioMenuItem("By _Modification Time");
        byModificationTime.setToggleGroup(sortByGroup);
        byModificationTime.setUserData(SortBy.MODIFICATION_TIME);

        RadioMenuItem bySize = new RadioMenuItem("By _Size");
        bySize.setToggleGroup(sortByGroup);
        bySize.setUserData(SortBy.SIZE);

        RadioMenuItem ascending = new RadioMenuItem("_Ascending");
        ascending.setToggleGroup(sortDirectionGroup);
        ascending.setUserData(SortDirection.ASCENDING);

        RadioMenuItem descending = new RadioMenuItem("_Descending");
        descending.setToggleGroup(sortDirectionGroup);
        descending.setUserData(SortDirection.DESCENDING);

        menu.getItems().addAll(
                byName,
                byModificationTime,
                bySize,
                new SeparatorMenuItem(),
                ascending,
                descending
        );

        result.getItems().add(menu);

        return result;
    }

    public int getAllSelectedCount() {
        return allSelectedCount.get();
    }

    public ReadOnlyIntegerProperty allSelectedCountProperty() {
        return allSelectedCount.getReadOnlyProperty();
    }

    public AllSelectedHandler getAllSelectedHandler() {
        return allSelectedHandler.get();
    }

    public ObjectProperty<AllSelectedHandler> allSelectedHandlerProperty() {
        return allSelectedHandler;
    }

    public void setAllSelectedHandler(AllSelectedHandler allSelectedHandler) {
        this.allSelectedHandler.set(allSelectedHandler);
    }

    public SortByChangeHandler getSortByChangeHandler() {
        return sortByChangeHandler.get();
    }

    public ObjectProperty<SortByChangeHandler> sortByChangeHandlerProperty() {
        return sortByChangeHandler;
    }

    public void setSortByChangeHandler(SortByChangeHandler sortByChangeHandler) {
        this.sortByChangeHandler.set(sortByChangeHandler);
    }

    public SortDirectionChangeHandler getSortDirectionChangeHandler() {
        return sortDirectionChangeHandler.get();
    }

    public ObjectProperty<SortDirectionChangeHandler> sortDirectionChangeHandlerProperty() {
        return sortDirectionChangeHandler;
    }

    public void setSortDirectionChangeHandler(SortDirectionChangeHandler sortDirectionChangeHandler) {
        this.sortDirectionChangeHandler.set(sortDirectionChangeHandler);
    }

    public RemoveSubtitleOptionHandler getRemoveSubtitleOptionHandler() {
        return removeSubtitleOptionHandler.get();
    }

    public ObjectProperty<RemoveSubtitleOptionHandler> removeSubtitleOptionHandlerProperty() {
        return removeSubtitleOptionHandler;
    }

    public void setRemoveSubtitleOptionHandler(RemoveSubtitleOptionHandler removeSubtitleOptionHandler) {
        this.removeSubtitleOptionHandler.set(removeSubtitleOptionHandler);
    }

    public SingleSubtitleLoader getSingleSubtitleLoader() {
        return singleSubtitleLoader.get();
    }

    public ObjectProperty<SingleSubtitleLoader> singleSubtitleLoaderProperty() {
        return singleSubtitleLoader;
    }

    public void setSingleSubtitleLoader(SingleSubtitleLoader singleSubtitleLoader) {
        this.singleSubtitleLoader.set(singleSubtitleLoader);
    }

    public SubtitleOptionPreviewHandler getSubtitleOptionPreviewHandler() {
        return subtitleOptionPreviewHandler.get();
    }

    public ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandlerProperty() {
        return subtitleOptionPreviewHandler;
    }

    public void setSubtitleOptionPreviewHandler(SubtitleOptionPreviewHandler subtitleOptionPreviewHandler) {
        this.subtitleOptionPreviewHandler.set(subtitleOptionPreviewHandler);
    }

    public AddFileWithSubtitlesHandler getAddFileWithSubtitlesHandler() {
        return addFileWithSubtitlesHandler.get();
    }

    public ObjectProperty<AddFileWithSubtitlesHandler> addFileWithSubtitlesHandlerProperty() {
        return addFileWithSubtitlesHandler;
    }

    public void setAddFileWithSubtitlesHandler(AddFileWithSubtitlesHandler addFileWithSubtitlesHandler) {
        this.addFileWithSubtitlesHandler.set(addFileWithSubtitlesHandler);
    }

    public AllFileSubtitleLoader getAllFileSubtitleLoader() {
        return allFileSubtitleLoader.get();
    }

    public ObjectProperty<AllFileSubtitleLoader> allFileSubtitleLoaderProperty() {
        return allFileSubtitleLoader;
    }

    public void setAllFileSubtitleLoader(AllFileSubtitleLoader allFileSubtitleLoader) {
        this.allFileSubtitleLoader.set(allFileSubtitleLoader);
    }

    public MergedSubtitlePreviewHandler getMergedSubtitlePreviewHandler() {
        return mergedSubtitlePreviewHandler.get();
    }

    public ObjectProperty<MergedSubtitlePreviewHandler> mergedSubtitlePreviewHandlerProperty() {
        return mergedSubtitlePreviewHandler;
    }

    public void setMergedSubtitlePreviewHandler(MergedSubtitlePreviewHandler mergedSubtitlePreviewHandler) {
        this.mergedSubtitlePreviewHandler.set(mergedSubtitlePreviewHandler);
    }

    public void setFilesInfo(
            List<TableFileInfo> filesInfo,
            SortBy sortBy,
            SortDirection sortDirection,
            int allSelectableCount,
            int selectedAvailableCount,
            int selectedUnavailableCount,
            Mode mode,
            boolean clearCache
    ) {
        if (clearCache) {
            clearCache();
        }

        updateSortToggles(sortBy, sortDirection);
        this.allSelectableCount = allSelectableCount;
        this.selectedAvailableCount = selectedAvailableCount;
        this.selectedUnavailableCount = selectedUnavailableCount;

        /*
         * It's very important that this line goes after the modification of the previous counters
         * (selectedAvailableCount and selectedUnavailableCount) because this property will have subscribers and they
         * need updated counter values there.
         */
        this.allSelectedCount.setValue(selectedAvailableCount + selectedUnavailableCount);

        allSelectedCheckBox.setSelected(getAllSelectedCount() > 0 && getAllSelectedCount() == allSelectableCount);
        this.mode = mode;

        /*
         * Have to set items this way because otherwise table may not be refreshed
         * https://stackoverflow.com/questions/11065140/javafx-2-1-tableview-refresh-items
         * I tried to use refresh() method instead but it works incorrectly - sometimes after the call first row
         * is truncated.
         */
        getItems().removeAll(getItems());
        setItems(FXCollections.observableArrayList(filesInfo));
    }

    private void clearCache() {
        cellCache.clear();
    }

    private void updateSortToggles(SortBy sortBy, SortDirection sortDirection) {
        for (Toggle toggle : sortByGroup.getToggles()) {
            if (Objects.equals(toggle.getUserData(), sortBy)) {
                toggle.setSelected(true);
            }
        }

        for (Toggle toggle : sortDirectionGroup.getToggles()) {
            if (Objects.equals(toggle.getUserData(), sortDirection)) {
                toggle.setSelected(true);
            }
        }
    }

    public void clearTable() {
        clearCache();
        sortByGroup.selectToggle(null);
        sortDirectionGroup.selectToggle(null);
        allSelectableCount = 0;
        selectedAvailableCount = 0;
        selectedUnavailableCount = 0;

        /*
         * It's very important that this line goes after the modification of the previous counters
         * (selectedAvailableCount and selectedUnavailableCount) because this property will have subscribers and they
         * need updated counter values there.
         */
        allSelectedCount.setValue(0);

        allSelectedCheckBox.setSelected(false);
        this.mode = null;
        setItems(FXCollections.emptyObservableList());
        System.gc();
    }

    public void setSelected(boolean selected, TableFileInfo fileInfo) {
        if (fileInfo.isSelected() == selected) {
            return;
        }

        fileInfo.setSelected(selected);

        handleFileSelectionChange(selected, fileInfo);
    }

    public void clearActionResult(TableFileInfo fileInfo) {
        setActionResult(ActionResult.NO_RESULT, fileInfo);
    }

    public void setActionResult(ActionResult actionResult, TableFileInfo fileInfo) {
        fileInfo.setActionResult(actionResult);
    }

    public void removeFileWithSubtitles(TableSubtitleOption option, TableFileInfo fileInfo) {
        if (!option.isRemovable()) {
            log.error("option " + option.getId() + " is not removable");
            throw new IllegalStateException();
        }

        option.setId(null);
        option.setTitle(null);
        option.setSize(UNKNOWN_SIZE);
        option.setUnavailabilityReason(null);
        option.setSelectedAsUpper(false);
        option.setSelectedAsLower(false);

        fileInfo.setVisibleOptionCount(fileInfo.getVisibleOptionCount() - 1);

        TableSubtitleOption upperOption = fileInfo.getUpperOption();
        if (upperOption != null && Objects.equals(option.getId(), upperOption.getId())) {
            fileInfo.setUpperOption(null);
        }

        TableSubtitleOption lowerOption = fileInfo.getLowerOption();
        if (lowerOption != null && Objects.equals(option.getId(), lowerOption.getId())) {
            fileInfo.setLowerOption(null);
        }

        fileInfo.setActionResult(ActionResult.onlySuccess("Subtitle file has been removed from the list successfully"));
    }

    public void subtitlesLoadedSuccessfully(int size, TableSubtitleOption subtitleOption, TableFileInfo fileInfo) {
        subtitleOption.setSize(size);
        subtitleOption.setFailedToLoadReason(null);
        fileInfo.setOptionsWithUnknownSizeCount(fileInfo.getOptionsWithUnknownSizeCount() - 1);
    }

    public void failedToLoadSubtitles(
            String failedToLoadReason,
            TableSubtitleOption subtitleOption
    ) {
        subtitleOption.setSize(UNKNOWN_SIZE);
        subtitleOption.setFailedToLoadReason(failedToLoadReason);
    }

    public void subtitleOptionPreviewClosed(
            TableSubtitleOption.UnavailabilityReason unavailabilityReason,
            TableSubtitleOption subtitleOption
    ) {
        subtitleOption.setUnavailabilityReason(unavailabilityReason);
    }

    public void failedToAddFileWithSubtitles(FileWithSubtitlesUnavailabilityReason reason, TableFileInfo fileInfo) {
        fileInfo.setActionResult(ActionResult.onlyError(unavailabilityReasonToString(reason)));
    }

    private static String unavailabilityReasonToString(FileWithSubtitlesUnavailabilityReason reason) {
        switch (reason) {
            case DUPLICATE:
                return "This file has already been added";
            case PATH_IS_TOO_LONG:
                return "File path is too long";
            case INVALID_PATH:
                return "File path is invalid";
            case IS_A_DIRECTORY:
                return "Is a directory, not a file";
            case FILE_DOES_NOT_EXIST:
                return "File doesn't exist";
            case FAILED_TO_GET_PARENT_DIRECTORY:
                return "Failed to get parent directory for the file";
            case EXTENSION_IS_NOT_VALID:
                return "File has an incorrect extension";
            case FILE_IS_EMPTY:
                return "File is empty";
            case FILE_IS_TOO_BIG:
                return "File is too big (>" + LogicConstants.INPUT_SUBTITLE_FILE_LIMIT_MEGABYTES + " megabytes)";
            case FAILED_TO_READ_CONTENT:
                return "Failed to read the file";
            default:
                throw new IllegalStateException();
        }
    }

    public void addFileWithSubtitles(
            String id,
            String title,
            boolean incorrectFormat,
            int size,
            TableFileInfo fileInfo
    ) {
        TableSubtitleOption subtitleOption = getFirstEmptySubtitleOption(fileInfo);

        subtitleOption.setId(id);
        subtitleOption.setTitle(title);
        subtitleOption.setSize(size);
        subtitleOption.setUnavailabilityReason(
                incorrectFormat ? TableSubtitleOption.UnavailabilityReason.INCORRECT_FORMAT : null
        );

        fileInfo.setVisibleOptionCount(fileInfo.getVisibleOptionCount() + 1);

        if (incorrectFormat) {
            fileInfo.setActionResult(
                    ActionResult.onlyWarn(
                            "File was added but it has an incorrect subtitle format, you can try and change the "
                                    + "encoding pressing the preview button"
                    )
            );
        } else {
            fileInfo.setActionResult(ActionResult.onlySuccess("Subtitle file has been added to the list successfully"));
        }
    }

    private TableSubtitleOption getFirstEmptySubtitleOption(TableFileInfo fileInfo) {
        for (TableSubtitleOption option : fileInfo.getSubtitleOptions()) {
            if (!option.isRemovable()) {
                continue;
            }

            if (StringUtils.isBlank(option.getTitle())) {
                return option;
            }
        }

        log.error("no empty options, that shouldn't happen");
        throw new IllegalStateException();
    }

    public enum Mode {
        SEPARATE_FILES,
        DIRECTORY
    }

    @AllArgsConstructor
    @Getter
    public enum SortBy {
        NAME,
        MODIFICATION_TIME,
        SIZE
    }

    @AllArgsConstructor
    @Getter
    public enum SortDirection {
        ASCENDING,
        DESCENDING
    }

    @FunctionalInterface
    public interface AllSelectedHandler {
        void handle(boolean allSelected);
    }

    @FunctionalInterface
    public interface SortByChangeHandler {
        void handle(SortBy sortBy);
    }

    @FunctionalInterface
    public interface SortDirectionChangeHandler {
        void handle(SortDirection sortDirection);
    }

    @FunctionalInterface
    public interface RemoveSubtitleOptionHandler {
        void remove(TableSubtitleOption subtitleOption, TableFileInfo fileInfo);
    }

    @FunctionalInterface
    public interface SingleSubtitleLoader {
        void loadSubtitles(TableSubtitleOption subtitleOption, TableFileInfo fileInfo);
    }

    @FunctionalInterface
    public interface SubtitleOptionPreviewHandler {
        void showPreview(TableSubtitleOption subtitleOption, TableFileInfo fileInfo);
    }

    @FunctionalInterface
    public interface AddFileWithSubtitlesHandler {
        void addFile(TableFileInfo fileInfo);
    }

    public enum FileWithSubtitlesUnavailabilityReason {
        DUPLICATE,
        PATH_IS_TOO_LONG,
        INVALID_PATH,
        IS_A_DIRECTORY,
        FILE_DOES_NOT_EXIST,
        FAILED_TO_GET_PARENT_DIRECTORY,
        EXTENSION_IS_NOT_VALID,
        FILE_IS_EMPTY,
        FILE_IS_TOO_BIG,
        FAILED_TO_READ_CONTENT
    }

    @FunctionalInterface
    public interface AllFileSubtitleLoader {
        void loadSubtitles(TableFileInfo fileInfo);
    }

    @FunctionalInterface
    public interface MergedSubtitlePreviewHandler {
        void showPreview(TableFileInfo fileInfo);
    }

    @AllArgsConstructor
    private class TableWithFilesCell<T> extends TableCell<TableFileInfo, T> {
        private CellType cellType;

        private CellPaneGenerator cellPaneGenerator;

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            TableFileInfo fileInfo = getTableRow().getItem();

            if (empty || fileInfo == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Pane pane = null;
            Map<CellType, Pane> fileInfoPanes = cellCache.get(fileInfo.getId());
            if (fileInfoPanes != null) {
                pane = cellCache.get(fileInfo.getId()).get(cellType);
            }

            if (pane == null) {
                Pane newPane = cellPaneGenerator.generatePane(fileInfo);

                if (fileInfo.getUnavailabilityReason() != null) {
                    if (cellType != CellType.SELECTED) {
                        newPane.getStyleClass().add(PANE_UNAVAILABLE_CLASS);
                    }
                } else {
                    setOrRemoveErrorClass(fileInfo, newPane);
                    fileInfo.actionResultProperty().addListener(observable -> setOrRemoveErrorClass(fileInfo, newPane));
                }

                pane = newPane;
                cellCache.putIfAbsent(fileInfo.getId(), new HashMap<>());
                cellCache.get(fileInfo.getId()).put(cellType, pane);
            }

            setGraphic(pane);
            setText(null);
        }

        private void setOrRemoveErrorClass(TableFileInfo fileInfo, Pane pane) {
            if (fileInfo.getActionResult() != null && !StringUtils.isBlank(fileInfo.getActionResult().getError())) {
                pane.getStyleClass().add(PANE_ERROR_CLASS);
            } else {
                pane.getStyleClass().remove(PANE_ERROR_CLASS);
            }
        }
    }

    @FunctionalInterface
    interface CellPaneGenerator {
        Pane generatePane(TableFileInfo fileInfo);
    }

    private enum CellType {
        SELECTED,
        FILE_DESCRIPTION,
        SUBTITLES
    }
}
