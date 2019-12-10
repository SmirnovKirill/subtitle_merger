package kirill.subtitlesmerger.gui.merge_in_videos_tab;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiLauncher;
import kirill.subtitlesmerger.logic.merge_in_files.entities.BriefFileInfo;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class RegularContentPane {
    private Button directoryChooseButton;

    private Label directoryPathLabel;

    private DirectoryChooser directoryChooser;

    private Label directoryIncorrectLabel;

    private CheckBox showOnlyValidCheckBox;

    private TableWithFiles tableWithFiles;

    @Getter
    private Pane regularContentPane;

    RegularContentPane(boolean debug) {
        this.directoryChooseButton = new Button("Choose file");
        this.directoryPathLabel = new Label("not selected");
        this.directoryChooser = generateDirectoryChooser();
        this.directoryIncorrectLabel = generateDirectoryIncorrectLabel();
        this.showOnlyValidCheckBox = new CheckBox("Show only valid video files");
        this.tableWithFiles = new TableWithFiles(debug);
        this.regularContentPane = generatePane(
                debug,
                directoryChooseButton,
                directoryPathLabel,
                showOnlyValidCheckBox,
                directoryIncorrectLabel,
                tableWithFiles
        );
    }

    private static DirectoryChooser generateDirectoryChooser() {
        DirectoryChooser result = new DirectoryChooser();

        result.setTitle("choose the directory with videos");

        return result;
    }

    private static Label generateDirectoryIncorrectLabel() {
        Label result = new Label();

        result.managedProperty().bind(result.visibleProperty());
        result.getStyleClass().add(GuiLauncher.LABEL_ERROR_CLASS);
        result.setVisible(false);

        return result;
    }

    private static Pane generatePane(
            boolean debug,
            Button directoryChooseButton,
            Label directoryPathLabel,
            CheckBox showOnlyValidCheckBox,
            Label directoryIncorrectLabel,
            TableWithFiles tableWithFiles
    ) {
        VBox result = new VBox();

        result.setFillWidth(true);
        result.setPadding(GuiLauncher.TAB_PADDING);
        result.setSpacing(10);

        Pane controlsPane = generateControlsPane(
                debug,
                directoryChooseButton,
                directoryPathLabel,
                showOnlyValidCheckBox
        );

        result.getChildren().addAll(controlsPane, directoryIncorrectLabel, tableWithFiles.getMainNode());

        VBox.setVgrow(tableWithFiles.getMainNode(), Priority.ALWAYS);

        return result;
    }

    private static Pane generateControlsPane(
            boolean debug,
            Button directoryChooseButton,
            Label directoryPathLabel,
            CheckBox showOnlyValidCheckBox
    ) {
        GridPane result = new GridPane();

        result.setHgap(30);
        result.setVgap(40); //todo remove
        result.setGridLinesVisible(debug);

        result.getColumnConstraints().addAll(generateControlPaneColumnConstraints());

        addFirstControlsRow(directoryChooseButton, directoryPathLabel, result);
        addSecondControlsRow(showOnlyValidCheckBox, result);

        return result;
    }

    private static List<ColumnConstraints> generateControlPaneColumnConstraints() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPrefWidth(400);
        firstColumn.setMinWidth(firstColumn.getPrefWidth());
        result.add(firstColumn);

        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPrefWidth(100);
        secondColumn.setMinWidth(secondColumn.getPrefWidth());
        result.add(secondColumn);

        ColumnConstraints thirdColumn = new ColumnConstraints();
        thirdColumn.setHgrow(Priority.ALWAYS);
        result.add(thirdColumn);

        return result;
    }

    private static void addFirstControlsRow(Button directoryChooseButton, Label directoryPathLabel, GridPane pane) {
        Label descriptionLabel = new Label("Please choose the directory with videos");

        pane.addRow(
                pane.getRowCount(),
                descriptionLabel,
                directoryChooseButton,
                directoryPathLabel
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(directoryChooseButton, HPos.RIGHT);
        GridPane.setHalignment(directoryPathLabel, HPos.LEFT);
    }

    private static void addSecondControlsRow(CheckBox showOnlyValidCheckBox, GridPane pane) {
        HBox row = new HBox();

        row.setSpacing(20);
        row.setAlignment(Pos.CENTER_LEFT);

        Image image = new Image(MergeInVideosTabView.class.getResourceAsStream("/refresh.png"));
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);
        imageView.setSmooth(true);

        Button refreshButton = new Button("Refresh", imageView);

        Button getSubtitleSizesButton = new Button("Get subtitle sizes");

        Button injectSubtitlesButton = new Button("Inject subtitles");

        row.getChildren().addAll(showOnlyValidCheckBox, refreshButton, getSubtitleSizesButton, injectSubtitlesButton);

        pane.addRow(
                pane.getRowCount(),
                row
        );

        GridPane.setHalignment(row, HPos.LEFT);
        GridPane.setColumnSpan(row, pane.getColumnCount());
    }

    void setDirectoryChooseButtonHandler(EventHandler<ActionEvent> handler) {
        directoryChooseButton.setOnAction(handler);
    }

    void setDirectoryChooserInitialDirectory(File initialDirectory) {
        directoryChooser.setInitialDirectory(initialDirectory);
    }

    void setShowOnlyValidCheckBoxChangeListener(ChangeListener<Boolean> listener) {
        showOnlyValidCheckBox.selectedProperty().addListener(listener);
    }

    Optional<File> getChosenDirectory(Stage stage) {
        return Optional.ofNullable(directoryChooser.showDialog(stage));
    }

    void setDirectoryPathLabel(String text) {
        directoryPathLabel.setText(text);
    }

    void setFiles(List<BriefFileInfo> briefFilesInfo) {
        tableWithFiles.setFiles(briefFilesInfo);
    }

    void hide() {
        regularContentPane.setVisible(false);
    }

    void show() {
        regularContentPane.setVisible(true);
    }
}
