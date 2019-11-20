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

import java.util.ArrayList;
import java.util.List;

class MergeFilesTab {
    private Stage stage;

    private TabPane mainPane;

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

        contentPane.setVgap(40);
        contentPane.setPadding(new Insets(20));

        contentPane.getColumnConstraints().addAll(generateColumnConstraints());
        addRowForUpperSubtitles(contentPane);
        addRowForLowerSubtitles(contentPane);
        addRowForResult(contentPane);
        addMergeButton(contentPane);
        addSpacer(contentPane);

        return contentPane;
    }

    private static List<ColumnConstraints> generateColumnConstraints() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPercentWidth(70);
        result.add(firstColumn);

        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPercentWidth(30);
        result.add(secondColumn);

        return result;
    }

    private void addRowForUpperSubtitles(GridPane contentPane) {
        Label label = new Label("Please choose the file with the upper subtitle");
        Button button = new Button("Choose file");

        GridPane.setHalignment(label, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);

        button.setOnAction(e -> new FileChooser().showOpenDialog(stage));

        contentPane.addRow(contentPane.getRowCount(), label, button);
    }

    private void addRowForLowerSubtitles(GridPane contentPane) {
        Label label = new Label("Please choose the file with the lower subtitle");
        Button button = new Button("Choose file");

        GridPane.setHalignment(label, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);

        button.setOnAction(e -> new FileChooser().showOpenDialog(stage));

        contentPane.addRow(contentPane.getRowCount(), label, button);
    }

    private void addRowForResult(GridPane contentPane) {
        Label label = new Label("Please choose where to save the result");
        Button button = new Button("Choose file");

        GridPane.setHalignment(label, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);

        button.setOnAction(e -> new FileChooser().showOpenDialog(stage));

        contentPane.addRow(contentPane.getRowCount(), label, button);
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
