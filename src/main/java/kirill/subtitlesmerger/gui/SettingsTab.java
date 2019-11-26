package kirill.subtitlesmerger.gui;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class SettingsTab {
    private static final LanguageCodeStringConverter LANGUAGE_CODE_STRING_CONVERTER = new LanguageCodeStringConverter();

    private Stage stage;

    private boolean debug;

    private TextField ffprobeField;

    private Button ffprobeSetButton;

    private FileChooser ffprobeFileChooser;

    private TextField ffmpegField;

    private Button ffmpegSetButton;

    private FileChooser ffmpegFileChooser;

    private ComboBox<LanguageAlpha3Code> upperSubtitlesLanguageComboBox;

    private ComboBox<LanguageAlpha3Code> lowerSubtitlesLanguageComboBox;

    private Label resultLabel;

    SettingsTab(Stage stage, boolean debug) {
        this.stage = stage;
        this.debug = debug;
    }

    Tab generateTab() {
        Tab result = new Tab("Settings");

        result.setGraphic(generateTabGraphic());
        result.setContent(generateContentPane());

        return result;
    }

    private static ImageView generateTabGraphic() {
        return new ImageView(new Image(SettingsTab.class.getResourceAsStream("/settings.png")));
    }

    private GridPane generateContentPane() {
        GridPane contentPane = new GridPane();

        contentPane.setHgap(30);
        contentPane.setVgap(40);
        contentPane.setPadding(new Insets(20));
        contentPane.setGridLinesVisible(debug);

        contentPane.getColumnConstraints().addAll(generateColumnConstraints());

        addRowForFfprobe(contentPane);
        addRowForFfmpeg(contentPane);
        addRowForUpperSubtitlesLanguage(contentPane);
        addRowForLowerSubtitlesLanguage(contentPane);
        addResultLabel(contentPane);

        return contentPane;
    }

    private static List<ColumnConstraints> generateColumnConstraints() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPrefWidth(300);
        firstColumn.setMinWidth(firstColumn.getPrefWidth());
        result.add(firstColumn);

        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setHgrow(Priority.ALWAYS);
        result.add(secondColumn);

        return result;
    }

    private void addRowForFfprobe(GridPane contentPane) {
        Label descriptionLabel = new Label("Path to ffprobe");

        ffprobeField = new TextField();
        ffprobeField.setEditable(false);

        ffprobeSetButton = new Button();

        HBox fieldButtonBox = new HBox(ffprobeField, ffprobeSetButton);
        fieldButtonBox.setSpacing(20);
        HBox.setHgrow(ffprobeField, Priority.ALWAYS);

        ffprobeFileChooser = new FileChooser();

        contentPane.addRow(
                contentPane.getRowCount(),
                descriptionLabel,
                fieldButtonBox
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(fieldButtonBox, HPos.RIGHT);
    }

    private void addRowForFfmpeg(GridPane contentPane) {
        Label descriptionLabel = new Label("Path to ffmpeg");

        ffmpegField = new TextField();
        ffmpegField.setEditable(false);

        ffmpegSetButton = new Button();

        HBox fieldButtonBox = new HBox(ffmpegField, ffmpegSetButton);
        fieldButtonBox.setSpacing(20);
        HBox.setHgrow(ffmpegField, Priority.ALWAYS);

        ffmpegFileChooser = new FileChooser();

        contentPane.addRow(
                contentPane.getRowCount(),
                descriptionLabel,
                fieldButtonBox
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(fieldButtonBox, HPos.RIGHT);
    }

    //todo make editable with drop-down
    private void addRowForUpperSubtitlesLanguage(GridPane contentPane) {
        Label descriptionLabel = new Label("Preferred language for upper subtitles");

        upperSubtitlesLanguageComboBox = new ComboBox<>();
        upperSubtitlesLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);

        contentPane.addRow(
                contentPane.getRowCount(),
                descriptionLabel,
                upperSubtitlesLanguageComboBox
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(upperSubtitlesLanguageComboBox, HPos.RIGHT);
    }

    //todo make editable with drop-down
    private void addRowForLowerSubtitlesLanguage(GridPane contentPane) {
        Label descriptionLabel = new Label("Preferred language for lower subtitles");

        lowerSubtitlesLanguageComboBox = new ComboBox<>();
        lowerSubtitlesLanguageComboBox.setConverter(LANGUAGE_CODE_STRING_CONVERTER);

        contentPane.addRow(
                contentPane.getRowCount(),
                descriptionLabel,
                lowerSubtitlesLanguageComboBox
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(lowerSubtitlesLanguageComboBox, HPos.RIGHT);
    }

    private void addResultLabel(GridPane contentPane) {
        resultLabel = new Label();
        contentPane.addRow(contentPane.getRowCount(), resultLabel);
        GridPane.setColumnSpan(resultLabel, contentPane.getColumnCount());
    }

    void setLanguageCodesForComboBoxes(List<LanguageAlpha3Code> languageCodes) {
        upperSubtitlesLanguageComboBox.getItems().setAll(languageCodes);
        lowerSubtitlesLanguageComboBox.getItems().setAll(languageCodes);
    }

    void setFfprobeSetButtonHandler(EventHandler<ActionEvent> handler) {
        ffprobeSetButton.setOnAction(handler);
    }

    void setFfmpegSetButtonHandler(EventHandler<ActionEvent> handler) {
        ffmpegSetButton.setOnAction(handler);
    }

    Optional<File> getSelectedFfprobeFile() {
        return Optional.ofNullable(ffprobeFileChooser.showOpenDialog(stage));
    }

    Optional<File> getSelectedFfmpegFile() {
        return Optional.ofNullable(ffmpegFileChooser.showOpenDialog(stage));
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