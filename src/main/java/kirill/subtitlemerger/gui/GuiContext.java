package kirill.subtitlemerger.gui;

import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffprobe;
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

    public GuiContext() {
        settings = new GuiSettings();
        if (settings.getFfprobeFile() != null) {
            try {
                ffprobe = new Ffprobe(settings.getFfprobeFile());
            } catch (FfmpegException e) {
                log.error("failed to initialize ffprobe");
            }
        }
        if (settings.getFfmpegFile() != null) {
            try {
                ffmpeg = new Ffmpeg(settings.getFfmpegFile());
            } catch (FfmpegException e) {
                log.error("failed to initialize ffmpeg");
            }
        }
    }
}
