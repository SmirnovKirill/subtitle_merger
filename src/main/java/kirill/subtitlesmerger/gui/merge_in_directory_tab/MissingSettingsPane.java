package kirill.subtitlesmerger.gui.merge_in_directory_tab;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import kirill.subtitlesmerger.gui.GuiLauncher;
import lombok.Getter;

import java.util.List;

class MissingSettingsPane {
    private Pane labelsPane;

    private Hyperlink goToSettingsLink;

    @Getter
    private Pane missingSettingsPane;

    MissingSettingsPane() {
        this.labelsPane = generateLabelsPane();
        this.goToSettingsLink = generateGoToSettingsLink();
        this.missingSettingsPane = generatePane(labelsPane, generateGoToSettingsLink());
    }

    private static Pane generateLabelsPane() {
        VBox result = new VBox();

        result.setSpacing(10);

        return result;
    }

    private static Hyperlink generateGoToSettingsLink() {
        Hyperlink result = new Hyperlink();

        result.setText("open settings tab");

        return result;
    }

    private static Pane generatePane(Pane labelsPane, Hyperlink goToSettingsLink) {
        VBox result = new VBox();

        result.setPadding(GuiLauncher.TAB_PADDING);
        result.setSpacing(10);

        result.getChildren().addAll(labelsPane, goToSettingsLink);

        return result;
    }

    void setGoToSettingsLinkHandler(EventHandler<ActionEvent> handler) {
        goToSettingsLink.setOnAction(handler);
    }

    void hide() {
        this.missingSettingsPane.setVisible(false);
    }

    void show() {
        this.missingSettingsPane.setVisible(true);
    }

    void setMissingSettings(List<String> missingSettings) {
        labelsPane.getChildren().clear();

        labelsPane.getChildren().add(new Label("The following settings are missing:"));
        for (String setting : missingSettings) {
            labelsPane.getChildren().add(new Label("\u2022 " + setting));
        }
    }
}
