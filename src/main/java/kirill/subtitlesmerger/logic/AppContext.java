package kirill.subtitlesmerger.logic;

import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.FfmpegException;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class AppContext {
    @Getter
    private Config config;

    @Getter
    @Setter
    private Ffprobe ffprobe;

    @Getter
    @Setter
    private Ffmpeg ffmpeg;

    public AppContext() {
        config = new Config();
        if (config.getFfprobeFile() != null) {
            try {
                ffprobe = new Ffprobe(config.getFfprobeFile());
            } catch (FfmpegException e) {
                log.error("failed to initialize ffprobe");
            }
        }
        if (config.getFfmpegFile() != null) {
            try {
                ffmpeg = new Ffmpeg(config.getFfmpegFile());
            } catch (FfmpegException e) {
                log.error("failed to initialize ffmpeg");
            }
        }
    }
}
