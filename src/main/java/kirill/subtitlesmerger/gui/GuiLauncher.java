package kirill.subtitlesmerger.gui;

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.merge_files_tab.MergeFilesTabController;
import kirill.subtitlesmerger.gui.merge_files_tab.MergeFilesTabView;
import kirill.subtitlesmerger.gui.merge_in_videos_tab.MergeInVideosTabController;
import kirill.subtitlesmerger.gui.merge_in_videos_tab.MergeInVideosTabView;
import kirill.subtitlesmerger.gui.settings_tab.SettingsTabController;
import kirill.subtitlesmerger.gui.settings_tab.SettingsTabView;
import kirill.subtitlesmerger.logic.Constants;
import kirill.subtitlesmerger.logic.data.Config;
import lombok.extern.apachecommons.CommonsLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CommonsLog
public class GuiLauncher extends Application {
    public static final String BUTTON_ERROR_CLASS = "button-error";

    public static final String LABEL_SUCCESS_CLASS = "label-success";

    public static final String LABEL_ERROR_CLASS = "label-error";

    public static final Insets TAB_PADDING = new Insets(20);

    private TabPane mainPane;

    private List<TabController> tabControllers;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        this.tabControllers = new ArrayList<>();

        Config config = new Config();

        mainPane = generateMainPane(stage, config);

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

    private TabPane generateMainPane(Stage stage, Config config) {
        TabPane result = new TabPane();

        result.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        result.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);

        result.setPrefWidth(800);
        result.setMinWidth(result.getPrefWidth());
        result.setPrefHeight(480);
        result.setMinHeight(result.getPrefHeight());

        addMergeFilesTabViewAndController(result, stage, config);
        addMergeInVideosTabViewAndController(result, stage, config);
        addSettingsTabViewAndController(result, stage, config);

        result.getSelectionModel().selectedItemProperty().addListener(this::tabChangedListener);

        return result;
    }

    private void addMergeFilesTabViewAndController(TabPane mainPane, Stage stage, Config config) {
        MergeFilesTabView tab = new MergeFilesTabView(stage, Constants.DEBUG);
        mainPane.getTabs().add(tab.getTab());

        MergeFilesTabController mergeFilesTabController = new MergeFilesTabController(tab, config);
        mergeFilesTabController.initialize();

        tabControllers.add(mergeFilesTabController);
    }

    private void addMergeInVideosTabViewAndController(TabPane mainPane, Stage stage, Config config) {
        MergeInVideosTabView tab = new MergeInVideosTabView(stage, Constants.DEBUG);
        mainPane.getTabs().add(tab.getTab());

        MergeInVideosTabController mergeInVideosTabController = new MergeInVideosTabController(
                stage,
                tab,
                config,
                this
        );
        mergeInVideosTabController.initialize();

        tabControllers.add(mergeInVideosTabController);
    }

    private void addSettingsTabViewAndController(TabPane mainPane, Stage stage, Config config) {
        SettingsTabView tab = new SettingsTabView(stage, Constants.DEBUG);
        mainPane.getTabs().add(tab.getTab());

        SettingsTabController settingsTabController = new SettingsTabController(tab, config);
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
