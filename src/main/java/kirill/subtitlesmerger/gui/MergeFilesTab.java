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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
class MergeFilesTab {
    static final String BUTTON_ERROR_CLASS = "button-error";

    private Stage stage;

    private TabPane mainPane;

    private boolean debug;

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

    private Label combinedErrorsLabel;

    private Label successLabel;

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
        addRowForMergedSubtitlesFile(contentPane);
        addMergeButton(contentPane);
        addErrorAndSuccessLabels(contentPane);
        addSpacer(contentPane);

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
                new FileChooser.ExtensionFilter("subrip files (*.srt)", "*.srt")
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

    private void addRowForMergedSubtitlesFile(GridPane contentPane) {
        Label descriptionLabel = new Label("Please choose where to save the result");

        mergedSubtitlesFileChooseButton = new Button("Choose file");
        mergedSubtitlesPathLabel = new Label("not selected");
        mergedSubtitlesFileChooser = getFileChooser("Please choose where to save the result");

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

    private void addErrorAndSuccessLabels(GridPane contentPane) {
        successLabel = new Label();
        successLabel.getStyleClass().add("label-success");
        contentPane.addRow(contentPane.getRowCount(), successLabel);
        GridPane.setColumnSpan(successLabel, contentPane.getColumnCount());

        combinedErrorsLabel = new Label();
        combinedErrorsLabel.setWrapText(true);
        combinedErrorsLabel.getStyleClass().add("label-error");
        contentPane.addRow(contentPane.getRowCount(), combinedErrorsLabel);
        GridPane.setColumnSpan(combinedErrorsLabel, contentPane.getColumnCount());
    }

    private void addSpacer(GridPane contentPane) {
        Region bottomSpacer = new Region();
        contentPane.addRow(contentPane.getRowCount(), bottomSpacer);
        GridPane.setColumnSpan(bottomSpacer, contentPane.getColumnCount());
        GridPane.setVgrow(bottomSpacer, Priority.ALWAYS);
    }

    void clearPreviousResults() {
        updateErrors(null, null, null);
        clearSuccessMessage();
    }

    void updateErrors(
            String upperSubtitlesFileErrorMessage,
            String lowerSubtitlesFileErrorMessage,
            String mergedSubtitlesFileErrorMessage
    ) {
        updateButtonErrorClass(upperSubtitlesFileChooseButton, !StringUtils.isBlank(upperSubtitlesFileErrorMessage));
        updateButtonErrorClass(lowerSubtitlesFileChooseButton, !StringUtils.isBlank(lowerSubtitlesFileErrorMessage));
        updateButtonErrorClass(mergedSubtitlesFileChooseButton, !StringUtils.isBlank(mergedSubtitlesFileErrorMessage));

        boolean atLeastOneError = !StringUtils.isBlank(upperSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(lowerSubtitlesFileErrorMessage)
                || !StringUtils.isBlank(mergedSubtitlesFileErrorMessage);
        if (!atLeastOneError) {
            clearCombinedErrorsMessage();
            return;
        }

        StringBuilder combinedErrorsMessage = new StringBuilder("Can't merge subtitles:");

        int errorNumber = 1;
        if (!StringUtils.isBlank(upperSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\n").append(errorNumber).append(") ").append(upperSubtitlesFileErrorMessage);
            errorNumber++;
        }
        if (!StringUtils.isBlank(lowerSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\n").append(errorNumber).append(") ").append(lowerSubtitlesFileErrorMessage);
            errorNumber++;
        }
        if (!StringUtils.isBlank(mergedSubtitlesFileErrorMessage)) {
            combinedErrorsMessage.append("\n").append(errorNumber).append(") ").append(mergedSubtitlesFileErrorMessage);
        }

        showCombinedErrorsMessage(combinedErrorsMessage.toString());
    }

    private static void updateButtonErrorClass(Button button, boolean hasErrors) {
        if (hasErrors && !button.getStyleClass().contains(MergeFilesTab.BUTTON_ERROR_CLASS)) {
            button.getStyleClass().add(MergeFilesTab.BUTTON_ERROR_CLASS);
        } else if (!hasErrors) {
            button.getStyleClass().remove(MergeFilesTab.BUTTON_ERROR_CLASS);
        }
    }

    private void clearCombinedErrorsMessage() {
        combinedErrorsLabel.setText("");
    }

    private void showCombinedErrorsMessage(String text) {
        combinedErrorsLabel.setText(text);
    }

    private void clearSuccessMessage() {
        successLabel.setText("");
    }

    void showSuccessMessage() {
        successLabel.setText("Subtitles have been merged successfully!");
    }

    enum FileType {
        UPPER_SUBTITLES,
        LOWER_SUBTITLES,
        MERGED_SUBTITLES
    }
}
