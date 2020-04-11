package kirill.subtitlemerger.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
@Getter
public class GuiContext {
    private GuiSettings settings;

    @Setter
    private Ffprobe ffprobe;

    @Setter
    private Ffmpeg ffmpeg;

    @Getter(value = AccessLevel.NONE)
    private BooleanProperty workWithVideosInProgress = new SimpleBooleanProperty(false);

    public GuiContext() {
        settings = new GuiSettings();
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

    public boolean isWorkWithVideosInProgress() {
        return workWithVideosInProgress.get();
    }

    public BooleanProperty workWithVideosInProgressProperty() {
        return workWithVideosInProgress;
    }

    public void setWorkWithVideosInProgress(boolean workWithVideosInProgress) {
        this.workWithVideosInProgress.set(workWithVideosInProgress);
    }
}
