package kirill.subtitlemerger.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.settings.SettingType;
import kirill.subtitlemerger.logic.settings.Settings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

import java.util.EnumSet;
import java.util.Set;

import static kirill.subtitlemerger.logic.settings.SettingType.*;

@CommonsLog
@Getter
public class GuiContext {
    private Settings settings;

    @Setter
    private Ffprobe ffprobe;

    @Setter
    private Ffmpeg ffmpeg;

    @Getter(value = AccessLevel.NONE)
    private BooleanProperty videosInProgress = new SimpleBooleanProperty(false);

    /**
     * Settings required for merging in videos.
     */
    private ObservableSet<SettingType> missingSettings;

    public GuiContext() {
        settings = new Settings();
        missingSettings = generateMissingSettings(settings);

        if (settings.getFfprobeFile() != null) {
            try {
                ffprobe = new Ffprobe(settings.getFfprobeFile());
            } catch (FfmpegException e) {
                log.error("failed to initialize ffprobe: " + e.getCode() + ", console output " + e.getConsoleOutput());
            } catch (InterruptedException e) {
                log.error("something's not right, process can't be interrupted");
            }
        }

        if (settings.getFfmpegFile() != null) {
            try {
                ffmpeg = new Ffmpeg(settings.getFfmpegFile());
            } catch (FfmpegException e) {
                log.error("failed to initialize ffmpeg: " + e.getCode() + ", console output " + e.getConsoleOutput());
            } catch (InterruptedException e) {
                log.error("something's not right, process can't be interrupted");
            }
        }
    }

    private static ObservableSet<SettingType> generateMissingSettings(Settings settings) {
        Set<SettingType> result = EnumSet.noneOf(SettingType.class);

        if (settings.getFfprobeFile() == null) {
            result.add(FFPROBE_PATH);
        }

        if (settings.getFfmpegFile() == null) {
            result.add(FFMPEG_PATH);
        }

        if (settings.getUpperLanguage() == null) {
            result.add(UPPER_LANGUAGE);
        }

        if (settings.getLowerLanguage() == null) {
            result.add(LOWER_LANGUAGE);
        }

        if (settings.getMergeMode() == null) {
            result.add(MERGE_MODE);
        }

        return FXCollections.observableSet(result);
    }

    public boolean getVideosInProgress() {
        return videosInProgress.get();
    }

    public BooleanProperty videosInProgressProperty() {
        return videosInProgress;
    }

    public void setVideosInProgress(boolean videosInProgress) {
        this.videosInProgress.set(videosInProgress);
    }
}
