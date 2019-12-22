package kirill.subtitlesmerger.gui.tabs.merge_in_directory.regular_content.table_with_files;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiConstants;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlesmerger.logic.work_with_files.entities.SubtitleStreamInfo;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubtitleStreamChooserCell {
    private FileInfo fileInfo;

    private boolean lowestRow;

    private SubtitleType subtitleType;

    private Stage stage;

    private GuiContext guiContext;

    private ToggleGroup toggleGroup;

    private List<RadioButton> streamRadioButtons;

    private RadioButton fileRadioButton;

    private Button fileChooseButton;

    private FileChooser fileChooser;

    private File chosenFile;

    private Button discardFileButton;

    @Getter
    private Pane pane;

    public SubtitleStreamChooserCell(
            FileInfo fileInfo,
            boolean lowestRow,
            SubtitleType subtitleType,
            Stage stage,
            GuiContext guiContext
    ) {
        this.fileInfo = fileInfo;
        this.lowestRow = lowestRow;
        this.subtitleType = subtitleType;
        this.stage = stage;
        this.guiContext = guiContext;

        if (fileInfo.getUnavailabilityReason() != null) {
            return;
        }

        this.toggleGroup = new ToggleGroup();
        this.streamRadioButtons = generateStreamRadioButtons(fileInfo, toggleGroup);
        this.fileRadioButton = generateFileRadioButton(toggleGroup);
        this.fileChooseButton = generateFileChooseButton();
        this.fileChooser = generateFileChooser(subtitleType);

        //this.pane = generatePane();
    }

    private static List<RadioButton> generateStreamRadioButtons(
            FileInfo fileInfo,
            ToggleGroup toggleGroup
    ) {
        List<RadioButton> result = new ArrayList<>();

        for (SubtitleStreamInfo stream : fileInfo.getSubtitleStreamsInfo()) {
            RadioButton radioButton = new RadioButton(getRadioButtonText(stream));

            radioButton.setToggleGroup(toggleGroup);

            result.add(radioButton);
        }

        return result;
    }

    private static String getRadioButtonText(SubtitleStreamInfo stream) {
        StringBuilder result = new StringBuilder();

        if (stream.getLanguage() != null) {
            result.append(stream.getLanguage());
        } else {
            result.append("unknown");
        }

        if (!StringUtils.isBlank(stream.getTitle())) {
            result.append(" ").append(stream.getTitle());
        }

        return result.toString();
    }

    private static RadioButton generateFileRadioButton(ToggleGroup toggleGroup) {
        RadioButton result = new RadioButton("from file");

        result.setToggleGroup(toggleGroup);

        return result;
    }

    private static Button generateFileChooseButton() {
        Button result = new Button("Choose file");

        return result;
    }

    private void fileChooseButtonClicked(ActionEvent event) {
        chosenFile = fileChooser.showOpenDialog(stage);
        if (chosenFile == null) {
            return;
        }

       /* redrawAfterFileChosen(MergeSingleFilesTabView.FileType.UPPER_SUBTITLES);

        saveLastDirectoryInConfigIfNecessary(MergeSingleFilesTabView.FileType.UPPER_SUBTITLES);
        updateFileChoosers();*/
    }

    private static FileChooser generateFileChooser(SubtitleType subtitleType) {
        FileChooser result = new FileChooser();

        if (subtitleType == SubtitleType.UPPER) {
            result.setTitle("Please choose the file with the upper subtitles");
        } else if (subtitleType == SubtitleType.LOWER) {
            result.setTitle("Please choose the file with the lower subtitles");
        } else {
            throw new IllegalStateException();
        }

        result.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("subrip files (*.srt)", "*.srt")
        );

        return result;
    }

    Pane generatePane(boolean lowestRow) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(10);

        result.getStyleClass().add(GuiConstants.TABLE_CELL_CLASS);
        if (lowestRow) {
            result.getStyleClass().add(GuiConstants.LOWEST_TABLE_CELL_CLASS);
        }

        if (!CollectionUtils.isEmpty(streamRadioButtons)) {
            result.getChildren().addAll(streamRadioButtons);
        }

        if (fileRadioButton != null) {
            HBox hboxFromFile = new HBox();
            hboxFromFile.setSpacing(10);
            hboxFromFile.setAlignment(Pos.CENTER_LEFT);
            hboxFromFile.getChildren().addAll(
                    fileRadioButton,
                    fileChooseButton
            );

            result.getChildren().add(hboxFromFile);
        }

        return result;
    }

    public Optional<File> getChosenFile() {
        //todo implement
        return Optional.empty();
    }

    public Optional<SubtitleStreamInfo> getChosenSubtitleStream() {
        //todo implement
        return Optional.empty();
    }

    public enum SubtitleType {
        UPPER,
        LOWER
    }
}
