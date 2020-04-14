package kirill.subtitlemerger.gui;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.application_specific.MainPaneController;
import kirill.subtitlemerger.gui.util.GuiUtils;
import kirill.subtitlemerger.gui.util.entities.NodeAndController;
import lombok.extern.apachecommons.CommonsLog;

import java.awt.*;

@CommonsLog
public class GuiLauncher extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(STYLESHEET_MODENA);

        NodeAndController nodeAndController = GuiUtils.loadNodeAndController(
                "/gui/application_specific/mainPane.fxml"
        );

        MainPaneController controller = nodeAndController.getController();
        controller.initialize(stage, new GuiContext());

        stage.getIcons().add(new Image(GuiLauncher.class.getResourceAsStream("/gui/icons/icon.png")));
        stage.setTitle("Subtitle merger");
        stage.setResizable(true);
        stage.setScene(generateScene(nodeAndController.getNode()));

        stage.show();
        closeSplashScreen();

        /*
         * I've encountered a very strange behaviour - at first stage's width and height are set to their computed sizes
         * but very soon after this method (start) is called during the startup window sizes are changed for a reason
         * completely unknown to me. Sizes are changed because com.sun.glass.ui.Window::notifyResize is called. This
         * method is called from a native method com.sun.glass.ui.gtk.GtkApplication::_runLoop so I can't understand why
         * that happens. Anyway, I've discovered that if I set stage's width and height explicitly these original sizes
         * will be restored after these weird changes. And it reproduces only in Kubuntu for me, in Windows 10
         * and Arch everything works fine.
         */
        stage.setWidth(stage.getWidth());
        stage.setHeight(stage.getHeight());

        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
    }

    private static Scene generateScene(Parent node) {
        Scene result = new Scene(node);

        result.getStylesheets().add("/gui/style.css");

        return result;
    }

    private void closeSplashScreen() {
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null && splash.isVisible()) {
            splash.close();
        }
    }
}
