package kirill.subtitlemerger.gui.core.custom_controls;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.core.NoSelectionModel;
import kirill.subtitlemerger.gui.core.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.core.entities.MultiPartResult;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@CommonsLog
public class PreviewWithEncodingDialog extends StackPane {
    private static final CharsetStringConverter CHARSET_STRING_CONVERTER = new CharsetStringConverter();

    @FXML
    private Pane mainPane;

    @FXML
    private Label titleLabel;

    @FXML
    private ComboBox<Charset> encodingComboBox;

    @FXML
    private MultiColorResultLabels resultLabels;

    @FXML
    private ListView<String> listView;

    @FXML
    private Button cancelButton;

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

    private Subtitles originalSubtitles;

    private Charset currentEncoding;

    private Subtitles currentSubtitles;

    private Stage dialogStage;

    @Getter
    private UserSelection userSelection;

    public PreviewWithEncodingDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/gui/custom_controls/previewWithEncodingDialog.fxml")
        );
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }

        encodingComboBox.setConverter(CHARSET_STRING_CONVERTER);
        encodingComboBox.getItems().setAll(GuiConstants.SUPPORTED_ENCODINGS);
        listView.setSelectionModel(new NoSelectionModel<>());
    }

    public void initialize(
            byte[] data,
            Charset originalEncoding,
            Subtitles originalSubtitles,
            String title,
            Stage dialogStage
    ) {
        this.data = data;
        this.originalEncoding = originalEncoding;
        this.originalSubtitles = originalSubtitles;
        currentEncoding = originalEncoding;
        currentSubtitles = originalSubtitles;
        this.dialogStage = dialogStage;
        userSelection = new UserSelection(currentSubtitles, currentEncoding);

        titleLabel.setText(title);
        encodingComboBox.getSelectionModel().select(originalEncoding);
        showContent(true);
    }

    private void showContent(boolean initialRun) {
        listView.getItems().clear();

        BackgroundTask<SubtitlesAndLiesToDisplay> task = new BackgroundTask<>() {
            @Override
            protected SubtitlesAndLiesToDisplay run() {
                return getSubtitlesAndLinesToDisplay(data, currentEncoding);
            }

            @Override
            protected void onFinish(SubtitlesAndLiesToDisplay result) {
                String success = null;
                String error = null;
                String warn = null;
                if (result.isLinesTruncated()) {
                    warn = "lines that are longer than 1000 symbols were truncated";
                }

                if (result.getSubtitles() == null) {
                    listView.setDisable(true);
                    listView.setItems(
                            FXCollections.observableArrayList(
                                    Collections.singletonList("Unfortunately, preview is unavailable")
                            )
                    );

                    error = String.format(
                            "This encoding (%s) doesn't fit or the file has an incorrect format",
                            currentEncoding.name()
                    );
                } else {
                    listView.setDisable(false);
                    listView.setItems(result.getLinesToDisplay());

                    if (!initialRun) {
                        if (Objects.equals(currentEncoding, originalEncoding)) {
                            success = "Encoding has been restored to the original value successfully";
                        } else {
                            success = "Encoding has been changed successfully";
                        }
                    }
                }

                resultLabels.update(new MultiPartResult(success, warn, error));

                progressPane.setVisible(false);
                mainPane.setDisable(false);
            }
        };

        progressPane.setVisible(true);
        mainPane.setDisable(true);
        progressIndicator.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());

        task.start();
    }

    private static SubtitlesAndLiesToDisplay getSubtitlesAndLinesToDisplay(
            byte[] data, Charset encoding
    ) {
        String text = new String(data, encoding);
        try {
            Subtitles subtitles = Parser.fromSubRipText(text, null);

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

            return new SubtitlesAndLiesToDisplay(subtitles, FXCollections.observableArrayList(lines), linesTruncated);
        } catch (Parser.IncorrectFormatException e) {
            return new SubtitlesAndLiesToDisplay(null, FXCollections.emptyObservableList(), false);
        }
    }

    @FXML
    private void encodingChanged() {
        Charset encoding = encodingComboBox.getSelectionModel().getSelectedItem();

        if (Objects.equals(encoding, currentEncoding)) {
            return;
        }

        currentEncoding = encoding;
        showContent(false);
        saveButton.setDisable(Objects.equals(currentEncoding, originalEncoding));
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
    private static class SubtitlesAndLiesToDisplay {
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
