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

class MergeFilesTab {
    private Stage stage;

    private TabPane mainPane;

    MergeFilesTab(Stage stage, TabPane mainPane) {
        this.stage = stage;
        this.mainPane = mainPane;
    }

    Tab generateTab() {
        Tab result = new Tab("Merge subtitle files");

        GridPane contentPane = new GridPane();

        contentPane.setVgap(40);
        contentPane.setPadding(new Insets(20));

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPercentWidth(70);
        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPercentWidth(30);
        contentPane.getColumnConstraints().addAll(firstColumn, secondColumn);

        addRowWithLabelAndButton(
                "Please choose the file with the upper subtitles",
                contentPane.getRowCount(),
                contentPane
        );
        addRowWithLabelAndButton(
                "Please choose the file with the lower subtitles",
                contentPane.getRowCount(),
                contentPane
        );
        addRowWithLabelAndButton(
                "Please choose where to save the result",
                contentPane.getRowCount(),
                contentPane
        );

        Button mergeButton = new Button("Merge subtitles");
        contentPane.addRow(contentPane.getRowCount(), mergeButton);
        GridPane.setColumnSpan(mergeButton, contentPane.getColumnCount());

        Region bottomSpacer = new Region();
        contentPane.addRow(contentPane.getRowCount(), bottomSpacer);
        GridPane.setColumnSpan(bottomSpacer, contentPane.getColumnCount());
        GridPane.setVgrow(bottomSpacer, Priority.ALWAYS);

        result.setContent(contentPane);

        return result;
    }

    private void addRowWithLabelAndButton(String labelText, int index, GridPane pane) {
        Label label = new Label(labelText);
        Button button = new Button("Choose file");

        GridPane.setHalignment(label, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);

        button.setOnAction(e -> new FileChooser().showOpenDialog(stage));

        pane.addRow(index, label, button);
    }
}
