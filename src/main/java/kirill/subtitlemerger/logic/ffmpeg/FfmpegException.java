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
        PROCESS_FAILED,
        FAILED_TO_CONVERT_JSON,
        FAILED_TO_READ_TEMP_SUBTITLE_FILE,
        FAILED_TO_CREATE_TEMP_SUBTITLE_FILE,
        FAILED_TO_MOVE_TEMP_VIDEO
    }
}
