package kirill.subtitlesmerger.gui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;

class MergeFilesTab {
    private TabPane mainPane;

    MergeFilesTab(TabPane mainPane) {
        this.mainPane = mainPane;
    }

    Tab generateTab() {
        Tab result = new Tab("Merge subtitle files");

        GridPane contentPane = new GridPane();

        contentPane.setVgap(30);
        contentPane.setPadding(new Insets(20));

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPercentWidth(70);
        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPercentWidth(30);
        contentPane.getColumnConstraints().addAll(firstColumn, secondColumn);

        Region topSpacer = new Region();
        contentPane.addRow(0, topSpacer);
        GridPane.setColumnSpan(topSpacer, 2);
        GridPane.setVgrow(topSpacer, Priority.ALWAYS);

        addRowWithLabelAndButton("Please choose the file with the upper subtitles", 1, contentPane);
        addRowWithLabelAndButton("Please choose the file with the lower subtitles", 2, contentPane);
        addRowWithLabelAndButton("Please choose where to save the result", 3, contentPane);

        Button mergeButton = new Button("Merge subtitles");
        contentPane.addRow(4, mergeButton);
        GridPane.setColumnSpan(mergeButton, 2);

        Region bottomSpacer = new Region();
        contentPane.addRow(5, bottomSpacer);
        GridPane.setColumnSpan(bottomSpacer, 2);
        GridPane.setVgrow(bottomSpacer, Priority.ALWAYS);

        result.setContent(contentPane);

        return result;
    }

    private void addRowWithLabelAndButton(String labelText, int index, GridPane pane) {
        Label label = new Label(labelText);
        Button button = new Button("Choose file");

        GridPane.setHalignment(label, HPos.LEFT);
        GridPane.setHalignment(button, HPos.RIGHT);

        pane.addRow(index, label, button);
    }
}
