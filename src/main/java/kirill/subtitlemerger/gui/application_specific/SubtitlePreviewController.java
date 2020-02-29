package kirill.subtitlemerger.gui.application_specific;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.background_tasks.BackgroundThreadProcessor;
import kirill.subtitlemerger.gui.utils.background_tasks.MainThreadProcessor;
import kirill.subtitlemerger.gui.utils.custom_controls.MultiColorLabels;
import kirill.subtitlemerger.gui.utils.entities.MultiPartResult;
import kirill.subtitlemerger.gui.utils.entities.NoSelectionModel;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.core.SubtitleParser;
import kirill.subtitlemerger.logic.core.SubtitleWriter;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SubtitlePreviewController extends AbstractController {
    private static final CharsetStringConverter CHARSET_STRING_CONVERTER = new CharsetStringConverter();

    @FXML
    private Label title;

    @FXML
    private Pane mergedUpperPane;

    @FXML
    private Label mergedUpperTitle;

    @FXML
    private Pane mergedLowerPane;

    @FXML
    private Label mergedLowerTitle;

    @FXML
    private Pane encodingPane;

    @FXML
    private ComboBox<Charset> encodingComboBox;

    @FXML
    private MultiColorLabels resultLabels;

    @FXML
    private ListView<String> listView;

    @FXML
    private Pane okPane;

    @FXML
    private Pane cancelSavePane;

    @FXML
    private Button saveButton;

    private BooleanProperty linesTruncated;

    private Mode mode;

    private byte[] data;

    private Charset originalEncoding;

    private Charset currentEncoding;

    private Subtitles originalSubtitles;

    private Subtitles currentSubtitles;

    private Stage dialogStage;

    private UserSelection userSelection;

    public SubtitlePreviewController() {
        linesTruncated = new SimpleBooleanProperty(false);
    }

    public void initializeSimple(
            Subtitles subtitles,
            String title,
            Stage dialogStage
    ) {
        mode = Mode.SIMPLE;
        this.originalSubtitles = subtitles;
        this.currentSubtitles = subtitles;
        this.dialogStage = dialogStage;

        this.title.setText(title);
        GuiUtils.setVisibleAndManaged(mergedUpperPane, false);
        GuiUtils.setVisibleAndManaged(mergedLowerPane, false);
        GuiUtils.setVisibleAndManaged(encodingPane, false);
        listView.setSelectionModel(new NoSelectionModel<>());
        GuiUtils.setVisibleAndManaged(cancelSavePane, false);

        processDataAndUpdateScene(true);
    }

    public void initializeMerged(
            Subtitles subtitles,
            String upperTitle,
            String lowerTitle,
            Stage dialogStage
    ) {
        mode = Mode.MERGED;
        this.originalSubtitles = subtitles;
        this.currentSubtitles = subtitles;
        this.dialogStage = dialogStage;

        title.setText("This is the result of merging");
        mergedUpperTitle.setText(upperTitle);
        mergedLowerTitle.setText(lowerTitle);
        GuiUtils.setVisibleAndManaged(encodingPane, false);
        listView.setSelectionModel(new NoSelectionModel<>());
        GuiUtils.setVisibleAndManaged(cancelSavePane, false);

        processDataAndUpdateScene(true);
    }

    public void initializeWithEncoding(
            byte[] data,
            Charset originalEncoding,
            String title,
            Stage dialogStage
    ) {
        mode = Mode.WITH_ENCODING;
        this.data = data;
        this.originalEncoding = originalEncoding;
        currentEncoding = originalEncoding;
        this.dialogStage = dialogStage;

        this.title.setText(title);
        GuiUtils.setVisibleAndManaged(mergedUpperPane, false);
        GuiUtils.setVisibleAndManaged(mergedLowerPane, false);
        encodingComboBox.setConverter(CHARSET_STRING_CONVERTER);
        encodingComboBox.getItems().setAll(GuiConstants.SUPPORTED_ENCODINGS);
        encodingComboBox.getSelectionModel().select(originalEncoding);
        listView.setSelectionModel(new NoSelectionModel<>());
        GuiUtils.setVisibleAndManaged(okPane, false);

        processDataAndUpdateScene(true);
    }

    private void processDataAndUpdateScene(boolean initialRun) {
        listView.getItems().clear();

        BackgroundThreadProcessor<ProcessedData> backgroundThreadProcessor = taskManager -> {
            if (mode == Mode.WITH_ENCODING) {
                return getProcessedData(data, currentEncoding);
            } else {
                return getProcessedData(originalSubtitles, null);
            }
        };

        MainThreadProcessor<ProcessedData> mainThreadProcessor = processedData -> {
            if (mode == Mode.WITH_ENCODING) {
                if (initialRun) {
                    originalSubtitles = currentSubtitles;
                    userSelection = new UserSelection(currentSubtitles, currentEncoding);
                }
                currentSubtitles = processedData.getSubtitles();
            }

            updateScene(processedData, initialRun);
        };

        startLongTask(backgroundThreadProcessor, mainThreadProcessor, false);
    }

    private static ProcessedData getProcessedData(byte[] data, Charset encoding) {
        String text = new String(data, encoding);
        Subtitles subtitles;
        try {
            subtitles = SubtitleParser.fromSubRipText(text, null);
        } catch (SubtitleParser.IncorrectFormatException e) {
            subtitles = null;
        }

        return getProcessedData(subtitles, text);
    }

    private static ProcessedData getProcessedData(Subtitles subtitles, String subtitleText) {
        if (subtitleText == null) {
            subtitleText = SubtitleWriter.toSubRipText(subtitles);
        }

        List<String> lines = new ArrayList<>();
        boolean linesTruncated = false;
        for (String line : LogicConstants.LINE_SEPARATOR_PATTERN.split(subtitleText)) {
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
        setLinesTruncated(processedData.isLinesTruncated());

        listView.setDisable(processedData.getSubtitles() == null);
        listView.setItems(processedData.getLinesToDisplay());

        if (mode == Mode.WITH_ENCODING) {
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

    public UserSelection getUserSelection() {
        if (mode != Mode.WITH_ENCODING) {
            throw new IllegalStateException();
        }

        return userSelection;
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
    private void okButtonClicked() {
        dialogStage.close();
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

    @AllArgsConstructor
    @Getter
    private static class ProcessedData {
        private Subtitles subtitles;

        private ObservableList<String> linesToDisplay;

        private boolean linesTruncated;
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
    public static class UserSelection {
        private Subtitles subtitles;

        private Charset encoding;
    }

    private enum Mode {
        SIMPLE,
        WITH_ENCODING,
        MERGED
    }
}