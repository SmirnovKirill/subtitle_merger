package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class MergeFilesTab {
    private Stage stage;

    private TabPane mainPane;

    private boolean debug;

    private Label upperSubtitlesPathLabel;

    private Label lowerSubtitlesPathLabel;

    private Label resultPathLabel;

    private File upperSubtitlesFile;

    private File lowerSubtitlesFile;

    private File resultFile;

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

        Button button = new Button("Choose file");
        button.setOnAction(this::upperSubtitlesFileButtonClicked);

        upperSubtitlesPathLabel = new Label("not selected");

        contentPane.addRow(contentPane.getRowCount(), descriptionLabel, button, upperSubtitlesPathLabel);

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);
        GridPane.setHalignment(upperSubtitlesPathLabel, HPos.LEFT);
    }

    private void upperSubtitlesFileButtonClicked(ActionEvent event) {
        FileChooser fileChooser = getFileChooser("Please choose the file with the upper subtitles");

        upperSubtitlesFile = fileChooser.showOpenDialog(stage);
        upperSubtitlesPathLabel.setText(getPathLabelText(upperSubtitlesFile));
    }

    private FileChooser getFileChooser(String title) {
        FileChooser result = new FileChooser();

        result.setTitle(title);
        result.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("subrip files *.srt", "*.srt")
        );

        return result;
    }

    private static String getPathLabelText(File file) {
        if (file == null) {
            return "not selected";
        }

        return file.getAbsolutePath();
    }

    private void addRowForLowerSubtitlesFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose the file with the lower subtitles");

        Button button = new Button("Choose file");
        button.setOnAction(this::lowerSubtitlesFileButtonClicked);

        lowerSubtitlesPathLabel = new Label("not selected");

        contentPane.addRow(contentPane.getRowCount(), descriptionLabel, button, lowerSubtitlesPathLabel);

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);
        GridPane.setHalignment(lowerSubtitlesPathLabel, HPos.LEFT);
    }

    private void lowerSubtitlesFileButtonClicked(ActionEvent event) {
        FileChooser fileChooser = getFileChooser("Please choose the file with the lower subtitles");

        lowerSubtitlesFile = fileChooser.showOpenDialog(stage);
        lowerSubtitlesPathLabel.setText(getPathLabelText(lowerSubtitlesFile));
    }

    private void addRowForResultFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose where to save the result");

        Button button = new Button("Choose file");
        button.setOnAction(this::resultFileButtonClicked);

        resultPathLabel = new Label("not selected");

        contentPane.addRow(contentPane.getRowCount(), descriptionLabel, button, resultPathLabel);

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);
        GridPane.setHalignment(resultPathLabel, HPos.LEFT);
    }

    private void resultFileButtonClicked(ActionEvent event) {
        FileChooser fileChooser = getFileChooser("Please choose where to save the result");

        resultFile = fileChooser.showSaveDialog(stage);
        resultPathLabel.setText(getPathLabelText(resultFile));
    }

    private void addMergeButton(GridPane contentPane) {
        Button mergeButton = new Button("Merge subtitles");
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
