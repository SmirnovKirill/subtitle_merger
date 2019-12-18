package kirill.subtitlesmerger.gui;

import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.FfmpegException;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class GuiContext {
    @Getter
    private GuiPreferences preferences;

    @Getter
    @Setter
    private Ffprobe ffprobe;

    @Getter
    @Setter
    private Ffmpeg ffmpeg;

    public GuiContext() {
        preferences = new GuiPreferences();
        if (preferences.getFfprobeFile() != null) {
            try {
                ffprobe = new Ffprobe(preferences.getFfprobeFile());
            } catch (FfmpegException e) {
                log.error("failed to initialize ffprobe");
            }
        }
        if (preferences.getFfmpegFile() != null) {
            try {
                ffmpeg = new Ffmpeg(preferences.getFfmpegFile());
            } catch (FfmpegException e) {
                log.error("failed to initialize ffmpeg");
            }
        }
    }
}
