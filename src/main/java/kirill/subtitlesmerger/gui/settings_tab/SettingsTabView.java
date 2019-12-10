package kirill.subtitlesmerger.gui.settings_tab;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import kirill.subtitlesmerger.gui.GuiLauncher;
import kirill.subtitlesmerger.gui.TabView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SettingsTabView implements TabView {
    public static final String TAB_NAME = "Settings";

    private static final LanguageCodeStringConverter LANGUAGE_CODE_STRING_CONVERTER = new LanguageCodeStringConverter();

    private Stage stage;

    private TextField ffprobeField;

    private Button ffprobeSetButton;

    private FileChooser ffprobeFileChooser;

    private TextField ffmpegField;

    private Button ffmpegSetButton;

    private FileChooser ffmpegFileChooser;

    private ComboBox<LanguageAlpha3Code> upperLanguageComboBox;

    private ComboBox<LanguageAlpha3Code> lowerLanguageComboBox;

    private Button swapLanguagesButton;

    private Label resultLabel;

    private Tab tab;

    public SettingsTabView(Stage stage, boolean debug) {
        this.stage = stage;
        this.ffprobeField = generateFfprobeField();
        this.ffprobeSetButton = new Button();
        this.ffprobeFileChooser = new FileChooser();
        this.ffmpegField = generateFfmpegField();
        this.ffmpegSetButton = new Button();
        this.ffmpegFileChooser = new FileChooser();
        this.upperLanguageComboBox = generateLanguageComboBox();
        this.lowerLanguageComboBox = generateLanguageComboBox();
        this.swapLanguagesButton = generateSwapLanguagesButton();
        this.resultLabel = new Label();
        this.tab = generateTab(
                debug,
                ffprobeField,
                ffprobeSetButton,
                ffmpegField,
                ffmpegSetButton,
                upperLanguageComboBox,
                lowerLanguageComboBox,
                swapLanguagesButton,
                resultLabel
        );
    }

    private static TextField generateFfprobeField() {
        TextField result = new TextField();

        result.setEditable(false);

        return result;
    }

    private static TextField generateFfmpegField() {
        TextField result = new TextField();

        result.setEditable(false);

        return result;
    }

    private static ComboBox<LanguageAlpha3Code> generateLanguageComboBox() {
        ComboBox<LanguageAlpha3Code> result = new ComboBox<>();

        result.setConverter(LANGUAGE_CODE_STRING_CONVERTER);
        result.setMaxWidth(Double.MAX_VALUE);

        return result;
    }

    private static Button generateSwapLanguagesButton() {
        Image image = new Image(SettingsTabView.class.getResourceAsStream("/swap.png"));
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(24);
        imageView.setFitWidth(24);

        Button result = new Button("", imageView);

        result.setPadding(new Insets(0));

        return result;
    }

    private static Tab generateTab(
            boolean debug,
            TextField ffprobeField,
            Button ffprobeSetButton,
            TextField ffmpegField,
            Button ffmpegSetButton,
            ComboBox<LanguageAlpha3Code> upperLanguageComboBox,
            ComboBox<LanguageAlpha3Code> lowerLanguageComboBox,
            Button swapLanguagesButton,
            Label resultLabel
    ) {
        Tab result = new Tab(TAB_NAME);

        result.setGraphic(generateTabGraphic());
        result.setContent(
                generateTabContent(
                        debug,
                        ffprobeField,
                        ffprobeSetButton,
                        ffmpegField,
                        ffmpegSetButton,
                        upperLanguageComboBox,
                        lowerLanguageComboBox,
                        swapLanguagesButton,
                        resultLabel
                )
        );

        return result;
    }

    private static ImageView generateTabGraphic() {
        ImageView result = new ImageView(new Image(SettingsTabView.class.getResourceAsStream("/settings.png")));

        result.setFitHeight(16);
        result.setFitWidth(16);

        return result;
    }

    private static GridPane generateTabContent(
            boolean debug,
            TextField ffprobeField,
            Button ffprobeSetButton,
            TextField ffmpegField,
            Button ffmpegSetButton,
            ComboBox<LanguageAlpha3Code> upperLanguageComboBox,
            ComboBox<LanguageAlpha3Code> lowerLanguageComboBox,
            Button swapLanguagesButton,
            Label resultLabel
    ) {
        GridPane result = new GridPane();

        result.setHgap(55);
        result.setPadding(GuiLauncher.TAB_PADDING);
        result.setGridLinesVisible(debug);

        result.getColumnConstraints().addAll(generateColumnConstraints());

        addRowForFfprobe(ffprobeField, ffprobeSetButton, result);
        addRowForFfmpeg(ffmpegField, ffmpegSetButton, result);
        addRowForUpperLanguage(upperLanguageComboBox, result);
        addRowForSwapLanguagesButton(swapLanguagesButton, result);
        addRowForLowerLanguage(lowerLanguageComboBox, result);
        addResultLabel(resultLabel, result);

        return result;
    }

    private static List<ColumnConstraints> generateColumnConstraints() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPrefWidth(275);
        firstColumn.setMinWidth(firstColumn.getPrefWidth());
        result.add(firstColumn);

        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setHgrow(Priority.ALWAYS);
        result.add(secondColumn);

        return result;
    }

    private static void addRowForFfprobe(TextField ffprobeField, Button ffprobeSetButton, GridPane pane) {
        Label descriptionLabel = new Label("Path to ffprobe");

        HBox fieldButtonBox = new HBox(ffprobeField, ffprobeSetButton);
        fieldButtonBox.setSpacing(20);
        HBox.setHgrow(ffprobeField, Priority.ALWAYS);

        pane.addRow(
                pane.getRowCount(),
                descriptionLabel,
                fieldButtonBox
        );

        GridPane.setMargin(descriptionLabel, new Insets(0, 0, 20, 0));
        GridPane.setMargin(fieldButtonBox, new Insets(0, 0, 20, 0));
        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(fieldButtonBox, HPos.RIGHT);
    }

    private static void addRowForFfmpeg(TextField ffmpegField, Button ffmpegSetButton, GridPane pane) {
        Label descriptionLabel = new Label("Path to ffmpeg");

        HBox fieldButtonBox = new HBox(ffmpegField, ffmpegSetButton);
        fieldButtonBox.setSpacing(20);
        HBox.setHgrow(ffmpegField, Priority.ALWAYS);

        pane.addRow(
                pane.getRowCount(),
                descriptionLabel,
                fieldButtonBox
        );

        GridPane.setMargin(descriptionLabel, new Insets(20, 0, 40, 0));
        GridPane.setMargin(fieldButtonBox, new Insets(20, 0, 40, 0));
        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(fieldButtonBox, HPos.RIGHT);
    }

    //todo make editable with drop-down
    private static void addRowForUpperLanguage(
            ComboBox<LanguageAlpha3Code> upperLanguageComboBox,
            GridPane pane
    ) {
        HBox descriptionAndInfo = generateDescriptionAndInfoIcon("Preferred language for upper subtitles");

        pane.addRow(
                pane.getRowCount(),
                descriptionAndInfo,
                upperLanguageComboBox
        );

        GridPane.setMargin(descriptionAndInfo, new Insets(0, 0, 0, 0));
        GridPane.setMargin(upperLanguageComboBox, new Insets(0, 0, 0, 0));
        GridPane.setHalignment(descriptionAndInfo, HPos.LEFT);
        GridPane.setHalignment(upperLanguageComboBox, HPos.RIGHT);
    }

    private static HBox generateDescriptionAndInfoIcon(String description) {
        HBox result = new HBox();

        ImageView imageView = getInfoImageView();

        /*
         * Have to use the wrapper for the image view because otherwise the tooltip won't be shown when hovering over
         * transparent parts of the image.
         */
        HBox imageViewWrapper = new HBox(imageView);
        imageViewWrapper.setAlignment(Pos.CENTER);
        Tooltip.install(imageViewWrapper, generateLanguageTooltip());

        result.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();

        result.getChildren().addAll(
                new Label(description),
                spacer,
                imageViewWrapper
        );

        HBox.setHgrow(spacer, Priority.ALWAYS);

        return result;
    }

    private static ImageView getInfoImageView() {
        Image result = new Image(SettingsTabView.class.getResourceAsStream("/info.png"));

        ImageView imageView = new ImageView(result);
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);

        return imageView;
    }

    private static Tooltip generateLanguageTooltip() {
        Tooltip result = new Tooltip(
                "this setting will be used to auto-detect subtitles\n"
                        + "for merging when working with videos"
        );

        result.setShowDelay(Duration.ZERO);
        result.setShowDuration(Duration.INDEFINITE);

        return result;
    }

    private static void addRowForSwapLanguagesButton(Button swapLanguagesButton, GridPane pane) {
        pane.addRow(
                pane.getRowCount(),
                new Region(),
                swapLanguagesButton
        );

        GridPane.setMargin(swapLanguagesButton, new Insets(8, 0, 8, 10));
        GridPane.setHalignment(swapLanguagesButton, HPos.LEFT);
    }

    //todo make editable with drop-down
    private static void addRowForLowerLanguage(
            ComboBox<LanguageAlpha3Code> lowerLanguageComboBox,
            GridPane pane
    ) {
        HBox descriptionAndInfo = generateDescriptionAndInfoIcon("Preferred language for lower subtitles");

        pane.addRow(
                pane.getRowCount(),
                descriptionAndInfo,
                lowerLanguageComboBox
        );

        GridPane.setMargin(descriptionAndInfo, new Insets(0, 0, 0, 0));
        GridPane.setMargin(lowerLanguageComboBox, new Insets(0, 0, 0, 0));
        GridPane.setHalignment(descriptionAndInfo, HPos.LEFT);
        GridPane.setHalignment(lowerLanguageComboBox, HPos.RIGHT);
    }

    private static void addResultLabel(Label resultLabel, GridPane pane) {
        pane.addRow(pane.getRowCount(), resultLabel);
        GridPane.setColumnSpan(resultLabel, pane.getColumnCount());
        GridPane.setMargin(resultLabel, new Insets(20, 0, 0, 0));
    }

    @Override
    public String getTabName() {
        return TAB_NAME;
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    void setLanguageCodesForComboBoxes(List<LanguageAlpha3Code> languageCodes) {
        upperLanguageComboBox.getItems().setAll(languageCodes);
        lowerLanguageComboBox.getItems().setAll(languageCodes);
    }

    void setFfprobeSetButtonHandler(EventHandler<ActionEvent> handler) {
        ffprobeSetButton.setOnAction(handler);
    }

    void setFfmpegSetButtonHandler(EventHandler<ActionEvent> handler) {
        ffmpegSetButton.setOnAction(handler);
    }

    void setUpperLanguageListener(ChangeListener<LanguageAlpha3Code> listener) {
        upperLanguageComboBox.getSelectionModel().selectedItemProperty().addListener(listener);
    }

    void setSwapLanguagesButtonHandler(EventHandler<ActionEvent> handler) {
        swapLanguagesButton.setOnAction(handler);
    }

    void setLowerLanguageListener(ChangeListener<LanguageAlpha3Code> listener) {
        lowerLanguageComboBox.getSelectionModel().selectedItemProperty().addListener(listener);
    }

    void updateFfprobeInfo(
            String fieldText,
            String buttonText,
            String fileChooserTitle,
            File fileChooserInitialDirectory
    ) {
        ffprobeField.setText(fieldText);
        ffprobeSetButton.setText(buttonText);
        ffprobeFileChooser.setTitle(fileChooserTitle);
        ffprobeFileChooser.setInitialDirectory(fileChooserInitialDirectory);
    }

    void updateFfmpegInfo(
            String fieldText,
            String buttonText,
            String fileChooserTitle,
            File fileChooserInitialDirectory
    ) {
        ffmpegField.setText(fieldText);
        ffmpegSetButton.setText(buttonText);
        ffmpegFileChooser.setTitle(fileChooserTitle);
        ffmpegFileChooser.setInitialDirectory(fileChooserInitialDirectory);
    }

    void setSelectedUpperLanguage(LanguageAlpha3Code languageCode) {
        upperLanguageComboBox.getSelectionModel().select(languageCode);
    }

    void setSelectedLowerLanguage(LanguageAlpha3Code languageCode) {
        lowerLanguageComboBox.getSelectionModel().select(languageCode);
    }

    void setSwapLanguagesButtonDisable(boolean disable) {
        swapLanguagesButton.setDisable(disable);
    }

    Optional<File> getSelectedFfprobeFile() {
        return Optional.ofNullable(ffprobeFileChooser.showOpenDialog(stage));
    }

    Optional<File> getSelectedFfmpegFile() {
        return Optional.ofNullable(ffmpegFileChooser.showOpenDialog(stage));
    }

    void clearResult() {
        resultLabel.setText("");
        resultLabel.getStyleClass().remove(GuiLauncher.LABEL_SUCCESS_CLASS);
        resultLabel.getStyleClass().remove(GuiLauncher.LABEL_ERROR_CLASS);
    }

    void showErrorMessage(String text) {
        clearResult();
        resultLabel.getStyleClass().add(GuiLauncher.LABEL_ERROR_CLASS);
        resultLabel.setText(text);
    }

    void showSuccessMessage(String text) {
        clearResult();
        resultLabel.getStyleClass().add(GuiLauncher.LABEL_SUCCESS_CLASS);
        resultLabel.setText(text);
    }

    private static class LanguageCodeStringConverter extends StringConverter<LanguageAlpha3Code> {
        private static final String LANGUAGE_NOT_SET = "language code is not set";

        @Override
        public String toString(LanguageAlpha3Code languageCode) {
            if (languageCode == null) {
                return LANGUAGE_NOT_SET;
            }

            return languageCode.getName() + " (" + languageCode.toString() + ")";
        }

        @Override
        public LanguageAlpha3Code fromString(String rawCode) {
            if (Objects.equals(rawCode, LANGUAGE_NOT_SET)) {
                return null;
            }

            int leftBracketIndex = rawCode.indexOf("(");

            /* + 4 because every code is 3 symbol long. */
            return LanguageAlpha3Code.getByCode(rawCode.substring(leftBracketIndex + 1, leftBracketIndex + 4));
        }
    }
}