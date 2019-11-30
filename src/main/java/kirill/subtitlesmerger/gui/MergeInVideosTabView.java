package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

class MergeInVideosTabView implements TabView {
    private static final String TAB_NAME = "Merge subtitles in videos";

    private boolean debug;

    private Tab tab;

    private VBox missingSettingsBox;

    private VBox missingSettingLabelsBox;

    private Hyperlink goToSettingsLink;

    MergeInVideosTabView(boolean debug) {
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

        contentPane.setHgap(55);
        contentPane.setPadding(GuiLauncher.TAB_PADDING);
        contentPane.setGridLinesVisible(debug);

        contentPane.getColumnConstraints().addAll(generateColumnConstraints());

        addRowMissingSettings(contentPane);

        return contentPane;
    }

    private static List<ColumnConstraints> generateColumnConstraints() {
        List<ColumnConstraints> result = new ArrayList<>();

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setHgrow(Priority.ALWAYS);
        result.add(firstColumn);

        return result;
    }

    private void addRowMissingSettings(GridPane contentPane) {
        missingSettingsBox = new VBox();
        missingSettingsBox.setPadding(GuiLauncher.TAB_PADDING);
        missingSettingsBox.setSpacing(10);

        missingSettingLabelsBox = new VBox();
        missingSettingLabelsBox.setSpacing(10);

        goToSettingsLink = new Hyperlink();
        goToSettingsLink.setText("open settings tab");

        missingSettingsBox.getChildren().addAll(missingSettingLabelsBox, goToSettingsLink);

        contentPane.addRow(contentPane.getRowCount(), missingSettingsBox);
        GridPane.setColumnSpan(missingSettingsBox, contentPane.getColumnCount());
    }

    @Override
    public String getTabName() {
        return TAB_NAME;
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    void setGoToSettingsLinkHandler(EventHandler<ActionEvent> handler) {
        goToSettingsLink.setOnAction(handler);
    }

    void showMissingSettings(
            List<String> missingSettings
    ) {
        this.missingSettingsBox.setVisible(true);
        this.missingSettingLabelsBox.getChildren().clear();

        this.missingSettingLabelsBox.getChildren().add(new Label("The following settings are missing:"));
        for (String setting : missingSettings) {
            this.missingSettingLabelsBox.getChildren().add(new Label("\u2022 " + setting));
        }
    }

    void showRegularContent() {
        this.missingSettingsBox.setVisible(false);
    }
}
