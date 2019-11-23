package kirill.subtitlesmerger.gui;

import com.neovisionaries.i18n.LanguageAlpha3Code;
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
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
class SettingsTab {
    private Stage stage;

    private TabPane mainPane;

    private boolean debug;

    private TextField ffprobeField;

    private Button ffprobeSetButton;

    private FileChooser ffprobeFileChooser;

    private TextField ffmpegField;

    private Button ffmpegSetButton;

    private FileChooser ffmpegFileChooser;

    private ComboBox<String> upperSubtitlesLanguageComboBox;

    private ComboBox<String> lowerSubtitlesLanguageComboBox;

    SettingsTab(Stage stage, TabPane mainPane, boolean debug) {
        this.stage = stage;
        this.mainPane = mainPane;
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
        upperSubtitlesLanguageComboBox.getItems().addAll(getLanguagesList());

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
        lowerSubtitlesLanguageComboBox.getItems().addAll(getLanguagesList());

        contentPane.addRow(
                contentPane.getRowCount(),
                descriptionLabel,
                lowerSubtitlesLanguageComboBox
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(lowerSubtitlesLanguageComboBox, HPos.RIGHT);
    }

    //todo move to controls
    private static List<String> getLanguagesList() {
        return Arrays.stream(LanguageAlpha3Code.values())
                .filter(code -> code != LanguageAlpha3Code.undefined)
                .map(code -> code.getName() + " (" + code.name() + ")")
                .sorted()
                .collect(Collectors.toList());
    }
}