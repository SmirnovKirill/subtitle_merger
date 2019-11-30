package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class MergeFilesTabView implements TabView {
    private static final String TAB_NAME = "Merge subtitle files";

    private Stage stage;

    private boolean debug;

    private Tab tab;

    private Button upperSubtitlesFileChooseButton;

    private Label upperSubtitlesPathLabel;

    private FileChooser upperSubtitlesFileChooser;

    private Button lowerSubtitlesFileChooseButton;

    private Label lowerSubtitlesPathLabel;

    private FileChooser lowerSubtitlesFileChooser;

    private Button mergedSubtitlesFileChooseButton;

    private Label mergedSubtitlesPathLabel;

    private FileChooser mergedSubtitlesFileChooser;

    private Button mergeButton;

    private Label resultLabel;

    MergeFilesTabView(Stage stage, boolean debug) {
        this.stage = stage;
        this.debug = debug;
        this.tab = generateTab();
    }

    private Tab generateTab() {
        Tab result = new Tab(TAB_NAME);

        result.setContent(generateContentPane());

        return result;
    }

    private GridPane generateContentPane() {
        GridPane contentPane = new GridPane();

        contentPane.setHgap(30);
        contentPane.setVgap(40);
        contentPane.setPadding(GuiLauncher.TAB_PADDING);
        contentPane.setGridLinesVisible(debug);

        contentPane.getColumnConstraints().addAll(generateColumnConstraints());

        addRowForUpperSubtitlesFile(contentPane);
        addRowForLowerSubtitlesFile(contentPane);
        addRowForMergedSubtitlesFile(contentPane);
        addMergeButton(contentPane);
        addResultLabel(contentPane);

        return contentPane;
    }

    private static List<ColumnConstraints> generateColumnConstraints() {
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

    private void addRowForUpperSubtitlesFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose the file with the upper subtitles");

        upperSubtitlesFileChooseButton = new Button("Choose file");
        upperSubtitlesPathLabel = new Label("not selected");
        upperSubtitlesFileChooser = generateFileChooser("Please choose the file with the upper subtitles");

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

    private static FileChooser generateFileChooser(String title) {
        FileChooser result = new FileChooser();

        result.setTitle(title);
        result.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("subrip files (*.srt)", "*.srt")
        );

        return result;
    }

    private void addRowForLowerSubtitlesFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose the file with the lower subtitles");

        lowerSubtitlesFileChooseButton = new Button("Choose file");
        lowerSubtitlesPathLabel = new Label("not selected");
        lowerSubtitlesFileChooser = generateFileChooser("Please choose the file with the lower subtitles");

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

    private void addRowForMergedSubtitlesFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose where to save the result");

        mergedSubtitlesFileChooseButton = new Button("Choose file");
        mergedSubtitlesPathLabel = new Label("not selected");
        mergedSubtitlesFileChooser = generateFileChooser("Please choose where to save the result");

        contentPane.addRow(
                contentPane.getRowCount(),
                descriptionLabel,
                mergedSubtitlesFileChooseButton,
                mergedSubtitlesPathLabel
        );

        GridPane.setHalignment(descriptionLabel, HPos.LEFT);
        GridPane.setHalignment(mergedSubtitlesFileChooseButton, HPos.RIGHT);
        GridPane.setHalignment(mergedSubtitlesPathLabel, HPos.LEFT);
    }

    private void addMergeButton(GridPane contentPane) {
        mergeButton = new Button("Merge subtitles");
        mergeButton.setDisable(true);

        contentPane.addRow(contentPane.getRowCount(), mergeButton);
        GridPane.setColumnSpan(mergeButton, contentPane.getColumnCount());
    }

    private void addResultLabel(GridPane contentPane) {
        resultLabel = new Label();
        contentPane.addRow(contentPane.getRowCount(), resultLabel);
        GridPane.setColumnSpan(resultLabel, contentPane.getColumnCount());
    }

    @Override
    public String getTabName() {
        return TAB_NAME;
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    void setUpperSubtitlesFileChooseButtonHandler(EventHandler<ActionEvent> handler) {
        upperSubtitlesFileChooseButton.setOnAction(handler);
    }

    void setLowerSubtitlesFileChooseButtonHandler(EventHandler<ActionEvent> handler) {
        lowerSubtitlesFileChooseButton.setOnAction(handler);
    }

    void setMergedSubtitlesFileChooseButtonHandler(EventHandler<ActionEvent> handler) {
        mergedSubtitlesFileChooseButton.setOnAction(handler);
    }

    void setMergeButtonHandler(EventHandler<ActionEvent> handler) {
        mergeButton.setOnAction(handler);
    }

    void updateFileChooserInitialDirectories(
            File upperSubtitlesDirectory,
            File lowerSubtitlesDirectory,
            File mergedSubtitlesDirectory
    ) {
        if (upperSubtitlesDirectory != null) {
            upperSubtitlesFileChooser.setInitialDirectory(upperSubtitlesDirectory);
        }
        if (lowerSubtitlesDirectory != null) {
            lowerSubtitlesFileChooser.setInitialDirectory(lowerSubtitlesDirectory);
        }
        if (mergedSubtitlesDirectory != null) {
            mergedSubtitlesFileChooser.setInitialDirectory(mergedSubtitlesDirectory);
        }
    }

    Optional<File> getSelectedUpperSubtitlesFile() {
        return Optional.ofNullable(upperSubtitlesFileChooser.showOpenDialog(stage));
    }

    Optional<File> getSelectedLowerSubtitlesFile() {
        return Optional.ofNullable(lowerSubtitlesFileChooser.showOpenDialog(stage));
    }

    Optional<File> getSelectedMergedSubtitlesFile() {
        return Optional.ofNullable(mergedSubtitlesFileChooser.showSaveDialog(stage));
    }

    void setMergeButtonDisable(boolean disable) {
        mergeButton.setDisable(disable);
    }

    void setUpperSubtitlesPathLabel(String text) {
        this.upperSubtitlesPathLabel.setText(text);
    }

    void setLowerSubtitlesPathLabel(String text) {
        this.lowerSubtitlesPathLabel.setText(text);
    }

    void setMergedSubtitlesPathLabel(String text) {
        this.mergedSubtitlesPathLabel.setText(text);
    }

    void removeErrorsAndResult() {
        removeButtonErrorClass(upperSubtitlesFileChooseButton);
        removeButtonErrorClass(lowerSubtitlesFileChooseButton);
        removeButtonErrorClass(mergedSubtitlesFileChooseButton);
        clearResult();
    }

    private void removeButtonErrorClass(Button button) {
        button.getStyleClass().remove(GuiLauncher.BUTTON_ERROR_CLASS);
    }

    private void clearResult() {
        resultLabel.setText("");
        resultLabel.getStyleClass().remove(GuiLauncher.LABEL_SUCCESS_CLASS);
        resultLabel.getStyleClass().remove(GuiLauncher.LABEL_ERROR_CLASS);
    }

    void showErrors(
            String upperSubtitlesFileErrorMessage,
            String lowerSubtitlesFileErrorMessage,
            String mergedSubtitlesFileErrorMessage
    ) {
        if (!StringUtils.isBlank(upperSubtitlesFileErrorMessage)) {
            addButtonErrorClass(upperSubtitlesFileChooseButton);
        } else {
            removeButtonErrorClass(upperSubtitlesFileChooseButton);
        }

        if (!StringUtils.isBlank(lowerSubtitlesFileErrorMessage)) {
            addButtonErrorClass(lowerSubtitlesFileChooseButton);
        } else {
            removeButtonErrorClass(lowerSubtitlesFileChooseButton);
        }

        if (!StringUtils.isBlank(mergedSubtitlesFileErrorMessage)) {
            addButtonErrorClass(mergedSubtitlesFileChooseButton);
        } else {
            removeButtonErrorClass(mergedSubtitlesFileChooseButton);
        }

        boolean atLeastOneError = !StringUtils.isBlank(upperSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(lowerSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(mergedSubtitlesFileErrorMessage);
        if (!atLeastOneError) {
            throw new IllegalStateException();
        }

        StringBuilder combinedErrorsMessage = new StringBuilder("Can't merge subtitles:");

        if (!StringUtils.isBlank(upperSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\n").append("\u2022").append(" ").append(upperSubtitlesFileErrorMessage);
        }

        if (!StringUtils.isBlank(lowerSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\n").append("\u2022").append(" ").append(lowerSubtitlesFileErrorMessage);
        }

        if (!StringUtils.isBlank(mergedSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\n").append("\u2022").append(" ").append(mergedSubtitlesFileErrorMessage);
        }

        showErrorMessage(combinedErrorsMessage.toString());
    }

    private static void addButtonErrorClass(Button button) {
        if (!button.getStyleClass().contains(GuiLauncher.BUTTON_ERROR_CLASS)) {
            button.getStyleClass().add(GuiLauncher.BUTTON_ERROR_CLASS);
        }
    }

    private void showErrorMessage(String text) {
        clearResult();
        resultLabel.getStyleClass().add(GuiLauncher.LABEL_ERROR_CLASS);
        resultLabel.setText(text);
    }

    void showSuccessMessage() {
        clearResult();
        resultLabel.getStyleClass().add(GuiLauncher.LABEL_SUCCESS_CLASS);
        resultLabel.setText("Subtitles have been merged successfully!");
    }

    enum FileType {
        UPPER_SUBTITLES,
        LOWER_SUBTITLES,
        MERGED_SUBTITLES
    }
}
