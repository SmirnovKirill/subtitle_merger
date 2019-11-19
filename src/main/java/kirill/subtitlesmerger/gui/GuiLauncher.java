package kirill.subtitlesmerger.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class GuiLauncher extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        TabPane mainPane = new TabPane();

        mainPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);

        mainPane.setMinWidth(640);
        mainPane.setPrefWidth(640);
        mainPane.setMinHeight(480);
        mainPane.setPrefHeight(480);

        MergeFilesTab mergeFilesTab = new MergeFilesTab(mainPane);
        mainPane.getTabs().add(mergeFilesTab.generateTab());

        MergeInVideosTab mergeInVideosTab = new MergeInVideosTab(mainPane);
        mainPane.getTabs().add(mergeInVideosTab.generateTab());

        Scene scene = new Scene(mainPane);

        stage.setMinWidth(mainPane.getMinWidth());
        stage.setMinHeight(mainPane.getMinHeight());
        stage.setResizable(true);
        stage.setScene(scene);
        stage.setTitle("Subtitles merger");
        stage.show();
    }
}
