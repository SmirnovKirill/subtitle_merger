package kirill.subtitlemerger.gui.forms.common.subtitle_preview;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.background.BackgroundCallback;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;

public class ReadOnlyPreviewFormController extends AbstractPreviewFormController {
    @FXML
    private Pane upperSubtitlesPane;

    @FXML
    private Label upperSubtitlesTitleLabel;

    @FXML
    private Pane lowerSubtitlesPane;

    @FXML
    private Label lowerSubtitlesTitleLabel;

    public void initializeSimple(String title, String text, Stage dialogStage) {
        titleLabel.setText(title);
        GuiUtils.setVisibleAndManaged(upperSubtitlesPane, false);
        GuiUtils.setVisibleAndManaged(lowerSubtitlesPane, false);
        listView.setSelectionModel(new NoSelectionModel<>());
        this.dialogStage = dialogStage;

        displayText(text);
    }

    public void initializeMerged(
            String upperSubtitlesTitle,
            String lowerSubtitlesTitle,
            String text,
            Stage dialogStage
    ) {
        titleLabel.setText("This is the result of merging");
        upperSubtitlesTitleLabel.setText(upperSubtitlesTitle);
        lowerSubtitlesTitleLabel.setText(lowerSubtitlesTitle);
        listView.setSelectionModel(new NoSelectionModel<>());
        this.dialogStage = dialogStage;

        displayText(text);
    }

    private void displayText(String text) {
        BackgroundRunner<SplitText> backgroundRunner = backgroundManager -> {
            backgroundManager.setCancelPossible(false);
            backgroundManager.setIndeterminateProgress();
            backgroundManager.updateMessage("Preparing the text...");
            return getSplitText(text);
        };

        BackgroundCallback<SplitText> callback = splitText -> {
            setLinesTruncated(splitText.isLinesTruncated());
            listView.setItems(FXCollections.observableArrayList(splitText.getLines()));
        };

        runInBackground(backgroundRunner, callback);
    }

    @FXML
    private void okClicked() {
        dialogStage.close();
    }
}