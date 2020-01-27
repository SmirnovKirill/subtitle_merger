package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import kirill.subtitlemerger.gui.GuiUtils;

class SubtitlesCell extends GridPane {
    @FXML
    private Label hiddenLabel;

    @FXML
    private Hyperlink showAllLink;

    @FXML
    private Hyperlink getAllSizesLink;

    SubtitlesCell(
            GuiFileInfo fileInfo,
            TableWithFiles.AllFileSubtitleSizesLoader allSizesLoader,
            TableWithFiles.SingleFileSubtitleSizeLoader singleSizeLoader
    ) {
        String fxmlPath = "/gui/tabs/videos/regular_content/table_with_files/subtitlesCell.fxml";
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        GuiUtils.loadWithUncheckedException(fxmlLoader);

        StringBinding hiddenBinding = Bindings.createStringBinding(
                () -> fileInfo.getSubtitleToHideCount() + " hidden ", fileInfo.subtitleToHideCountProperty()
        );

        hiddenLabel.visibleProperty().bind(fileInfo.subtitleToHideCountProperty().greaterThan(0));
        hiddenLabel.managedProperty().bind(fileInfo.subtitleToHideCountProperty().greaterThan(0));
        hiddenLabel.textProperty().bind(
                Bindings.when(fileInfo.someSubtitlesHiddenProperty())
                        .then(hiddenBinding)
                        .otherwise("")
        );

        showAllLink.visibleProperty().bind(fileInfo.subtitleToHideCountProperty().greaterThan(0));
        showAllLink.managedProperty().bind(fileInfo.subtitleToHideCountProperty().greaterThan(0));
        showAllLink.textProperty().bind(
                Bindings.when(fileInfo.someSubtitlesHiddenProperty())
                        .then("show all")
                        .otherwise("hide extra subtitles")
        );
        showAllLink.setOnAction(event -> fileInfo.setSomeSubtitlesHidden(!fileInfo.isSomeSubtitlesHidden()));

        getAllSizesLink.setOnAction(event -> allSizesLoader.load(fileInfo));
        getAllSizesLink.visibleProperty().bind(fileInfo.haveSubtitleSizesToLoadProperty());
        getAllSizesLink.managedProperty().bind(fileInfo.haveSubtitleSizesToLoadProperty());
    }
}
