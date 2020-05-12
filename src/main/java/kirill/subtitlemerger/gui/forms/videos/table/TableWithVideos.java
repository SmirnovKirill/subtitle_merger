package kirill.subtitlemerger.gui.forms.videos.table;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Callback;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.common_controls.ActionResultPane;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption.UNKNOWN_SIZE;

@CommonsLog
public class TableWithVideos extends TableView<TableVideo> {
    private static final int CELL_PADDING = 4;

    private static final int TITLE_AND_REMOVE_PANE_MIN_WIDTH = 190;

    /*
     * On Windows the default font is more compact than the Linux's one. So it's better to set the width smaller because
     * the gap between an unknown size label and a load link looks pretty big anyway but on Windows it looks even
     * bigger.
     */
    private static final int SIZE_AND_PREVIEW_PANE_WIDTH = SystemUtils.IS_OS_LINUX ? 90 : 82;

    private static final int OPTION_SELECTION_PANE_WIDTH = 110;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");

    private final Map<String, Map<CellType, Pane>> cellCache;

    private ObjectProperty<SelectAllHandler> selectAllHandler;

    private CheckBox selectAllCheckBox;

    private ObjectProperty<AddSubtitleFileHandler> addSubtitleFileHandler;

    private ObjectProperty<RemoveSubtitleFileHandler> removeSubtitleFileHandler;

    private ObjectProperty<SingleSubtitleLoader> singleSubtitleLoader;

    private ObjectProperty<AllVideoSubtitleLoader> allVideoSubtitleLoader;

    private ObjectProperty<SubtitleOptionPreviewHandler> subtitleOptionPreviewHandler;

    private ObjectProperty<MergedSubtitlePreviewHandler> mergedSubtitlePreviewHandler;

    private ToggleGroup sortByGroup;

    private ToggleGroup sortDirectionGroup;

    private ObjectProperty<ChangeSortHandler> changeSortHandler;

    @Getter
    private TableMode mode;

    @Getter
    private int selectableCount;

    @Getter
    private int selectedAvailableCount;

    @Getter
    private int selectedUnavailableCount;

    private ReadOnlyIntegerWrapper selectedCount;

    public TableWithVideos() {
        cellCache = new HashMap<>();

        selectAllHandler = new SimpleObjectProperty<>();
        selectAllCheckBox = getSelectAllCheckBox(selectAllHandler);

        addSubtitleFileHandler = new SimpleObjectProperty<>();
        removeSubtitleFileHandler = new SimpleObjectProperty<>();
        singleSubtitleLoader = new SimpleObjectProperty<>();
        allVideoSubtitleLoader = new SimpleObjectProperty<>();
        subtitleOptionPreviewHandler = new SimpleObjectProperty<>();
        mergedSubtitlePreviewHandler = new SimpleObjectProperty<>();

        sortByGroup = new ToggleGroup();
        sortDirectionGroup = new ToggleGroup();
        changeSortHandler = new SimpleObjectProperty<>();

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        getColumns().add(getSelectionColumn(selectAllCheckBox));
        getColumns().add(getVideoDescriptionColumn());
        getColumns().add(getSubtitleColumn());

        setSelectionModel(null);
        setPlaceholder(new Label("There are no videos to display"));
        setContextMenu(getContextMenu(sortByGroup, sortDirectionGroup, changeSortHandler));

        selectedCount = new ReadOnlyIntegerWrapper();
    }

    private CheckBox getSelectAllCheckBox(ObjectProperty<SelectAllHandler> selectAllHandler) {
        CheckBox result = new CheckBox();

        result.setOnAction(event -> selectAllHandler.get().handle(result.isSelected()));

        return result;
    }

    private TableColumn<TableVideo, ?> getSelectionColumn(CheckBox selectAllCheckBox) {
        TableColumn<TableVideo, ?> result = new TableColumn<>();

        result.setResizable(false);
        result.setMinWidth(26);
        result.setMaxWidth(26);
        result.setReorderable(false);
        result.setSortable(false);

        result.setGraphic(selectAllCheckBox);
        result.setCellFactory(getCellFactory(CellType.SELECTION, this::getSelectionPane, cellCache));

        return result;
    }

    private static <T> Callback<TableColumn<TableVideo, T>, TableCell<TableVideo, T>> getCellFactory(
            CellType cellType,
            CellPaneGenerator cellPaneGenerator,
            Map<String, Map<CellType, Pane>> cellCache
    ) {
        return tableColumn -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                TableVideo video = getTableRow().getItem();

                if (empty || video == null) {
                    setGraphic(null);
                    return;
                }

                Pane pane;
                Map<CellType, Pane> rowCache = cellCache.get(video.getId());
                if (rowCache != null && rowCache.containsKey(cellType)) {
                    pane = rowCache.get(cellType);
                } else {
                    pane = cellPaneGenerator.getPane(video);

                    if (!StringUtils.isBlank(video.getNotValidReason())) {
                        if (cellType != CellType.SELECTION) {
                            pane.getStyleClass().add("pane-unavailable");
                        }
                    } else {
                        video.actionResultProperty().addListener(observable -> setPaneDynamicClass(video, pane));
                        setPaneDynamicClass(video, pane);
                    }

                    cellCache.putIfAbsent(video.getId(), new HashMap<>());
                    cellCache.get(video.getId()).put(cellType, pane);
                }

                setGraphic(pane);
            }

            private void setPaneDynamicClass(TableVideo video, Pane pane) {
                String warnClass = "pane-warn";
                String errorClass = "pane-error";

                pane.getStyleClass().remove(warnClass);
                pane.getStyleClass().remove(errorClass);

                ActionResult actionResult = video.getActionResult();
                if (actionResult != null) {
                    if (actionResult.haveWarnings() && !actionResult.haveErrors()) {
                        pane.getStyleClass().add(warnClass);
                    } else if (actionResult.haveErrors()) {
                        pane.getStyleClass().add(errorClass);
                    }
                }
            }
        };
    }

    private Pane getSelectionPane(TableVideo video) {
        HBox result = new HBox();

        result.setAlignment(Pos.TOP_CENTER);
        result.setPadding(new Insets(CELL_PADDING, 0, CELL_PADDING, 0));

        /*
         * We should stop here if in the directory mode, the checkbox isn't needed because it will not be allowed to
         * select unavailable videos. On the contrary, in the separate videos mode it's possible to select an
         * unavailable video to remove it. Because of the ability to remove the video the behaviour is different.
         */
        if (!StringUtils.isBlank(video.getNotValidReason()) && mode == TableMode.WHOLE_DIRECTORY) {
            return result;
        }

        CheckBox selectionCheckBox = new CheckBox();
        selectionCheckBox.selectedProperty().bindBidirectional(video.selectedProperty());

        result.getChildren().add(selectionCheckBox);

        return result;
    }

    private TableColumn<TableVideo, ?> getVideoDescriptionColumn() {
        TableColumn<TableVideo, ?> result = new TableColumn<>("video");

        result.setMinWidth(200);
        result.setReorderable(false);
        result.setSortable(false);

        result.setCellFactory(getCellFactory(CellType.VIDEO_DESCRIPTION, this::getVideoDescriptionPane, cellCache));

        return result;
    }

    private Pane getVideoDescriptionPane(TableVideo video) {
        GridPane result = new GridPane();

        result.setHgap(30);
        result.setGridLinesVisible(GuiConstants.GRID_LINES_VISIBLE);
        result.setPadding(new Insets(CELL_PADDING, CELL_PADDING + 1, CELL_PADDING, CELL_PADDING));

        Label pathLabel = new Label(video.getFilePath());
        pathLabel.getStyleClass().add("path-label");
        GridPane.setColumnSpan(pathLabel, 2);
        GridPane.setMargin(pathLabel, new Insets(0, 0, 10, 0));

        Label sizeTitleLabel = new Label("size");
        GridPane.setMargin(sizeTitleLabel, new Insets(0, 0, 3, 0));

        Label sizeLabel = new Label();
        GridPane.setMargin(sizeLabel, new Insets(0, 0, 3, 0));
        sizeLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> Utils.getSizeTextual(video.getSize(), false),
                        video.sizeProperty()
                )
        );

        Label lastModifiedTitleLabel = new Label("last modified");

        Label lastModifiedLabel = new Label();
        lastModifiedLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> DATE_TIME_FORMATTER.print(video.getLastModified()),
                        video.lastModifiedProperty()
                )
        );

        result.addRow(0, pathLabel);
        result.addRow(1, sizeTitleLabel, sizeLabel);
        result.addRow(2, lastModifiedTitleLabel, lastModifiedLabel);

        return result;
    }

    private TableColumn<TableVideo, ?> getSubtitleColumn() {
        TableColumn<TableVideo, ?> result = new TableColumn<>("subtitles");

        result.setMinWidth(
                CELL_PADDING + 1 + TITLE_AND_REMOVE_PANE_MIN_WIDTH + 15 + SIZE_AND_PREVIEW_PANE_WIDTH + 15
                        + OPTION_SELECTION_PANE_WIDTH + CELL_PADDING
        );
        result.setReorderable(false);
        result.setSortable(false);

        result.setCellFactory(getCellFactory(CellType.SUBTITLES, this::getSubtitlesPane, cellCache));

        return result;
    }

    private Pane getSubtitlesPane(TableVideo video) {
        if (!StringUtils.isBlank(video.getNotValidReason())) {
            return getNotValidVideoPane(video);
        }

        VBox result = new VBox();

        result.setPadding(new Insets(CELL_PADDING, CELL_PADDING, CELL_PADDING, CELL_PADDING + 1));

        result.getChildren().addAll(
                getOptionsPane(video),
                GuiUtils.getFixedHeightSpacer(5),
                getRowWithActionsPane(video),
                GuiUtils.getFixedHeightSpacer(10),
                getActionResultPane(video)
        );

        return result;
    }

    private Pane getNotValidVideoPane(TableVideo video) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setPadding(new Insets(CELL_PADDING, CELL_PADDING, CELL_PADDING, CELL_PADDING + 1));

        result.getChildren().add(new Label(video.getNotValidReason()));

        return result;
    }

    private Pane getOptionsPane(TableVideo video) {
        VBox result = new VBox();

        result.setSpacing(2);

        video.getOptions().addListener((InvalidationListener) observable -> {
            result.getChildren().clear();
            for (TableSubtitleOption option : video.getOptions()) {
                result.getChildren().add(getOptionPane(option));
            }
        });
        for (TableSubtitleOption option : video.getOptions()) {
            result.getChildren().add(getOptionPane(option));
        }

        return result;
    }

    private Pane getOptionPane(TableSubtitleOption option) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        if (option.isHideable()) {
            GuiUtils.bindVisibleAndManaged(result, Bindings.not(option.getVideo().someOptionsHiddenProperty()));
        }
        result.setSpacing(15);

        result.getChildren().addAll(
                getTitleAndRemovePane(option),
                getSizeAndPreviewPane(option),
                getOptionSelectionPane(option)
        );

        return result;
    }

    private Pane getTitleAndRemovePane(TableSubtitleOption option) {
        HBox result = new HBox();

        result.setMinWidth(TITLE_AND_REMOVE_PANE_MIN_WIDTH);
        HBox.setHgrow(result, Priority.ALWAYS);
        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(10);

        Label optionTitleLabel = new Label(option.getTitle());
        optionTitleLabel.setMaxWidth(Double.MAX_VALUE);
        if (option.isMerged()) {
            optionTitleLabel.getStyleClass().add("merged-title");
        }
        result.getChildren().add(optionTitleLabel);

        if (option.getType() == TableSubtitleOptionType.EXTERNAL) {
            Button removeButton = GuiUtils.getImageButton(
                    null,
                    "/gui/icons/remove.png",
                    8,
                    8
            );
            removeButton.setOnAction(event -> removeSubtitleFileHandler.get().remove(option));
            result.getChildren().add(removeButton);
        }

        return result;
    }

    private Pane getSizeAndPreviewPane(TableSubtitleOption option) {
        StackPane result = new StackPane();

        GuiUtils.setFixedWidth(result, SIZE_AND_PREVIEW_PANE_WIDTH);

        result.getChildren().addAll(
                getUnknownSizePane(option),
                getKnownSizeAndPreviewPane(option)
        );

        return result;
    }

    private Pane getUnknownSizePane(TableSubtitleOption option) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.visibleProperty().bind(option.sizeProperty().isEqualTo(UNKNOWN_SIZE));

        Label sizeLabel = new Label("Size: ? KB");

        Label failedToLoadLabel = new Label();
        failedToLoadLabel.setGraphic(GuiUtils.getImageView("/gui/icons/error.png", 12, 12));
        failedToLoadLabel.setTooltip(GuiUtils.getTooltip(option.failedToLoadReasonProperty()));
        GuiUtils.bindVisibleAndManaged(failedToLoadLabel, option.failedToLoadReasonProperty().isNotEmpty());

        result.getChildren().addAll(sizeLabel, GuiUtils.getFixedWidthSpacer(5), failedToLoadLabel);

        if (StringUtils.isBlank(option.getNotValidReason())) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Hyperlink loadSubtitleLink = new Hyperlink("load");
            loadSubtitleLink.setOnAction(event -> singleSubtitleLoader.get().load(option));
            loadSubtitleLink.visibleProperty().bind(option.sizeProperty().isEqualTo(UNKNOWN_SIZE));

            result.getChildren().addAll(spacer, loadSubtitleLink);
        }

        return result;
    }

    private Pane getKnownSizeAndPreviewPane(TableSubtitleOption option) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.visibleProperty().bind(option.sizeProperty().isNotEqualTo(UNKNOWN_SIZE));

        Label sizeLabel = new Label();
        sizeLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> "Size: " + Utils.getSizeTextual(option.getSize(), true),
                        option.sizeProperty()
                )
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button previewButton = GuiUtils.getImageButton("", "/gui/icons/eye.png", 15, 10);
        previewButton.setOnAction(event -> subtitleOptionPreviewHandler.get().showPreview(option));

        result.getChildren().addAll(sizeLabel, spacer, previewButton);

        return result;
    }

    private Pane getOptionSelectionPane(TableSubtitleOption option) {
        HBox result = new HBox();

        GuiUtils.setFixedWidth(result, OPTION_SELECTION_PANE_WIDTH);
        result.setAlignment(Pos.CENTER);
        result.setSpacing(5);

        if (!option.isMerged()) {
            option.notValidReasonProperty().addListener(observable -> setOptionSelectionTooltip(option, result));
            setOptionSelectionTooltip(option, result);

            RadioButton upperRadio = new RadioButton("upper");
            upperRadio.disableProperty().bind(option.notValidReasonProperty().isNotEmpty());
            upperRadio.selectedProperty().bindBidirectional(option.selectedAsUpperProperty());

            RadioButton lowerRadio = new RadioButton("lower");
            lowerRadio.disableProperty().bind(option.notValidReasonProperty().isNotEmpty());
            lowerRadio.selectedProperty().bindBidirectional(option.selectedAsLowerProperty());

            result.getChildren().addAll(upperRadio, lowerRadio);
        } else {
            /*
             * I had to make this fake button because otherwise the pane's height for merged options is smaller than for
             * the regular ones and that's because they have radio buttons. So instead of setting the minimum height of
             * the pane to some constant value I decided that it's more reliable just to add an invisible but managed
             * button.
             */
            RadioButton fakeRadio = new RadioButton("fake");
            fakeRadio.setVisible(false);
            result.getChildren().add(fakeRadio);
        }

        return result;
    }

    private void setOptionSelectionTooltip(TableSubtitleOption option, Pane optionSelectionPane) {
        if (StringUtils.isBlank(option.getNotValidReason())) {
            Tooltip.install(optionSelectionPane, null);
        } else {
            Tooltip tooltip = GuiUtils.getTooltip(option.getNotValidReason());
            Tooltip.install(optionSelectionPane, tooltip);
        }
    }

    private Pane getRowWithActionsPane(TableVideo video) {
        HBox result = new HBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(15);

        result.getChildren().addAll(
                getShowHideAddPane(video),
                getLoadAllSubtitlesPane(video),
                getMergedPreviewPane(video)
        );

        return result;
    }

    private Pane getShowHideAddPane(TableVideo video) {
        HBox result = new HBox();

        result.setMinWidth(TITLE_AND_REMOVE_PANE_MIN_WIDTH);
        HBox.setHgrow(result, Priority.ALWAYS);
        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(25);

        if (video.getHideableOptionCount() != 0) {
            Hyperlink showHideLink = new Hyperlink();
            showHideLink.setOnAction(event -> video.setSomeOptionsHidden(!video.isSomeOptionsHidden()));
            showHideLink.textProperty().bind(
                    Bindings.when(video.someOptionsHiddenProperty())
                            .then("show " + video.getHideableOptionCount() + " hidden")
                            .otherwise("hide extra")
            );
            result.getChildren().add(showHideLink);
        }

        result.getChildren().add(getAddSubtitleFileButton(video));

        return result;
    }

    private Button getAddSubtitleFileButton(TableVideo video) {
        Button result = GuiUtils.getImageButton(
                "Add subtitles",
                "/gui/icons/add.png",
                9,
                9
        );

        result.setOnAction(event -> addSubtitleFileHandler.get().add(video));
        result.visibleProperty().bind(video.externalOptionCountProperty().lessThan(2));

        return result;
    }

    private Pane getLoadAllSubtitlesPane(TableVideo video) {
        HBox result = new HBox();

        GuiUtils.setFixedWidth(result, SIZE_AND_PREVIEW_PANE_WIDTH);
        result.setAlignment(Pos.CENTER);

        Hyperlink loadAllLink = new Hyperlink("load all subtitles");
        loadAllLink.setOnAction(event -> allVideoSubtitleLoader.get().load(video));
        loadAllLink.visibleProperty().bind(video.notLoadedOptionCountProperty().greaterThan(1));

        result.getChildren().add(loadAllLink);

        return result;
    }

    private Pane getMergedPreviewPane(TableVideo video) {
        HBox result = new HBox();

        GuiUtils.setFixedWidth(result, OPTION_SELECTION_PANE_WIDTH);
        result.setAlignment(Pos.CENTER);
        result.visibleProperty().bind(Bindings.size(video.getOptions()).greaterThanOrEqualTo(2));

        Button previewButton = GuiUtils.getImageButton(
                "result preview",
                "/gui/icons/eye.png",
                15,
                10
        );
        previewButton.setOnAction(event -> mergedSubtitlePreviewHandler.get().showPreview(video));

        InvalidationListener listener = observable -> setMergedPreviewDisabledAndTooltip(previewButton, result, video);
        video.upperOptionProperty().addListener(listener);
        video.lowerOptionProperty().addListener(listener);
        setMergedPreviewDisabledAndTooltip(previewButton, result, video);

        result.getChildren().add(previewButton);

        return result;
    }

    private void setMergedPreviewDisabledAndTooltip(Button previewButton, Pane previewPane, TableVideo video) {
        TableSubtitleOption upperOption = video.getUpperOption();
        TableSubtitleOption lowerOption = video.getLowerOption();

        if (upperOption == null || lowerOption == null) {
            previewButton.setDisable(true);
            Tooltip tooltip = GuiUtils.getTooltip("Please select subtitles to merge first");
            Tooltip.install(previewPane, tooltip);
        } else {
            previewButton.setDisable(false);
            Tooltip.install(previewPane, null);
        }
    }

    private ActionResultPane getActionResultPane(TableVideo video) {
        ActionResultPane result = new ActionResultPane();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setWrapText(true);
        result.actionResultProperty().bind(video.actionResultProperty());

        return result;
    }

    private ContextMenu getContextMenu(
            ToggleGroup sortByGroup,
            ToggleGroup sortDirectionGroup,
            ObjectProperty<ChangeSortHandler> changeSortHandler
    ) {
        Menu menu = new Menu("_Sort videos");

        menu.setOnAction(
                event -> changeSortHandler.get().handle(
                        (TableSortBy) sortByGroup.getSelectedToggle().getUserData(),
                        (TableSortDirection) sortDirectionGroup.getSelectedToggle().getUserData()
                )
        );

        RadioMenuItem byNameRadio = new RadioMenuItem("By _Name");
        byNameRadio.setToggleGroup(sortByGroup);
        byNameRadio.setUserData(TableSortBy.NAME);

        RadioMenuItem byModificationTimeRadio = new RadioMenuItem("By _Modification Time");
        byModificationTimeRadio.setToggleGroup(sortByGroup);
        byModificationTimeRadio.setUserData(TableSortBy.MODIFICATION_TIME);

        RadioMenuItem bySizeRadio = new RadioMenuItem("By _Size");
        bySizeRadio.setToggleGroup(sortByGroup);
        bySizeRadio.setUserData(TableSortBy.SIZE);

        RadioMenuItem ascendingRadio = new RadioMenuItem("_Ascending");
        ascendingRadio.setToggleGroup(sortDirectionGroup);
        ascendingRadio.setUserData(TableSortDirection.ASCENDING);

        RadioMenuItem descendingRadio = new RadioMenuItem("_Descending");
        descendingRadio.setToggleGroup(sortDirectionGroup);
        descendingRadio.setUserData(TableSortDirection.DESCENDING);

        menu.getItems().addAll(
                byNameRadio,
                byModificationTimeRadio,
                bySizeRadio,
                new SeparatorMenuItem(),
                ascendingRadio,
                descendingRadio
        );

        return new ContextMenu(menu);
    }

    public void setData(TableData data, boolean clearCache) {
        if (clearCache) {
            cellCache.clear();
        }

        setItems(FXCollections.observableArrayList(data.getVideos()));
        mode = data.getMode();

        selectableCount = data.getSelectableCount();
        selectedAvailableCount = data.getSelectedAvailableCount();
        selectedUnavailableCount = data.getSelectedUnavailableCount();
        /*
         * It's very important that this line goes after the modification of the previous counters
         * (selectedAvailableCount and selectedUnavailableCount) because this property will have subscribers and they
         * need updated counter values there.
         */
        selectedCount.set(selectedAvailableCount + selectedUnavailableCount);
        selectAllCheckBox.setSelected(getSelectedCount() > 0 && getSelectedCount() == data.getSelectableCount());

        updateSortToggles(data.getSortBy(), data.getSortDirection());
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
        cellCache.clear();

        mode = null;
        setItems(FXCollections.emptyObservableList());

        selectableCount = 0;
        selectedAvailableCount = 0;
        selectedUnavailableCount = 0;
        /*
         * It's very important that this line goes after the modification of the previous counters
         * (selectedAvailableCount and selectedUnavailableCount) because this property will have subscribers and they
         * need updated counter values there.
         */
        selectedCount.set(0);
        selectAllCheckBox.setSelected(false);

        sortByGroup.selectToggle(null);
        sortDirectionGroup.selectToggle(null);

        System.gc();
    }

    void handleVideoSelected(boolean selected, boolean videoAvailable) {
        int addValue = selected ? 1 : -1;

        if (videoAvailable) {
            selectedAvailableCount += addValue;
        } else {
            selectedUnavailableCount += addValue;
        }

        /*
         * It's very important that this line goes after the modification of the previous counters
         * (selectedAvailableCount and selectedUnavailableCount) because this property will have subscribers and they
         * need updated counter values there.
         */
        selectedCount.set(getSelectedCount() + addValue);

        selectAllCheckBox.setSelected(getSelectedCount() > 0 && getSelectedCount() == selectableCount);
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
    public AddSubtitleFileHandler getAddSubtitleFileHandler() {
        return addSubtitleFileHandler.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<AddSubtitleFileHandler> addSubtitleFileHandlerProperty() {
        return addSubtitleFileHandler;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setAddSubtitleFileHandler(AddSubtitleFileHandler addSubtitleFileHandler) {
        this.addSubtitleFileHandler.set(addSubtitleFileHandler);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public RemoveSubtitleFileHandler getRemoveSubtitleFileHandler() {
        return removeSubtitleFileHandler.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<RemoveSubtitleFileHandler> removeSubtitleFileHandlerProperty() {
        return removeSubtitleFileHandler;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setRemoveSubtitleFileHandler(RemoveSubtitleFileHandler removeSubtitleFileHandler) {
        this.removeSubtitleFileHandler.set(removeSubtitleFileHandler);
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
    public AllVideoSubtitleLoader getAllVideoSubtitleLoader() {
        return allVideoSubtitleLoader.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<AllVideoSubtitleLoader> allVideoSubtitleLoaderProperty() {
        return allVideoSubtitleLoader;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setAllVideoSubtitleLoader(AllVideoSubtitleLoader allVideoSubtitleLoader) {
        this.allVideoSubtitleLoader.set(allVideoSubtitleLoader);
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

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ChangeSortHandler getChangeSortHandler() {
        return changeSortHandler.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<ChangeSortHandler> changeSortHandlerProperty() {
        return changeSortHandler;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setChangeSortHandler(ChangeSortHandler changeSortHandler) {
        this.changeSortHandler.set(changeSortHandler);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public int getSelectedCount() {
        return selectedCount.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyIntegerProperty selectedCountProperty() {
        return selectedCount.getReadOnlyProperty();
    }

    @FunctionalInterface
    public interface SelectAllHandler {
        void handle(boolean selectAll);
    }

    @FunctionalInterface
    public interface AddSubtitleFileHandler {
        void add(TableVideo video);
    }

    @FunctionalInterface
    public interface RemoveSubtitleFileHandler {
        void remove(TableSubtitleOption option);
    }

    @FunctionalInterface
    public interface SingleSubtitleLoader {
        void load(TableSubtitleOption option);
    }

    @FunctionalInterface
    public interface AllVideoSubtitleLoader {
        void load(TableVideo video);
    }

    @FunctionalInterface
    public interface SubtitleOptionPreviewHandler {
        void showPreview(TableSubtitleOption option);
    }

    @FunctionalInterface
    public interface MergedSubtitlePreviewHandler {
        void showPreview(TableVideo video);
    }

    @FunctionalInterface
    public interface ChangeSortHandler {
        void handle(TableSortBy sortBy, TableSortDirection sortDirection);
    }

    private enum CellType {
        SELECTION,
        VIDEO_DESCRIPTION,
        SUBTITLES
    }

    @FunctionalInterface
    interface CellPaneGenerator {
        Pane getPane(TableVideo video);
    }
}
