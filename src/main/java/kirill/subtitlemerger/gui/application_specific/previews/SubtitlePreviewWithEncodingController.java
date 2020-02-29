package kirill.subtitlemerger.gui.application_specific.previews;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.utils.entities.NoSelectionModel;
import kirill.subtitlemerger.gui.utils.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.utils.entities.MultiPartResult;
import kirill.subtitlemerger.gui.utils.custom_controls.MultiColorLabels;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.core.SubtitleParser;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;
import java.util.*;

//todo refactor, make hierarchy maybe
public class SubtitlePreviewWithEncodingController {
    private static final CharsetStringConverter CHARSET_STRING_CONVERTER = new CharsetStringConverter();

    @FXML
    private Pane mainPane;

    @FXML
    private Label titleLabel;

    @FXML
    private ComboBox<Charset> encodingComboBox;

    @FXML
    private MultiColorLabels resultLabels;

    private BooleanProperty linesTruncated;

    @FXML
    private ListView<String> listView;

    @FXML
    private Button saveButton;

    @FXML
    private Pane progressPane;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label progressLabel;

    private byte[] data;

    private Charset originalEncoding;

    private Charset currentEncoding;

    private Subtitles originalSubtitles;

    private Subtitles currentSubtitles;

    private Stage dialogStage;

    @Getter
    private UserSelection userSelection;

    public SubtitlePreviewWithEncodingController() {
        linesTruncated = new SimpleBooleanProperty(false);
    }

    public boolean isLinesTruncated() {
        return linesTruncated.get();
    }

    public BooleanProperty linesTruncatedProperty() {
        return linesTruncated;
    }

    public void setLinesTruncated(boolean linesTruncated) {
        this.linesTruncated.set(linesTruncated);
    }

    public void initialize(
            byte[] data,
            Charset originalEncoding,
            String title,
            Stage dialogStage
    ) {
        this.data = data;
        this.originalEncoding = originalEncoding;
        currentEncoding = originalEncoding;
        this.dialogStage = dialogStage;

        titleLabel.setText(title);

        encodingComboBox.setConverter(CHARSET_STRING_CONVERTER);
        encodingComboBox.getItems().setAll(GuiConstants.SUPPORTED_ENCODINGS);
        encodingComboBox.getSelectionModel().select(originalEncoding);

        listView.setSelectionModel(new NoSelectionModel<>());

        processDataAndUpdateScene(true);

        userSelection = new UserSelection(currentSubtitles, currentEncoding);
    }

    private void processDataAndUpdateScene(boolean initialRun) {
        listView.getItems().clear();

        BackgroundTask<ProcessedData> task = new BackgroundTask<>() {
            @Override
            protected ProcessedData run() {
                return getProcessedData(data, currentEncoding);
            }

            @Override
            protected void onFinish(ProcessedData result) {
                currentSubtitles = result.getSubtitles();
                if (initialRun) {
                    originalSubtitles = currentSubtitles;
                }

                progressPane.setVisible(false);
                mainPane.setDisable(false);

                updateScene(result, initialRun);
            }
        };

        progressPane.setVisible(true);
        mainPane.setDisable(true);
        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());

        task.start();
    }

    private static ProcessedData getProcessedData(byte[] data, Charset encoding) {
        String text = new String(data, encoding);
        Subtitles subtitles;
        try {
            subtitles = SubtitleParser.fromSubRipText(text, null);
        } catch (SubtitleParser.IncorrectFormatException e) {
            subtitles = null;
        }

        List<String> lines = new ArrayList<>();
        boolean linesTruncated = false;
        for (String line : LogicConstants.LINE_SEPARATOR_PATTERN.split(text)) {
            if (line.length() > 1000) {
                lines.add(line.substring(0, 1000));
                linesTruncated = true;
            } else {
                lines.add(line);
            }
        }

        return new ProcessedData(subtitles, FXCollections.observableArrayList(lines), linesTruncated);
    }

    private void updateScene(ProcessedData processedData, boolean initialRun) {
        listView.setDisable(processedData.getSubtitles() == null);
        listView.setItems(processedData.getLinesToDisplay());

        setLinesTruncated(processedData.isLinesTruncated());

        MultiPartResult result = MultiPartResult.EMPTY;
        if (processedData.getSubtitles() == null) {
            result = MultiPartResult.onlyError(
                    String.format(
                            "This encoding (%s) doesn't fit or the file has an incorrect format",
                            currentEncoding.name()
                    )
            );
        } else {
            if (!initialRun) {
                if (Objects.equals(currentEncoding, originalEncoding)) {
                    result = MultiPartResult.onlySuccess(
                            "Encoding has been restored to the original value successfully"
                    );
                } else {
                    result = MultiPartResult.onlySuccess("Encoding has been changed successfully");
                }
            }
        }
        resultLabels.set(result);

        saveButton.setDisable(Objects.equals(currentEncoding, originalEncoding) || currentSubtitles == null);
    }

    @FXML
    private void encodingChanged() {
        Charset encoding = encodingComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(encoding, currentEncoding)) {
            return;
        }

        currentEncoding = encoding;
        processDataAndUpdateScene(false);
    }

    @FXML
    private void cancelButtonClicked() {
        userSelection = new UserSelection(originalSubtitles, originalEncoding);
        dialogStage.close();
    }

    @FXML
    private void saveButtonClicked() {
        userSelection = new UserSelection(currentSubtitles, currentEncoding);
        dialogStage.close();
    }

    private static class CharsetStringConverter extends StringConverter<Charset> {
        @Override
        public String toString(Charset charset) {
            return charset.name();
        }

        @Override
        public Charset fromString(String name) {
            return Charset.forName(name);
        }
    }

    @AllArgsConstructor
    @Getter
    private static class ProcessedData {
        private Subtitles subtitles;

        private ObservableList<String> linesToDisplay;

        private boolean linesTruncated;
    }

    @AllArgsConstructor
    @Getter
    public static class UserSelection {
        private Subtitles subtitles;

        private Charset encoding;
    }
}
