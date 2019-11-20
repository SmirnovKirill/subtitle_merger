package kirill.subtitlesmerger.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class GuiLauncher extends Application {
    private static final boolean DEBUG = true;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        TabPane mainPane = generateMainPane(stage);

        Scene scene = new Scene(mainPane);
        stage.setScene(scene);

        stage.setMinWidth(mainPane.getMinWidth());
        stage.setMinHeight(mainPane.getMinHeight());
        stage.setResizable(true);
        stage.getIcons().add(new Image(GuiLauncher.class.getResourceAsStream("/icon.jpg")));
        stage.setTitle("Subtitles merger");
        Application.setUserAgentStylesheet(STYLESHEET_MODENA);

        stage.show();
    }

    private TabPane generateMainPane(Stage stage) {
        TabPane result = new TabPane();

        result.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        result.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);

        result.setMinWidth(800);
        result.setPrefWidth(800);
        result.setMinHeight(480);
        result.setPrefHeight(480);

        MergeFilesTab mergeFilesTab = new MergeFilesTab(stage, result, DEBUG);
        result.getTabs().add(mergeFilesTab.generateTab());

        MergeInVideosTab mergeInVideosTab = new MergeInVideosTab(result);
        result.getTabs().add(mergeInVideosTab.generateTab());

        return result;
    }
}
