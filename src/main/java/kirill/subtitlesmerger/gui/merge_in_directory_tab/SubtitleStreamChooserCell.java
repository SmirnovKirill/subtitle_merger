package kirill.subtitlesmerger.gui.merge_in_directory_tab;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiLauncher;
import kirill.subtitlesmerger.logic.AppContext;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlesmerger.logic.work_with_files.entities.SubtitleStream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SubtitleStreamChooserCell {
    private FileInfo fileInfo;

    private SubtitleType subtitleType;

    private Stage stage;

    private AppContext appContext;

    private ToggleGroup toggleGroup;

    private List<RadioButton> streamRadioButtons;

    private RadioButton fileRadioButton;

    private Button fileChooseButton;

    private FileChooser fileChooser;

    private File chosenFile;

    private Button discardFileButton;

    public SubtitleStreamChooserCell(FileInfo fileInfo, SubtitleType subtitleType, Stage stage, AppContext appContext) {
        this.fileInfo = fileInfo;
        this.subtitleType = subtitleType;
        this.stage = stage;
        this.appContext = appContext;

        if (fileInfo.getUnavailabilityReason() != null) {
            return;
        }

        this.toggleGroup = new ToggleGroup();
        this.streamRadioButtons = generateStreamRadioButtons(fileInfo, toggleGroup);
        this.fileRadioButton = generateFileRadioButton(toggleGroup);
        this.fileChooseButton = new Button("Choose file");
        this.fileChooser = generateFileChooser(subtitleType);
    }

    private static List<RadioButton> generateStreamRadioButtons(
            FileInfo fileInfo,
            ToggleGroup toggleGroup
    ) {
        List<RadioButton> result = new ArrayList<>();

        for (SubtitleStream stream : fileInfo.getSubtitleStreams()) {
            RadioButton radioButton = new RadioButton(getRadioButtonText(stream));

            radioButton.setToggleGroup(toggleGroup);

            result.add(radioButton);
        }

        return result;
    }

    private static String getRadioButtonText(SubtitleStream stream) {
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

    public Pane generatePane(boolean lowestRow) {
        VBox result = new VBox();

        result.setAlignment(Pos.CENTER_LEFT);
        result.setSpacing(10);

        result.getStyleClass().add(GuiLauncher.TABLE_CELL_CLASS);
        if (lowestRow) {
            result.getStyleClass().add(GuiLauncher.LOWEST_TABLE_CELL_CLASS);
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

    public enum SubtitleType {
        UPPER,
        LOWER
    }
}
