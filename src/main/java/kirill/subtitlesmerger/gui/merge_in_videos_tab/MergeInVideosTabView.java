package kirill.subtitlesmerger.gui.merge_in_videos_tab;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiLauncher;
import kirill.subtitlesmerger.gui.TabView;
import kirill.subtitlesmerger.logic.data.BriefFileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MergeInVideosTabView implements TabView {
    private static final String TAB_NAME = "Merge subtitles in videos";

    private Stage stage;

    private boolean debug;

    private Tab tab;

    private MissingSettingsPane missingSettingsPane;

    private Node regularContent;

    private Button directoryChooseButton;

    private Label directoryPathLabel;

    private DirectoryChooser directoryChooser;

    private Label directoryIncorrectLabel;

    private CheckBox showOnlyValidCheckBox;

    private TableWithFiles tableWithFiles;

    public MergeInVideosTabView(Stage stage, boolean debug) {
        this.stage = stage;
        this.debug = debug;
        this.tab = new Tab(TAB_NAME);
        this.missingSettingsPane = new MissingSettingsPane();
        this.tableWithFiles = new TableWithFiles(debug);
        this.regularContent = generateRegularContent();
    }

    private Node generateRegularContent() {
        VBox result = new VBox();

        result.setFillWidth(true);
        result.setPadding(GuiLauncher.TAB_PADDING);
        result.setSpacing(10);

        Node controlsNode = generateControlsNode();
        directoryIncorrectLabel = generateDirectoryIncorrectLabel();

        result.getChildren().addAll(controlsNode, directoryIncorrectLabel, tableWithFiles.getMainNode());

        VBox.setVgrow(tableWithFiles.getMainNode(), Priority.ALWAYS);

        return result;
    }

    private Node generateControlsNode() {
        GridPane result = new GridPane();

        result.setHgap(30);
        result.setVgap(40); //todo remove
        result.setGridLinesVisible(debug);

        result.getColumnConstraints().addAll(generateControlNodeColumns());

        addFirstControlsRow(result);
        addSecondControlsRow(result);

        return result;
    }

    private void addFirstControlsRow(GridPane pane) {
        Label descriptionLabel = new Label("Please choose the directory with videos");

        directoryChooseButton = new Button("Choose file");
        directoryPathLabel = new Label("not selected");
        directoryChooser = generateDirectoryChooser();

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

    private void addSecondControlsRow(GridPane pane) {
        HBox row = new HBox();

        row.setSpacing(20);
        row.setAlignment(Pos.CENTER_LEFT);

        showOnlyValidCheckBox = new CheckBox("Show only valid video files");

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

    private static List<ColumnConstraints> generateControlNodeColumns() {
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

    @Override
    public String getTabName() {
        return TAB_NAME;
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    void setGoToSettingsLinkHandler(EventHandler<ActionEvent> handler) {
        missingSettingsPane.setGoToSettingsLinkHandler(handler);
    }

    void showMissingSettings(List<String> missingSettings) {
        missingSettingsPane.showMissingSettings(missingSettings);
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

    Optional<File> getChosenDirectory() {
        return Optional.ofNullable(directoryChooser.showDialog(stage));
    }

    void setDirectoryPathLabel(String text) {
        directoryPathLabel.setText(text);
    }

    void showDirectoryErrorMessage(String text) {
        tableWithFiles.hide();
        directoryIncorrectLabel.setVisible(true);
        directoryIncorrectLabel.setText(text);
    }

    void showTableWithFiles(List<BriefFileInfo> briefFilesInfo) {
        directoryIncorrectLabel.setVisible(false);
        tableWithFiles.show();

        tableWithFiles.setFiles(briefFilesInfo);
    }

    void showRegularContent() {
        tab.setContent(regularContent);
    }
}
