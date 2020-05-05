package kirill.subtitlemerger.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.forms.MainFormController;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.gui.utils.entities.FormInfo;

import java.awt.*;

public class GuiLauncher extends Application {
    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(STYLESHEET_MODENA);

        FormInfo mainFormInfo = GuiUtils.loadForm("/gui/javafx/forms/main_form.fxml");

        MainFormController controller = mainFormInfo.getController();
        controller.initialize(stage, new GuiContext());

        stage.getIcons().add(new Image(GuiLauncher.class.getResourceAsStream("/gui/icons/icon.png")));
        stage.setTitle("Subtitle Merger");
        stage.setResizable(true);
        Scene scene = new Scene(mainFormInfo.getRootNode());
        scene.getStylesheets().add("/gui/javafx/main.css");
        stage.setScene(scene);

        stage.show();
        closeSplashScreen();

        /*
         * I've encountered a very strange behaviour - at first the stage's width and height are set to their computed
         * sizes but very soon after this method (start) is called during the startup window the sizes are changed for a
         * reason completely unknown to me. The sizes are changed because the com.sun.glass.ui.Window::notifyResize is
         * called. This method is called from a native method com.sun.glass.ui.gtk.GtkApplication::_runLoop so I can't
         * understand why that happens. Anyway, I've discovered that if I set the stage's width and height explicitly
         * these original sizes will be restored after these weird changes. And it reproduces only in Kubuntu for me, in
         * Windows 10 and Arch everything works fine.
         */
        stage.setWidth(stage.getWidth());
        stage.setHeight(stage.getHeight());

        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
    }

    private void closeSplashScreen() {
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null && splash.isVisible()) {
            splash.close();
        }
    }
}
