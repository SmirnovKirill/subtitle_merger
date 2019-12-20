package kirill.subtitlesmerger.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.tabs.TabPaneController;
import lombok.extern.apachecommons.CommonsLog;

import java.awt.*;
import java.io.IOException;

@CommonsLog
public class GuiLauncher extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        GuiContext guiContext = new GuiContext();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/tabs/tabPane.fxml"));

        TabPane tabPane = loader.load();
        TabPaneController tabPaneController = loader.getController();
        tabPaneController.initialize(stage, guiContext);

        Scene scene = new Scene(tabPane);
        scene.getStylesheets().add("/gui/style.css");
        stage.setScene(scene);

        stage.setResizable(true);
        stage.getIcons().add(new Image(GuiLauncher.class.getResourceAsStream("/gui/icons/icon.png")));
        stage.setTitle("Subtitles merger");
        Application.setUserAgentStylesheet(STYLESHEET_MODENA);

        stage.show();

        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null && splash.isVisible()) {
            splash.close();
        }

        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
    }
}
