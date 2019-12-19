package kirill.subtitlesmerger.gui;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.tabs.TabPaneController;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;

@CommonsLog
public class GuiLauncher extends Application {
    private static long start = System.currentTimeMillis();

    //todo refactor and optimize
    public static void main(String[] args) {
        System.out.println("start");
        launch();
    }

    @Override
    public void start(Stage stage) {
        Task<TabPane> loadTask = new Task<>() {
            @Override
            public TabPane call() throws IOException {
                long start = System.currentTimeMillis();
                GuiContext guiContext = new GuiContext();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/tabs/tabPane.fxml"));
                loader.load();
                TabPaneController tabPaneController = loader.getController();
                tabPaneController.initialize(stage, guiContext);
                TabPane result = loader.getRoot();

                System.out.println("concurrent part took " + (System.currentTimeMillis() - start) + " ms");

                return result;
            }
        };

        loadTask.setOnSucceeded(e -> {
            long start = System.currentTimeMillis();
            stage.getScene().setRoot(loadTask.getValue());
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
            System.out.println("on succeed took " + (System.currentTimeMillis() - start) + " ms");
        });

        loadTask.setOnFailed(e -> log.error("failed to load fxml: " + ExceptionUtils.getStackTrace(e.getSource().getException())));

        Scene scene = new Scene(generatePaneWithProgress());
        scene.getStylesheets().add("/gui/style.css");
        stage.setScene(scene);

        stage.setResizable(true);
        stage.getIcons().add(new Image(GuiLauncher.class.getResourceAsStream("/gui/icons/icon.png")));
        stage.setTitle("Subtitle merger");
        Application.setUserAgentStylesheet(STYLESHEET_MODENA);

        stage.show();

        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());

        Thread thread = new Thread(loadTask);
        thread.start();

        System.out.println("total time " + (System.currentTimeMillis() - start) + " ms");
    }

    private static Pane generatePaneWithProgress() {
        BorderPane result = new BorderPane();

        result.setMinWidth(GuiConstants.MIN_ROOT_PANE_WIDTH);
        result.setMinHeight(GuiConstants.MIN_ROOT_PANE_HEIGHT);

        result.setCenter(new ProgressIndicator());

        return result;
    }
}
