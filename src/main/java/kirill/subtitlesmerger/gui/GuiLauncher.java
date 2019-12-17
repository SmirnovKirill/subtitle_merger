package kirill.subtitlesmerger.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.tabs.TabController;
import lombok.extern.apachecommons.CommonsLog;

import java.io.IOException;

@CommonsLog
public class GuiLauncher extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        GuiContext guiContext = new GuiContext();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/tabs/tabs.fxml"));
        loader.load();

        TabController tabController = loader.getController();
        tabController.initialize(stage, guiContext);

        Scene scene = new Scene(loader.getRoot());
        scene.getStylesheets().add("/gui/style.css");
        stage.setScene(scene);

        stage.setResizable(true);
        stage.getIcons().add(new Image(GuiLauncher.class.getResourceAsStream("/gui/icons/icon.png")));
        stage.setTitle("Subtitles merger");
        Application.setUserAgentStylesheet(STYLESHEET_MODENA);

        stage.show();

        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
    }
}
