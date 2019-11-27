package kirill.subtitlesmerger.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kirill.subtitlesmerger.logic.Constants;
import kirill.subtitlesmerger.logic.data.Config;

public class GuiLauncher extends Application {
    static final String BUTTON_ERROR_CLASS = "button-error";

    static final String LABEL_SUCCESS_CLASS = "label-success";

    static final String LABEL_ERROR_CLASS = "label-error";

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        Config config = new Config();

        TabPane mainPane = generateMainPane(stage, config);

        Scene scene = new Scene(mainPane);
        scene.getStylesheets().add("style.css");
        stage.setScene(scene);

        stage.setResizable(true);
        stage.getIcons().add(new Image(GuiLauncher.class.getResourceAsStream("/icon.png")));
        stage.setTitle("Subtitles merger");
        Application.setUserAgentStylesheet(STYLESHEET_MODENA);

        stage.show();

        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
    }

    private TabPane generateMainPane(Stage stage, Config config) {
        TabPane result = new TabPane();

        result.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        result.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);

        result.setPrefWidth(800);
        result.setMinWidth(result.getPrefWidth());
        result.setPrefHeight(480);
        result.setMinHeight(result.getPrefHeight());

        MergeFilesTab mergeFilesTab = new MergeFilesTab(stage, Constants.DEBUG);
        result.getTabs().add(mergeFilesTab.generateTab());

        MergeFilesTabController mergeFilesTabController = new MergeFilesTabController(mergeFilesTab, config);
        mergeFilesTabController.initialize();

        MergeInVideosTab mergeInVideosTab = new MergeInVideosTab(stage, Constants.DEBUG);
        result.getTabs().add(mergeInVideosTab.generateTab());

        MergeInVideosTabController mergeInVideosTabController = new MergeInVideosTabController(
                mergeInVideosTab,
                config
        );
        mergeInVideosTabController.initialize();

        SettingsTab settingsTab = new SettingsTab(stage, Constants.DEBUG);
        result.getTabs().add(settingsTab.generateTab());

        SettingsTabController settingsTabController = new SettingsTabController(settingsTab, config);
        settingsTabController.initialize();

        return result;
    }
}
