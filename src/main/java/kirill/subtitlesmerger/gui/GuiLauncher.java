package kirill.subtitlesmerger.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kirill.subtitlesmerger.logic.Constants;
import kirill.subtitlesmerger.logic.data.Config;

public class GuiLauncher extends Application {
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

        stage.setMinWidth(mainPane.getMinWidth());
        stage.setMinHeight(mainPane.getMinHeight());
        stage.setResizable(true);
        stage.getIcons().add(new Image(GuiLauncher.class.getResourceAsStream("/icon.jpg")));
        stage.setTitle("Subtitles merger");
        Application.setUserAgentStylesheet(STYLESHEET_MODENA);

        stage.show();
    }

    private TabPane generateMainPane(Stage stage, Config config) {
        TabPane result = new TabPane();

        result.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        result.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);

        result.setPrefWidth(800);
        result.setMinWidth(result.getPrefWidth());
        result.setPrefHeight(480);
        result.setMinHeight(result.getPrefWidth());

        MergeFilesTab mergeFilesTab = new MergeFilesTab(stage, result, Constants.DEBUG);
        result.getTabs().add(mergeFilesTab.generateTab());

        MergeFilesTabInteractions mergeFilesTabInteractions = new MergeFilesTabInteractions(mergeFilesTab, config);
        mergeFilesTabInteractions.addCallbacks();

        MergeInVideosTab mergeInVideosTab = new MergeInVideosTab(result);
        result.getTabs().add(mergeInVideosTab.generateTab());

        return result;
    }
}
