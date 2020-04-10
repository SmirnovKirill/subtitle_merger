package kirill.subtitlemerger.logic.ffmpeg;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FfmpegException extends Exception {
    private Code code;

    private String consoleOutput;

    public enum Code {
        INCORRECT_FFPROBE_PATH,
        INCORRECT_FFMPEG_PATH,
        FAILED_TO_MOVE_TEMP_VIDEO,
        GENERAL_ERROR,
        INTERRUPTED
    }
}
