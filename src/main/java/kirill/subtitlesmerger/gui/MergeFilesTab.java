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

    private Label upperSubtitlesPathLabel;

    private Label lowerSubtitlesPathLabel;

    private Label resultPathLabel;

    private File fileWithUpperSubtitles;

    private File fileWithLowerSubtitles;

    private File resultFile;

    MergeFilesTab(Stage stage, TabPane mainPane) {
        this.stage = stage;
        this.mainPane = mainPane;
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
        Label descriptionLabel = new Label("Please choose the file with the upper subtitle");

        Button button = new Button("Choose file");
        button.setOnAction(this::upperSubtitlesFileButtonClicked);

        upperSubtitlesPathLabel = new Label("not selected");

        contentPane.addRow(contentPane.getRowCount(), descriptionLabel, button, upperSubtitlesPathLabel);

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);
        GridPane.setHalignment(upperSubtitlesPathLabel, HPos.LEFT);
    }

    private void upperSubtitlesFileButtonClicked(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();

        fileWithUpperSubtitles = fileChooser.showOpenDialog(stage);
        if (fileWithUpperSubtitles == null) {
            upperSubtitlesPathLabel.setText("not selected");
        } else {
            upperSubtitlesPathLabel.setText(fileWithUpperSubtitles.getAbsolutePath());
        }
    }

    private void addRowForLowerSubtitlesFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose the file with the lower subtitle");

        Button button = new Button("Choose file");
        button.setOnAction(this::lowerSubtitlesFileButtonClicked);

        upperSubtitlesPathLabel = new Label("not selected");

        contentPane.addRow(contentPane.getRowCount(), descriptionLabel, button, upperSubtitlesPathLabel);

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);
        GridPane.setHalignment(upperSubtitlesPathLabel, HPos.LEFT);
    }

    private void lowerSubtitlesFileButtonClicked(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();

        fileWithUpperSubtitles = fileChooser.showOpenDialog(stage);
        if (fileWithUpperSubtitles == null) {
            upperSubtitlesPathLabel.setText("not selected");
        } else {
            upperSubtitlesPathLabel.setText(fileWithUpperSubtitles.getAbsolutePath());
        }
    }

    private void addRowForResultFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose where to save the result");

        Button button = new Button("Choose file");
        button.setOnAction(this::resultFileButtonClicked);

        upperSubtitlesPathLabel = new Label("not selected");

        contentPane.addRow(contentPane.getRowCount(), descriptionLabel, button, upperSubtitlesPathLabel);

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);
        GridPane.setHalignment(upperSubtitlesPathLabel, HPos.LEFT);
    }

    private void resultFileButtonClicked(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();

        fileWithUpperSubtitles = fileChooser.showOpenDialog(stage);
        if (fileWithUpperSubtitles == null) {
            upperSubtitlesPathLabel.setText("not selected");
        } else {
            upperSubtitlesPathLabel.setText(fileWithUpperSubtitles.getAbsolutePath());
        }
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
