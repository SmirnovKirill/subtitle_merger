package kirill.subtitlesmerger.gui;

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.merge_in_directory_tab.MergeInDirectoryTabController;
import kirill.subtitlesmerger.gui.merge_in_directory_tab.MergeInDirectoryTabView;
import kirill.subtitlesmerger.gui.settings_tab.SettingsTabController;
import kirill.subtitlesmerger.gui.settings_tab.SettingsTabView;
import kirill.subtitlesmerger.logic.AppContext;
import kirill.subtitlesmerger.logic.Constants;
import lombok.extern.apachecommons.CommonsLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CommonsLog
public class GuiLauncher extends Application {
    public static final String BUTTON_ERROR_CLASS = "button-error";

    public static final String LABEL_SUCCESS_CLASS = "label-success";

    public static final String LABEL_ERROR_CLASS = "label-error";

    public static final Insets TAB_PADDING = new Insets(20);

    public static final String TABLE_CELL_CLASS = "file-table-cell";

    public static final String FIRST_TABLE_CELL_CLASS = "first-file-table-cell";

    public static final String LOWEST_TABLE_CELL_CLASS = "lowest-file-table-cell";

    public static final String FIRST_LOWEST_TABLE_CELL_CLASS = "first-lowest-file-table-cell";

    private TabPane mainPane;

    private List<TabController> tabControllers;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.tabControllers = new ArrayList<>();

        AppContext appContext = new AppContext();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.load();

        MainController mainController = loader.getController();
        mainController.init(stage, appContext);
        mainPane = loader.getRoot();

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

    private void addMergeInVideosTabViewAndController(TabPane mainPane, Stage stage, AppContext appContext) {
        MergeInDirectoryTabView tab = new MergeInDirectoryTabView(stage, Constants.DEBUG);
        mainPane.getTabs().add(tab.getTab());

        MergeInDirectoryTabController mergeInDirectoryTabController = new MergeInDirectoryTabController(
                stage,
                tab,
                appContext,
                this
        );
        mergeInDirectoryTabController.initialize();

        tabControllers.add(mergeInDirectoryTabController);
    }

    private void addSettingsTabViewAndController(TabPane mainPane, Stage stage, AppContext appContext) {
        SettingsTabView tab = new SettingsTabView(stage, Constants.DEBUG);
        mainPane.getTabs().add(tab.getTab());

        SettingsTabController settingsTabController = new SettingsTabController(tab, appContext);
        settingsTabController.initialize();

        tabControllers.add(settingsTabController);
    }

    private void tabChangedListener(
            ObservableValue<? extends Tab> observableValue,
            Tab oldTab,
            Tab newTab
    ) {
        for (TabController controller : tabControllers) {
            if (Objects.equals(controller.getTabView().getTabName(), newTab.getText())) {
                controller.tabClicked();
                return;
            }
        }

        log.error("unknown tab " + newTab.getText());
        throw new IllegalStateException();
    }

    public void openSettingsTab() {
        for (TabController controller : tabControllers) {
            if (Objects.equals(controller.getTabView().getTabName(), SettingsTabView.TAB_NAME)) {
                mainPane.getSelectionModel().select(controller.getTabView().getTab());
                return;
            }
        }

        log.error("failed to find settings tab");
        throw new IllegalStateException();
    }
}
