package kirill.subtitlesmerger.gui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
class MergeFilesTab {
    private Stage stage;

    private TabPane mainPane;

    private boolean debug;

    private Button upperSubtitlesFileChooseButton;

    private Label upperSubtitlesPathLabel;

    private FileChooser upperSubtitlesFileChooser;

    private Button lowerSubtitlesFileChooseButton;

    private Label lowerSubtitlesPathLabel;

    private FileChooser lowerSubtitlesFileChooser;

    private Button resultFileChooseButton;

    private Label resultPathLabel;

    private FileChooser resultFileChooser;

    private Button mergeButton;

    MergeFilesTab(Stage stage, TabPane mainPane, boolean debug) {
        this.stage = stage;
        this.mainPane = mainPane;
        this.debug = debug;
    }

    Tab generateTab() {
        Tab result = new Tab("Merge subtitle files");

        result.setContent(generateContentPane());

        return result;
    }

    private GridPane generateContentPane() {
        GridPane contentPane = new GridPane();

        contentPane.setHgap(30);
        contentPane.setVgap(40);
        contentPane.setPadding(new Insets(20));
        contentPane.setGridLinesVisible(debug);

        contentPane.getColumnConstraints().addAll(generateColumnConstraints());
        addRowForUpperSubtitlesFile(contentPane);
        addRowForLowerSubtitlesFile(contentPane);
        addRowForResultFile(contentPane);
        addMergeButton(contentPane);
        addSpacer(contentPane);

        return contentPane;
    }

    private static List<ColumnConstraints> generateColumnConstraints() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPrefWidth(400);
        result.add(firstColumn);

        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPrefWidth(100);
        result.add(secondColumn);

        ColumnConstraints thirdColumn = new ColumnConstraints();
        thirdColumn.setHgrow(Priority.ALWAYS);
        result.add(thirdColumn);

        return result;
    }

    private void addRowForUpperSubtitlesFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose the file with the upper subtitles");

        upperSubtitlesFileChooseButton = new Button("Choose file");
        upperSubtitlesPathLabel = new Label("not selected");
        upperSubtitlesFileChooser = getFileChooser("Please choose the file with the upper subtitles");

        contentPane.addRow(
                contentPane.getRowCount(),
                descriptionLabel,
                upperSubtitlesFileChooseButton,
                upperSubtitlesPathLabel
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(upperSubtitlesFileChooseButton, HPos.RIGHT);
        GridPane.setHalignment(upperSubtitlesPathLabel, HPos.LEFT);
    }

    private static FileChooser getFileChooser(String title) {
        FileChooser result = new FileChooser();

        result.setTitle(title);
        result.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("subrip files *.srt", "*.srt")
        );

        return result;
    }

    private void addRowForLowerSubtitlesFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose the file with the lower subtitles");

        lowerSubtitlesFileChooseButton = new Button("Choose file");
        lowerSubtitlesPathLabel = new Label("not selected");
        lowerSubtitlesFileChooser = getFileChooser("Please choose the file with the lower subtitles");

        contentPane.addRow(
                contentPane.getRowCount(),
                descriptionLabel,
                lowerSubtitlesFileChooseButton,
                lowerSubtitlesPathLabel
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(lowerSubtitlesFileChooseButton, HPos.RIGHT);
        GridPane.setHalignment(lowerSubtitlesPathLabel, HPos.LEFT);
    }

    private void addRowForResultFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose where to save the result");

        resultFileChooseButton = new Button("Choose file");
        resultPathLabel = new Label("not selected");
        resultFileChooser = getFileChooser("Please choose where to save the result");

        contentPane.addRow(contentPane.getRowCount(), descriptionLabel, resultFileChooseButton, resultPathLabel);

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(resultFileChooseButton, HPos.RIGHT);
        GridPane.setHalignment(resultPathLabel, HPos.LEFT);
    }

    private void addMergeButton(GridPane contentPane) {
        mergeButton = new Button("Merge subtitles");
        mergeButton.setDisable(true);

        contentPane.addRow(contentPane.getRowCount(), mergeButton);
        GridPane.setColumnSpan(mergeButton, contentPane.getColumnCount());
    }

    private void addSpacer(GridPane contentPane) {
        Region bottomSpacer = new Region();
        contentPane.addRow(contentPane.getRowCount(), bottomSpacer);
        GridPane.setColumnSpan(bottomSpacer, contentPane.getColumnCount());
        GridPane.setVgrow(bottomSpacer, Priority.ALWAYS);
    }
}
