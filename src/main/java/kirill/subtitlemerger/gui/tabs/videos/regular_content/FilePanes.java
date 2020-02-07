package kirill.subtitlemerger.gui.tabs.videos.regular_content;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiUtils;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiExternalSubtitleFile;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStream;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@Getter
public class FilePanes {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");

    private GuiFileInfo fileInfo;

    private LongProperty selected;

    private BooleanProperty allSelected;

    private IntegerProperty allAvailableCount;

    private AllFileSubtitleSizesLoader allFileSubtitleSizesLoader;

    private SingleFileSubtitleSizeLoader singleFileSubtitleSizeLoader;

    private AddExternalSubtitleFileHandler addExternalSubtitleFileHandler;

    private RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler;

    private Pane selectPane;

    private Pane fileDescriptionPane;

    private Pane subtitlePane;

    public FilePanes(
            GuiFileInfo fileInfo,
            LongProperty selected,
            BooleanProperty allSelected,
            IntegerProperty allAvailableCount,
            AllFileSubtitleSizesLoader allFileSubtitleSizesLoader,
            SingleFileSubtitleSizeLoader singleFileSubtitleSizeLoader,
            AddExternalSubtitleFileHandler addExternalSubtitleFileHandler,
            RemoveExternalSubtitleFileHandler removeExternalSubtitleFileHandler
    ) {
        this.fileInfo = fileInfo;
        this.selected = selected;
        this.allSelected = allSelected;
        this.allAvailableCount = allAvailableCount;
        this.allFileSubtitleSizesLoader = allFileSubtitleSizesLoader;
        this.singleFileSubtitleSizeLoader = singleFileSubtitleSizeLoader;
        this.addExternalSubtitleFileHandler = addExternalSubtitleFileHandler;
        this.removeExternalSubtitleFileHandler = removeExternalSubtitleFileHandler;
        this.selectPane = generateSelectedCellPane();
        this.fileDescriptionPane = generateFileDescriptionCellPane(fileInfo);
        this.subtitlePane = generateSubtitlesCellPane(fileInfo);
    }

    private Pane generateSelectedCellPane() {
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
            allSelected.set(selected.getValue() == allAvailableCount.get());
        });

        result.getChildren().add(checkBox);

        return result;
    }

    private Pane generateFileDescriptionCellPane(GuiFileInfo fileInfo) {
        VBox result = new VBox();

        result.setPadding(new Insets(3, 5, 3, 3));
        result.setSpacing(10);

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
            result.setAlignment(Pos.CENTER_LEFT);

            Label reason = new Label(StringUtils.capitalize(fileInfo.getUnavailabilityReason()));
            result.getChildren().add(reason);

            return result;
        }

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

        Button button = new Button("Add subtitle file");
        button.getStyleClass().add("add-subtitle-file");

        BooleanBinding canAddMoreFiles = Bindings.isEmpty(fileInfo.getExternalSubtitleFiles().get(0).fileNameProperty())
                .or(Bindings.isEmpty(fileInfo.getExternalSubtitleFiles().get(1).fileNameProperty()));

        button.visibleProperty().bind(canAddMoreFiles);
        button.managedProperty().bind(canAddMoreFiles);
        Image image = new Image("/gui/icons/add.png");
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(8);
        imageView.setFitHeight(imageView.getFitWidth());
        button.setGraphic(imageView);
        button.setOnAction((event -> addExternalSubtitleFileHandler.buttonClicked(fileInfo)));

        result.addRow(
                0,
                button
        );

        GridPane.setColumnSpan(button, 3);
        GridPane.setMargin(button, new Insets(0, 0, 5, 0));

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
            image = new Image("/gui/icons/remove.png");
            imageView = new ImageView(image);
            imageView.setFitWidth(8);
            imageView.setFitHeight(imageView.getFitWidth());
            removeButton.setGraphic(imageView);

            //todo remove awful
            int index = externalSubtitleFileIndex;

            removeButton.setOnAction(event -> removeExternalSubtitleFileHandler.buttonClicked(index, fileInfo));

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

            int rowIndex = 1 + externalSubtitleFileIndex;
            result.add(fileNameAndRemove, 0, rowIndex);
            result.add(sizeLabel, 1, rowIndex);
            result.add(radios, 2, rowIndex);

            int bottomMargin = 2;

            GridPane.setMargin(fileNameAndRemove, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(sizeLabel, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(radios, new Insets(0, 0, bottomMargin, 0));

            BooleanBinding externalFileUsed = Bindings.isNotEmpty(externalSubtitleFile.fileNameProperty());

            fileNameAndRemove.visibleProperty().bind(externalFileUsed);
            fileNameAndRemove.managedProperty().bind(externalFileUsed);
            sizeLabel.visibleProperty().bind(externalFileUsed);
            sizeLabel.managedProperty().bind(externalFileUsed);
            radios.visibleProperty().bind(externalFileUsed);
            radios.managedProperty().bind(externalFileUsed);

            externalSubtitleFileIndex++;
        }

        BooleanBinding showExtra = Bindings.not(fileInfo.someSubtitlesHiddenProperty());

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

            errorImageLabel.setTooltip(GuiUtils.generateTooltip(stream.getFailedToLoadReason()));

            if (StringUtils.isBlank(stream.getFailedToLoadReason())) {
                errorImageLabel.setVisible(false);
                errorImageLabel.setManaged(false);
            }

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

            Hyperlink getSizeLink = new Hyperlink("get size");
            getSizeLink.setOnAction(event -> singleFileSubtitleSizeLoader.load(fileInfo, stream.getFfmpegIndex()));
            getSizeLink.visibleProperty().bind(stream.sizeProperty().isEqualTo(GuiSubtitleStream.UNKNOWN_SIZE));
            getSizeLink.managedProperty().bind(stream.sizeProperty().isEqualTo(GuiSubtitleStream.UNKNOWN_SIZE));

            sizePane.getChildren().addAll(sizeLabel, getSizeLink, errorImageLabel);

            HBox radios = new HBox();
            radios.setSpacing(5);
            radios.setAlignment(Pos.CENTER);

            RadioButton upper = new RadioButton("upper");
            upper.selectedProperty().bindBidirectional(stream.selectedAsUpperProperty());

            RadioButton lower = new RadioButton("lower");
            lower.selectedProperty().bindBidirectional(stream.selectedAsLowerProperty());

            radios.getChildren().addAll(upper, lower);

            result.add(titlePane, 0, 1 + fileInfo.getExternalSubtitleFiles().size() + streamIndex);
            result.add(sizePane, 1, 1 + fileInfo.getExternalSubtitleFiles().size() + streamIndex);
            result.add(radios, 2, 1 + fileInfo.getExternalSubtitleFiles().size() + streamIndex);

            int bottomMargin = 2;

            if (stream.isExtra()) {
                titlePane.visibleProperty().bind(showExtra);
                titlePane.managedProperty().bind(showExtra);
                sizeLabel.visibleProperty().bind(showExtra);
                sizeLabel.managedProperty().bind(showExtra);
                sizePane.visibleProperty().bind(showExtra);
                sizePane.managedProperty().bind(showExtra);
                radios.visibleProperty().bind(showExtra);
                radios.managedProperty().bind(showExtra);
            }

            GridPane.setMargin(titlePane, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(sizePane, new Insets(0, 0, bottomMargin, 0));
            GridPane.setMargin(radios, new Insets(0, 0, bottomMargin, 0));

            streamIndex++;
        }

        Pane hiddenPane = generateHiddenPane(fileInfo);

        HBox getAllSizesPane = new HBox();
        getAllSizesPane.setAlignment(Pos.CENTER);

        Hyperlink getAllSizes = new Hyperlink("get all sizes");
        getAllSizes.setOnAction(event -> allFileSubtitleSizesLoader.load(fileInfo));

        getAllSizes.visibleProperty().bind(fileInfo.haveSubtitleSizesToLoadProperty());
        getAllSizes.managedProperty().bind(fileInfo.haveSubtitleSizesToLoadProperty());

        getAllSizesPane.getChildren().add(getAllSizes);

        result.add(hiddenPane, 0, 1 + fileInfo.getExternalSubtitleFiles().size() + fileInfo.getSubtitleStreams().size());
        result.add(getAllSizesPane, 1, 1 + fileInfo.getExternalSubtitleFiles().size() + fileInfo.getSubtitleStreams().size());
        result.add(new Region(), 2, 1 + fileInfo.getExternalSubtitleFiles().size() + fileInfo.getSubtitleStreams().size());

        GridPane.setMargin(hiddenPane, new Insets(0, 0, 0, 0));
        GridPane.setMargin(getAllSizesPane, new Insets(0, 0, 0, 0));
        GridPane.setMargin(result.getChildren().get(result.getChildren().size() - 1), new Insets(0, 0, 0, 0));

        StackPane statusPane = new StackPane();

        Label errorMessageLabel = new Label();
        errorMessageLabel.textProperty().bind(fileInfo.errorMessageProperty());
        errorMessageLabel.getStyleClass().add("label-error");
        BooleanBinding showErrorMessage = Bindings.isNotEmpty(fileInfo.errorMessageProperty());
        errorMessageLabel.visibleProperty().bind(showErrorMessage);
        errorMessageLabel.managedProperty().bind(showErrorMessage);

        Label successMessageLabel = new Label();
        successMessageLabel.textProperty().bind(fileInfo.successMessageProperty());
        successMessageLabel.getStyleClass().add("label-success");
        BooleanBinding showSuccessMessage = Bindings.isNotEmpty(fileInfo.successMessageProperty());
        successMessageLabel.visibleProperty().bind(showSuccessMessage);
        successMessageLabel.managedProperty().bind(showSuccessMessage);

        statusPane.getChildren().addAll(errorMessageLabel, successMessageLabel);

        result.addRow(
                2 + fileInfo.getExternalSubtitleFiles().size() + fileInfo.getSubtitleStreams().size(),
                statusPane
        );

        GridPane.setColumnSpan(statusPane, 3);
        GridPane.setMargin(statusPane, new Insets(10, 0, 0, 0));

        return result;
    }

    private Pane generateHiddenPane(GuiFileInfo fileInfo) {
        HBox result = new HBox(); //todo wrapper isn't necessary

        result.setAlignment(Pos.CENTER);

        StringBinding showHiddenBinding = Bindings.createStringBinding(
                () -> "show " + fileInfo.getSubtitleToHideCount() + " hidden ", fileInfo.subtitleToHideCountProperty()
        );

        Hyperlink showAllLink = new Hyperlink();
        showAllLink.visibleProperty().bind(fileInfo.subtitleToHideCountProperty().greaterThan(0));
        showAllLink.managedProperty().bind(fileInfo.subtitleToHideCountProperty().greaterThan(0));
        showAllLink.textProperty().bind(
                Bindings.when(fileInfo.someSubtitlesHiddenProperty())
                        .then(showHiddenBinding)
                        .otherwise("hide extra subtitles")
        );
        showAllLink.setOnAction(event -> fileInfo.setSomeSubtitlesHidden(!fileInfo.isSomeSubtitlesHidden()));

        result.getChildren().addAll(showAllLink);

        return result;
    }

    @FunctionalInterface
    public interface AllFileSubtitleSizesLoader {
        void load(GuiFileInfo guiFileInfo);
    }

    @FunctionalInterface
    public interface SingleFileSubtitleSizeLoader {
        void load(GuiFileInfo guiFileInfo, int ffmpegIndex);
    }

    @FunctionalInterface
    public interface AddExternalSubtitleFileHandler {
        void buttonClicked(GuiFileInfo guiFileInfo);
    }

    @FunctionalInterface
    public interface RemoveExternalSubtitleFileHandler {
        void buttonClicked(int index, GuiFileInfo guiFileInfo);
    }
}
