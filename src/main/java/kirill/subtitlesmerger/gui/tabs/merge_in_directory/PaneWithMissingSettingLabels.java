package kirill.subtitlesmerger.gui.tabs.merge_in_directory;

import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import kirill.subtitlesmerger.gui.GuiSettings;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

public class PaneWithMissingSettingLabels extends VBox {
    @Getter
    private ObservableSet<GuiSettings.SettingType> missingSettings;

    void setMissingSettings(ObservableSet<GuiSettings.SettingType> missingSettings) {
        this.missingSettings = missingSettings;
        this.missingSettings.addListener(this::missingSettingsChanged);

        setLabels();
    }

    private void missingSettingsChanged(SetChangeListener.Change<? extends GuiSettings.SettingType> change) {
        setLabels();
    }

    private void setLabels() {
        getChildren().clear();

        if (CollectionUtils.isEmpty(missingSettings)) {
            return;
        }

        for (GuiSettings.SettingType settingType : missingSettings) {
            getChildren().add(new Label("\u2022 " + getText(settingType)));
        }
    }

    private static String getText(GuiSettings.SettingType settingType) {
        switch (settingType) {
            case FFPROBE_PATH:
                return "path to ffprobe";
            case FFMPEG_PATH:
                return "path to ffmpeg";
            case UPPER_LANGUAGE:
                return "preferred language for upper subtitles";
            case LOWER_LANGUAGE:
                return "preferred language for lower subtitles";
            default:
                throw new IllegalStateException();
        }
    }
}
