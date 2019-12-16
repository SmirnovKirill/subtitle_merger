package kirill.subtitlesmerger.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kirill.subtitlesmerger.logic.AppContext;
import lombok.extern.apachecommons.CommonsLog;

import java.io.IOException;

@CommonsLog
public class GuiLauncher extends Application {
    public static final String BUTTON_ERROR_CLASS = "button-error";

    public static final String LABEL_SUCCESS_CLASS = "label-success";

    public static final String LABEL_ERROR_CLASS = "label-error";

    public static final String TABLE_CELL_CLASS = "file-table-cell";

    public static final String FIRST_TABLE_CELL_CLASS = "first-file-table-cell";

    public static final String LOWEST_TABLE_CELL_CLASS = "lowest-file-table-cell";

    public static final String FIRST_LOWEST_TABLE_CELL_CLASS = "first-lowest-file-table-cell";

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        AppContext appContext = new AppContext();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.load();

        MainController mainController = loader.getController();
        mainController.init(stage, appContext);

        Scene scene = new Scene(loader.getRoot());
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
}
