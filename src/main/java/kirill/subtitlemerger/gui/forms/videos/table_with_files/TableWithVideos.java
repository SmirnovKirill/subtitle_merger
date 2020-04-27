package kirill.subtitlemerger.gui.forms.videos.table_with_files;

import javafx.beans.InvalidationListener;
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
import kirill.subtitlemerger.gui.common_controls.ActionResultPane;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;

import static kirill.subtitlemerger.gui.forms.videos.table_with_files.TableSubtitleOption.UNKNOWN_SIZE;

@CommonsLog
public class TableWithVideos extends TableView<TableVideoInfo> {
    private static final String PANE_UNAVAILABLE_CLASS = "pane-unavailable";

    private static final String PANE_ERROR_CLASS = "pane-error";

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

    private ReadOnlyIntegerWrapper selectedCount;

    private CheckBox selectAllCheckBox;

    @Getter
    private int selectableCount;

    @Getter
    private int selectedAvailableCount;

    @Getter
    private int selectedUnavailableCount;

    private ObjectProperty<SelectAllHandler> selectAllHandler;

    private ToggleGroup sortByGroup;

    private ToggleGroup sortDirectionGroup;

    private ObjectProperty<SortChangeHandler> sortChangeHandler;

    private ObjectProperty<RemoveSubtitleOptionHandler> removeSubtitleOptionHandler;

    private ObjectProperty<SingleSubtitleLoader> singleSubtitleLoader;

    private ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandler;

    private ObjectProperty<AddFileWithSubtitlesHandler> addFileWithSubtitlesHandler;

    private ObjectProperty<AllFileSubtitleLoader> allFileSubtitleLoader;

    private ObjectProperty<MergedSubtitlePreviewHandler> mergedSubtitlePreviewHandler;

    public TableWithVideos() {
        cellCache = new HashMap<>();

        selectedCount = new ReadOnlyIntegerWrapper();
        selectAllHandler = new SimpleObjectProperty<>();
        selectAllCheckBox = getSelectAllCheckBox(selectAllHandler);

        sortByGroup = new ToggleGroup();

        sortDirectionGroup = new ToggleGroup();

        sortChangeHandler = new SimpleObjectProperty<>();

        removeSubtitleOptionHandler = new SimpleObjectProperty<>();
        singleSubtitleLoader = new SimpleObjectProperty<>();
        subtitleOptionPreviewHandler = new SimpleObjectProperty<>();
        addFileWithSubtitlesHandler = new SimpleObjectProperty<>();
        allFileSubtitleLoader = new SimpleObjectProperty<>();
        mergedSubtitlePreviewHandler = new SimpleObjectProperty<>();

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addColumns(selectAllCheckBox);

        setContextMenu(getContextMenu(sortByGroup, sortDirectionGroup, sortChangeHandler));
        setPlaceholder(new Label("There are no files to display"));
        setSelectionModel(null);
    }

    private static CheckBox getSelectAllCheckBox(ObjectProperty<SelectAllHandler> selectAllHandler) {
        CheckBox result = new CheckBox();

        result.setOnAction(event -> selectAllHandler.get().handle(result.isSelected()));

        return result;
    }

    private void addColumns(CheckBox allSelectedCheckBox) {
        TableColumn<TableVideoInfo, ?> selectedColumn = new TableColumn<>();
        selectedColumn.setCellFactory(
                column -> new TableWithFilesCell<>(CellType.SELECTED, this::getSelectedCellPane)
        );
        selectedColumn.setGraphic(allSelectedCheckBox);
        selectedColumn.setMaxWidth(26);
        selectedColumn.setMinWidth(26);
        selectedColumn.setReorderable(false);
        selectedColumn.setResizable(false);
        selectedColumn.setSortable(false);

        TableColumn<TableVideoInfo, ?> fileDescriptionColumn = new TableColumn<>("file");
        fileDescriptionColumn.setCellFactory(
                column -> new TableWithFilesCell<>(CellType.FILE_DESCRIPTION, this::getFileDescriptionCellPane)
        );
        fileDescriptionColumn.setMinWidth(200);
        fileDescriptionColumn.setReorderable(false);
        fileDescriptionColumn.setSortable(false);

        TableColumn<TableVideoInfo, ?> subtitleColumn = new TableColumn<>("subtitles");
        subtitleColumn.setCellFactory(
                column -> new TableWithFilesCell<>(CellType.SUBTITLES, this::getSubtitleCellPane)
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

    private Pane getSelectedCellPane(TableVideoInfo fileInfo) {
        HBox result = new HBox();

        result.setPadding(new Insets(CELL_PADDING, 0, CELL_PADDING, 0));
        result.setAlignment(Pos.TOP_CENTER);

        /*
         * We should stop here if in the directory mode, checkbox isn't needed because there is no point in selecting an
         * unavailable file. On the contrary, in the files mode it's possible to select the unavailable file to remove
         * it. Because of the ability to remove the file the behaviour is different.
         */
        if (!StringUtils.isBlank(fileInfo.getNotValidReason()) && mode == Mode.DIRECTORY) {
            return result;
        }

        CheckBox selectedCheckBox = new CheckBox();

        selectedCheckBox.selectedProperty().bindBidirectional(fileInfo.selectedProperty());
        selectedCheckBox.setOnAction(event -> handleFileSelectionChange(selectedCheckBox.isSelected(), fileInfo));

        result.getChildren().add(selectedCheckBox);

        return result;
    }

    private void handleFileSelectionChange(boolean selected, TableVideoInfo fileInfo) {
        int addValue = selected ? 1 : -1;

        if (StringUtils.isBlank(fileInfo.getNotValidReason())) {
            selectedAvailableCount += addValue;
        } else {
            selectedUnavailableCount += addValue;
        }

        /*
         * It's very important that this line goes after the modification of the previous counters
         * (selectedAvailableCount and selectedUnavailableCount) because this property will have subscribers and they
         * need updated counter values there.
         */
        selectedCount.setValue(getSelectedCount() + addValue);

        selectAllCheckBox.setSelected(getSelectedCount() > 0 && getSelectedCount() == selectableCount);
    }

    private Pane getFileDescriptionCellPane(TableVideoInfo fileInfo) {
        VBox result = new VBox();

        result.setPadding(new Insets(CELL_PADDING, CELL_PADDING + 1, CELL_PADDING, CELL_PADDING));
        result.setSpacing(10);

        Label pathLabel = new Label(fileInfo.getFilePath());
        pathLabel.getStyleClass().add("path-label");

        Pane sizeAndLastModifiedPane = getSizeAndLastModifiedPane(fileInfo);

        result.getChildren().addAll(pathLabel, sizeAndLastModifiedPane);

        return result;
    }

    private static Pane getSizeAndLastModifiedPane(TableVideoInfo fileInfo) {
        GridPane result = new GridPane();

        result.setHgap(30);
        result.setGridLinesVisible(GuiConstants.GRID_LINES_VISIBLE);

        Label sizeTitle = new Label("size");
        Label lastModifiedTitle = new Label("last modified");

        Label size = new Label(Utils.getFileSizeTextual(fileInfo.getSize(), false));
        Label lastModified = new Label(FORMATTER.print(fileInfo.getLastModified()));

        result.addRow(0, sizeTitle, size);
        result.addRow(1, lastModifiedTitle, lastModified);

        GridPane.setMargin(sizeTitle, new Insets(0, 0, 3, 0));
        GridPane.setMargin(size, new Insets(0, 0, 3, 0));

        return result;
    }

    private Pane getSubtitleCellPane(TableVideoInfo fileInfo) {
        if (!StringUtils.isBlank(fileInfo.getNotValidReason())) {
            return getVideoNotValidPane(fileInfo);
        }

        VBox result = new VBox();

        result.setPadding(new Insets(CELL_PADDING, CELL_PADDING, CELL_PADDING, CELL_PADDING + 1));
        result.setSpacing(2);

        for (TableSubtitleOption subtitleOption : fileInfo.getSubtitleOptions()) {
            result.getChildren().addAll(
                    getSubtitleOptionPane(
                            subtitleOption,
                            fileInfo,
                            removeSubtitleOptionHandler,
                            singleSubtitleLoader,
                            subtitleOptionPreviewHandler
                    )
            );
        }

        result.getChildren().addAll(
                GuiUtils.getFixedHeightSpacer(1),
                getRowWithActionsPane(
                        fileInfo,
                        addFileWithSubtitlesHandler,
                        allFileSubtitleLoader,
                        mergedSubtitlePreviewHandler
                ),
                GuiUtils.getFixedHeightSpacer(6),
                getActionResultPane(fileInfo)
        );

        return result;
    }

    private static Pane getVideoNotValidPane(TableVideoInfo fileInfo) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setPadding(new Insets(CELL_PADDING, CELL_PADDING, CELL_PADDING, CELL_PADDING + 1));

        result.getChildren().add(new Label(fileInfo.getNotValidReason()));

        return result;
    }

    private static Pane getSubtitleOptionPane(
            TableSubtitleOption subtitleOption,
            TableVideoInfo fileInfo,
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
                getOptionTitleAndRemovePane(subtitleOption, fileInfo, removeSubtitleOptionHandler),
                getSizeAndPreviewPane(
                        subtitleOption,
                        fileInfo,
                        subtitleOptionPreviewHandler,
                        singleSubtitleLoader
                ),
                getSelectOptionPane(subtitleOption)
        );

        HBox.setHgrow(result.getChildren().get(0), Priority.ALWAYS);

        return result;
    }

    private static Pane getOptionTitleAndRemovePane(
            TableSubtitleOption subtitleOption,
            TableVideoInfo fileInfo,
            ObjectProperty<RemoveSubtitleOptionHandler> removeSubtitleOptionHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setMinWidth(OPTION_TITLE_PANE_MIN_WIDTH);
        result.setSpacing(10);

        result.getChildren().add(getOptionTitleLabel(subtitleOption));

        @SuppressWarnings("SimplifyOptionalCallChains")
        Button removeButton = getRemoveButton(subtitleOption, fileInfo, removeSubtitleOptionHandler).orElse(null);
        if (removeButton != null) {
            result.getChildren().add(removeButton);
        }

        return result;
    }

    private static Label getOptionTitleLabel(TableSubtitleOption subtitleOption) {
        Label result = new Label();

        result.textProperty().bind(subtitleOption.titleProperty());
        result.setMaxWidth(Double.MAX_VALUE);

        return result;
    }

    private static Optional<Button> getRemoveButton(
            TableSubtitleOption subtitleOption,
            TableVideoInfo fileInfo,
            ObjectProperty<RemoveSubtitleOptionHandler> removeSubtitleOptionHandler
    ) {
        if (!subtitleOption.isRemovable()) {
            return Optional.empty();
        }

        Button result = GuiUtils.getImageButton(
                null,
                "/gui/icons/remove.png",
                8,
                8
        );

        result.setOnAction(event -> removeSubtitleOptionHandler.get().remove(subtitleOption, fileInfo));

        return Optional.of(result);
    }

    private static Pane getSizeAndPreviewPane(
            TableSubtitleOption subtitleOption,
            TableVideoInfo fileInfo,
            ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandler,
            ObjectProperty<SingleSubtitleLoader> singleSubtitleLoader
    ) {
        StackPane result = new StackPane();

        GuiUtils.setFixedWidth(result, SIZE_AND_PREVIEW_PANE_WIDTH);

        Pane knownSizeAndPreviewPane = getKnownSizeAndPreviewPane(
                subtitleOption,
                fileInfo,
                subtitleOptionPreviewHandler
        );
        knownSizeAndPreviewPane.visibleProperty().bind(subtitleOption.sizeProperty().isNotEqualTo(UNKNOWN_SIZE));

        Pane unknownSizePane = getUnknownSizePane(subtitleOption, fileInfo, singleSubtitleLoader);
        unknownSizePane.visibleProperty().bind(subtitleOption.sizeProperty().isEqualTo(UNKNOWN_SIZE));

        result.getChildren().addAll(knownSizeAndPreviewPane, unknownSizePane);

        return result;
    }

    private static Pane getKnownSizeAndPreviewPane(
            TableSubtitleOption subtitleOption,
            TableVideoInfo fileInfo,
            ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);

        Label sizeLabel = new Label();
        sizeLabel.textProperty().bind(
                Bindings.createStringBinding(() ->
                        "Size: " + Utils.getFileSizeTextual(subtitleOption.getSize(), true),
                        subtitleOption.sizeProperty()
                )
        );

        Region spacer = new Region();

        result.getChildren().addAll(
                sizeLabel,
                spacer,
                getPreviewButton(subtitleOption, fileInfo, subtitleOptionPreviewHandler)
        );
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return result;
    }

    private static Button getPreviewButton(
            TableSubtitleOption subtitleOption,
            TableVideoInfo fileInfo,
            ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandler
    ) {
        Button result = GuiUtils.getImageButton("", "/gui/icons/eye.png", 15, 10);

        result.setOnAction(event -> subtitleOptionPreviewHandler.get().showPreview(subtitleOption, fileInfo));

        return result;
    }

    private static Pane getUnknownSizePane(
            TableSubtitleOption subtitleOption,
            TableVideoInfo fileInfo,
            ObjectProperty<SingleSubtitleLoader> singleSubtitleLoader
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();

        result.getChildren().add(getSizeAndFailedToLoadPane(subtitleOption));

        if (StringUtils.isBlank(subtitleOption.getNotValidReason())) {
            result.getChildren().addAll(
                    spacer,
                    getLoadSubtitleLink(subtitleOption, fileInfo, singleSubtitleLoader)
            );
            HBox.setHgrow(spacer, Priority.ALWAYS);
        }

        return result;
    }

    private static Pane getSizeAndFailedToLoadPane(TableSubtitleOption subtitleOption) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(5);

        result.getChildren().addAll(new Label("Size: ? KB"), getFailedToLoadLabel(subtitleOption));

        return result;
    }

    private static Label getFailedToLoadLabel(TableSubtitleOption subtitleOption) {
        Label result = new Label();

        result.setGraphic(GuiUtils.getImageView("/gui/icons/error.png", 12, 12));

        result.setTooltip(GuiUtils.getTooltip(subtitleOption.failedToLoadReasonProperty()));
        GuiUtils.bindVisibleAndManaged(result, subtitleOption.failedToLoadReasonProperty().isNotNull());

        return result;
    }

    private static Hyperlink getLoadSubtitleLink(
            TableSubtitleOption subtitleOption,
            TableVideoInfo fileInfo,
            ObjectProperty<SingleSubtitleLoader> singleSubtitleLoader
    ) {
        Hyperlink result = new Hyperlink("load");

        result.visibleProperty().bind(subtitleOption.sizeProperty().isEqualTo(UNKNOWN_SIZE));

        result.setOnAction(event -> singleSubtitleLoader.get().loadSubtitles(subtitleOption, fileInfo));

        return result;
    }

    private static Pane getSelectOptionPane(TableSubtitleOption subtitleOption) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER);
        result.setSpacing(5);

        GuiUtils.setFixedWidth(result, SELECT_OPTION_PANE_WIDTH);

        setSelectOptionPaneTooltip(result, subtitleOption);
        subtitleOption.notValidReasonProperty().addListener(
                observable -> setSelectOptionPaneTooltip(result, subtitleOption)
        );

        RadioButton upper = new RadioButton("upper");
        upper.selectedProperty().bindBidirectional(subtitleOption.selectedAsUpperProperty());
        upper.disableProperty().bind(subtitleOption.notValidReasonProperty().isNotEmpty());

        RadioButton lower = new RadioButton("lower");
        lower.selectedProperty().bindBidirectional(subtitleOption.selectedAsLowerProperty());
        lower.disableProperty().bind(subtitleOption.notValidReasonProperty().isNotEmpty());

        result.getChildren().addAll(upper, lower);

        return result;
    }

    private static void setSelectOptionPaneTooltip(Pane selectOptionPane, TableSubtitleOption subtitleOption) {
        if (StringUtils.isBlank(subtitleOption.getNotValidReason())) {
            Tooltip.install(selectOptionPane, null);
        } else {
            Tooltip tooltip = GuiUtils.getTooltip(subtitleOption.getNotValidReason());
            Tooltip.install(selectOptionPane, tooltip);
        }
    }

    private static Pane getRowWithActionsPane(
            TableVideoInfo fileInfo,
            ObjectProperty<AddFileWithSubtitlesHandler> addFileWithSubtitlesHandler,
            ObjectProperty<AllFileSubtitleLoader> allFileSubtitleLoader,
            ObjectProperty<MergedSubtitlePreviewHandler> mergedSubtitlePreviewHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(15);

        result.getChildren().addAll(
                getShowHideAddFilePane(fileInfo, addFileWithSubtitlesHandler),
                getLoadAllSubtitlesPane(fileInfo, allFileSubtitleLoader),
                getMergedPreviewPane(fileInfo, mergedSubtitlePreviewHandler)
        );

        HBox.setHgrow(result.getChildren().get(0), Priority.ALWAYS);

        return result;
    }

    private static Pane getShowHideAddFilePane(
            TableVideoInfo fileInfo,
            ObjectProperty<AddFileWithSubtitlesHandler> addFileWithSubtitlesHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setMinWidth(OPTION_TITLE_PANE_MIN_WIDTH);
        result.setSpacing(25);

        @SuppressWarnings("SimplifyOptionalCallChains")
        Hyperlink showHideLink = getShowHideLink(fileInfo).orElse(null);
        if (showHideLink != null) {
            result.getChildren().add(showHideLink);
        }

        result.getChildren().add(getAddFileButton(fileInfo, addFileWithSubtitlesHandler));

        return result;
    }

    private static Optional<Hyperlink> getShowHideLink(TableVideoInfo fileInfo) {
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

    private static Button getAddFileButton(
            TableVideoInfo fileInfo,
            ObjectProperty<AddFileWithSubtitlesHandler> addFileWithSubtitlesHandler
    ) {
        Button result = GuiUtils.getImageButton(
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

        result.setOnAction(event -> addFileWithSubtitlesHandler.get().addFile(fileInfo));

        return result;
    }

    private static Pane getLoadAllSubtitlesPane(
            TableVideoInfo fileInfo,
            ObjectProperty<AllFileSubtitleLoader> allFileSubtitleLoader
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER);
        GuiUtils.setFixedWidth(result, SIZE_AND_PREVIEW_PANE_WIDTH);

        Hyperlink loadAllLink = new Hyperlink("load all subtitles");
        loadAllLink.visibleProperty().bind(fileInfo.optionsWithUnknownSizeCountProperty().greaterThan(1));
        loadAllLink.setOnAction(event -> allFileSubtitleLoader.get().loadSubtitles(fileInfo));

        result.getChildren().add(loadAllLink);

        return result;
    }

    private static Pane getMergedPreviewPane(
            TableVideoInfo fileInfo,
            ObjectProperty<MergedSubtitlePreviewHandler> mergedSubtitlePreviewHandler
    ) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER);
        GuiUtils.setFixedWidth(result, SELECT_OPTION_PANE_WIDTH);
        result.visibleProperty().bind(fileInfo.visibleOptionCountProperty().greaterThanOrEqualTo(2));

        Button previewButton = GuiUtils.getImageButton(
                "result preview",
                "/gui/icons/eye.png",
                15,
                10
        );

        previewButton.setOnAction(event -> mergedSubtitlePreviewHandler.get().showPreview(fileInfo));

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
            TableVideoInfo fileInfo
    ) {
        TableSubtitleOption upperOption = fileInfo.getUpperOption();
        TableSubtitleOption lowerOption = fileInfo.getLowerOption();

        if (upperOption == null || lowerOption == null) {
            previewButton.setDisable(true);
            Tooltip tooltip = GuiUtils.getTooltip("Please select subtitles to merge first");
            Tooltip.install(previewPane, tooltip);
        } else {
            previewButton.setDisable(false);
            Tooltip.install(previewPane, null);
        }
    }

    private static ActionResultPane getActionResultPane(TableVideoInfo fileInfo) {
        ActionResultPane result = new ActionResultPane();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setWrapText(true);

        setActionResultPane(result, fileInfo);
        fileInfo.actionResultProperty().addListener(observable -> setActionResultPane(result, fileInfo));

        return result;
    }

    private static void setActionResultPane(ActionResultPane actionResultPane, TableVideoInfo fileInfo) {
        if (fileInfo.getActionResult() == null) {
            return;
        }

        actionResultPane.set(fileInfo.getActionResult());
    }

    public void setSelectedAsUpper(TableSubtitleOption subtitleOption) {
        subtitleOption.setSelectedAsUpper(true);
    }

    public void setSelectedAsLower(TableSubtitleOption subtitleOption) {
        subtitleOption.setSelectedAsLower(true);
    }

    private static ContextMenu getContextMenu(
            ToggleGroup sortByGroup,
            ToggleGroup sortDirectionGroup,
            ObjectProperty<SortChangeHandler> sortChangeHandler
    ) {
        ContextMenu result = new ContextMenu();

        Menu menu = new Menu("_Sort files");

        RadioMenuItem byName = new RadioMenuItem("By _Name");
        byName.setToggleGroup(sortByGroup);
        byName.setUserData(TableSortBy.NAME);

        RadioMenuItem byModificationTime = new RadioMenuItem("By _Modification Time");
        byModificationTime.setToggleGroup(sortByGroup);
        byModificationTime.setUserData(TableSortBy.MODIFICATION_TIME);

        RadioMenuItem bySize = new RadioMenuItem("By _Size");
        bySize.setToggleGroup(sortByGroup);
        bySize.setUserData(TableSortBy.SIZE);

        RadioMenuItem ascending = new RadioMenuItem("_Ascending");
        ascending.setToggleGroup(sortDirectionGroup);
        ascending.setUserData(TableSortDirection.ASCENDING);

        RadioMenuItem descending = new RadioMenuItem("_Descending");
        descending.setToggleGroup(sortDirectionGroup);
        descending.setUserData(TableSortDirection.DESCENDING);

        menu.getItems().addAll(
                byName,
                byModificationTime,
                bySize,
                new SeparatorMenuItem(),
                ascending,
                descending
        );

        result.getItems().add(menu);

        menu.setOnAction(
                event -> sortChangeHandler.get().handle(
                        (TableSortBy) sortByGroup.getSelectedToggle().getUserData(),
                        (TableSortDirection) sortDirectionGroup.getSelectedToggle().getUserData()
                )
        );

        return result;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public int getSelectedCount() {
        return selectedCount.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyIntegerProperty selectedCountProperty() {
        return selectedCount.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public SelectAllHandler getSelectAllHandler() {
        return selectAllHandler.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<SelectAllHandler> selectAllHandlerProperty() {
        return selectAllHandler;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setSelectAllHandler(SelectAllHandler selectAllHandler) {
        this.selectAllHandler.set(selectAllHandler);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public SortChangeHandler getSortChangeHandler() {
        return sortChangeHandler.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<SortChangeHandler> sortChangeHandlerProperty() {
        return sortChangeHandler;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setSortChangeHandler(SortChangeHandler sortChangeHandler) {
        this.sortChangeHandler.set(sortChangeHandler);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public RemoveSubtitleOptionHandler getRemoveSubtitleOptionHandler() {
        return removeSubtitleOptionHandler.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<RemoveSubtitleOptionHandler> removeSubtitleOptionHandlerProperty() {
        return removeSubtitleOptionHandler;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setRemoveSubtitleOptionHandler(RemoveSubtitleOptionHandler removeSubtitleOptionHandler) {
        this.removeSubtitleOptionHandler.set(removeSubtitleOptionHandler);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public SingleSubtitleLoader getSingleSubtitleLoader() {
        return singleSubtitleLoader.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<SingleSubtitleLoader> singleSubtitleLoaderProperty() {
        return singleSubtitleLoader;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setSingleSubtitleLoader(SingleSubtitleLoader singleSubtitleLoader) {
        this.singleSubtitleLoader.set(singleSubtitleLoader);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public SubtitleOptionPreviewHandler getSubtitleOptionPreviewHandler() {
        return subtitleOptionPreviewHandler.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandlerProperty() {
        return subtitleOptionPreviewHandler;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setSubtitleOptionPreviewHandler(SubtitleOptionPreviewHandler subtitleOptionPreviewHandler) {
        this.subtitleOptionPreviewHandler.set(subtitleOptionPreviewHandler);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public AddFileWithSubtitlesHandler getAddFileWithSubtitlesHandler() {
        return addFileWithSubtitlesHandler.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<AddFileWithSubtitlesHandler> addFileWithSubtitlesHandlerProperty() {
        return addFileWithSubtitlesHandler;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setAddFileWithSubtitlesHandler(AddFileWithSubtitlesHandler addFileWithSubtitlesHandler) {
        this.addFileWithSubtitlesHandler.set(addFileWithSubtitlesHandler);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public AllFileSubtitleLoader getAllFileSubtitleLoader() {
        return allFileSubtitleLoader.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<AllFileSubtitleLoader> allFileSubtitleLoaderProperty() {
        return allFileSubtitleLoader;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setAllFileSubtitleLoader(AllFileSubtitleLoader allFileSubtitleLoader) {
        this.allFileSubtitleLoader.set(allFileSubtitleLoader);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public MergedSubtitlePreviewHandler getMergedSubtitlePreviewHandler() {
        return mergedSubtitlePreviewHandler.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<MergedSubtitlePreviewHandler> mergedSubtitlePreviewHandlerProperty() {
        return mergedSubtitlePreviewHandler;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setMergedSubtitlePreviewHandler(MergedSubtitlePreviewHandler mergedSubtitlePreviewHandler) {
        this.mergedSubtitlePreviewHandler.set(mergedSubtitlePreviewHandler);
    }

    public void setData(TableData data, boolean clearCache) {
        if (clearCache) {
            clearCache();
        }

        updateSortToggles(data.getSortBy(), data.getSortDirection());
        selectableCount = data.getAllSelectableCount();
        selectedAvailableCount = data.getSelectedAvailableCount();
        selectedUnavailableCount = data.getSelectedUnavailableCount();

        /*
         * It's very important that this line goes after the modification of the previous counters
         * (selectedAvailableCount and selectedUnavailableCount) because this property will have subscribers and they
         * need updated counter values there.
         */
        selectedCount.setValue(selectedAvailableCount + selectedUnavailableCount);

        selectAllCheckBox.setSelected(getSelectedCount() > 0 && getSelectedCount() == data.getAllSelectableCount());
        mode = data.getMode();

        /*
         * Have to set items this way because otherwise table may not be refreshed
         * https://stackoverflow.com/questions/11065140/javafx-2-1-tableview-refresh-items
         * I tried to use refresh() method instead but it works incorrectly - sometimes after the call first row
         * is truncated.
         */
        getItems().removeAll(getItems());
        setItems(FXCollections.observableArrayList(data.getVideosInfo()));
    }

    private void clearCache() {
        cellCache.clear();
    }

    private void updateSortToggles(TableSortBy sortBy, TableSortDirection sortDirection) {
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
        selectableCount = 0;
        selectedAvailableCount = 0;
        selectedUnavailableCount = 0;

        /*
         * It's very important that this line goes after the modification of the previous counters
         * (selectedAvailableCount and selectedUnavailableCount) because this property will have subscribers and they
         * need updated counter values there.
         */
        selectedCount.setValue(0);

        selectAllCheckBox.setSelected(false);
        this.mode = null;
        setItems(FXCollections.emptyObservableList());
        System.gc();
    }

    public void setSelected(boolean selected, TableVideoInfo fileInfo) {
        if (fileInfo.isSelected() == selected) {
            return;
        }

        fileInfo.setSelected(selected);

        handleFileSelectionChange(selected, fileInfo);
    }

    public void clearActionResult(TableVideoInfo fileInfo) {
        setActionResult(ActionResult.NO_RESULT, fileInfo);
    }

    public void setActionResult(ActionResult actionResult, TableVideoInfo fileInfo) {
        fileInfo.setActionResult(actionResult);
    }

    public void removeFileWithSubtitles(TableSubtitleOption option, TableVideoInfo fileInfo) {
        if (!option.isRemovable()) {
            log.error("option " + option.getId() + " is not removable");
            throw new IllegalStateException();
        }

        option.setId(null);
        option.setTitle(null);
        option.setSize(UNKNOWN_SIZE);
        option.setNotValidReason(null);
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

    public void subtitlesLoadedSuccessfully(int size, TableSubtitleOption subtitleOption, TableVideoInfo fileInfo) {
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

    public void subtitleOptionPreviewClosed(String notValidReason, TableSubtitleOption subtitleOption) {
        subtitleOption.setNotValidReason(notValidReason);
    }

    public void failedToAddFileWithSubtitles(FileWithSubtitlesUnavailabilityReason reason, TableVideoInfo fileInfo) {
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
            TableVideoInfo fileInfo
    ) {
        TableSubtitleOption subtitleOption = getFirstEmptySubtitleOption(fileInfo);

        subtitleOption.setId(id);
        subtitleOption.setTitle(title);
        subtitleOption.setSize(size);
        subtitleOption.setNotValidReason(incorrectFormat ? "The subtitles have an incorrect format" : null);

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

    private TableSubtitleOption getFirstEmptySubtitleOption(TableVideoInfo fileInfo) {
        for (TableSubtitleOption option : fileInfo.getSubtitleOptions()) {
            if (!option.isRemovable()) {
                continue;
            }

            if (StringUtils.isBlank(option.getTitle())) {
                return option;
            }
        }

        log.error("no empty options, most likely a bug");
        throw new IllegalStateException();
    }

    public TableSortBy getSortBy() {
        Toggle toggle = sortByGroup.getSelectedToggle();
        if (toggle == null) {
            log.error("this method shouldn't be called until the table is initialized, most likely a bug");
            throw new IllegalStateException();
        }

        return (TableSortBy) toggle.getUserData();
    }

    public TableSortDirection getSortDirection() {
        Toggle toggle = sortDirectionGroup.getSelectedToggle();
        if (toggle == null) {
            log.error("this method shouldn't be called until the table is initialized, most likely a bug");
            throw new IllegalStateException();
        }

        return (TableSortDirection) toggle.getUserData();
    }

    public enum Mode {
        SEPARATE_FILES,
        DIRECTORY
    }

    @FunctionalInterface
    public interface SelectAllHandler {
        void handle(boolean allSelected);
    }

    @FunctionalInterface
    public interface SortChangeHandler {
        void handle(TableSortBy sortBy, TableSortDirection sortDirection);
    }

    @FunctionalInterface
    public interface RemoveSubtitleOptionHandler {
        void remove(TableSubtitleOption subtitleOption, TableVideoInfo fileInfo);
    }

    @FunctionalInterface
    public interface SingleSubtitleLoader {
        void loadSubtitles(TableSubtitleOption subtitleOption, TableVideoInfo fileInfo);
    }

    @FunctionalInterface
    public interface SubtitleOptionPreviewHandler {
        void showPreview(TableSubtitleOption subtitleOption, TableVideoInfo fileInfo);
    }

    @FunctionalInterface
    public interface AddFileWithSubtitlesHandler {
        void addFile(TableVideoInfo fileInfo);
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
        void loadSubtitles(TableVideoInfo fileInfo);
    }

    @FunctionalInterface
    public interface MergedSubtitlePreviewHandler {
        void showPreview(TableVideoInfo fileInfo);
    }

    @AllArgsConstructor
    private class TableWithFilesCell<T> extends TableCell<TableVideoInfo, T> {
        private CellType cellType;

        private CellPaneGenerator cellPaneGenerator;

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            TableVideoInfo fileInfo = getTableRow().getItem();

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
                Pane newPane = cellPaneGenerator.getPane(fileInfo);

                if (!StringUtils.isBlank(fileInfo.getNotValidReason())) {
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

        private void setOrRemoveErrorClass(TableVideoInfo fileInfo, Pane pane) {
            if (fileInfo.getActionResult() != null && !StringUtils.isBlank(fileInfo.getActionResult().getError())) {
                pane.getStyleClass().add(PANE_ERROR_CLASS);
            } else {
                pane.getStyleClass().remove(PANE_ERROR_CLASS);
            }
        }
    }

    @FunctionalInterface
    interface CellPaneGenerator {
        Pane getPane(TableVideoInfo fileInfo);
    }

    private enum CellType {
        SELECTED,
        FILE_DESCRIPTION,
        SUBTITLES
    }
}
