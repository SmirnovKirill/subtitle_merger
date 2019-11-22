package kirill.subtitlesmerger.gui;

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

import java.util.ArrayList;
import java.util.List;

class SettingsTab {
    private TabPane mainPane;

    private boolean debug;

    private TextField ffprobeField;

    private Button ffprobeSetButton;

    private FileChooser ffprobeFileChooser;

    private TextField ffmpegField;

    private Button ffmpegSetButton;

    private FileChooser ffmpegFileChooser;

    private TextField upperSubtitlesLanguageField;

    private TextField lowerSubtitlesLanguageField;

    SettingsTab(TabPane mainPane, boolean debug) {
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
       // addRowForUpperSubtitlesLanguage(contentPane);
       // addRowForLowerSubtitlesLanguage(contentPane);

        return contentPane;
    }

    private static List<ColumnConstraints> generateColumnConstraints() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPrefWidth(200);
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

        ffprobeSetButton = new Button("Choose file"); //todo modify

        HBox fieldButtonBox = new HBox(ffprobeField, ffprobeSetButton);
        fieldButtonBox.setSpacing(20);
        HBox.setHgrow(ffprobeField, Priority.ALWAYS);

        ffprobeFileChooser = new FileChooser();
        ffprobeFileChooser.setTitle("choose file"); //todo modify

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

        ffmpegSetButton = new Button("Choose file"); //todo modify

        HBox fieldButtonBox = new HBox(ffmpegField, ffmpegSetButton);
        fieldButtonBox.setSpacing(20);
        HBox.setHgrow(ffmpegField, Priority.ALWAYS);

        ffmpegFileChooser = new FileChooser();
        ffmpegFileChooser.setTitle("choose file"); //todo modify

        contentPane.addRow(
                contentPane.getRowCount(),
                descriptionLabel,
                fieldButtonBox
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(fieldButtonBox, HPos.RIGHT);
    }
}