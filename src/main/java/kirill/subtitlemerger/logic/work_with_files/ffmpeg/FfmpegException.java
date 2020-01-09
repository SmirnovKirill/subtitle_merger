package kirill.subtitlemerger.logic.work_with_files.ffmpeg;

import lombok.Getter;

@Getter
public class FfmpegException extends Exception {
    private Code code;

    FfmpegException(Code code) {
        super();
        this.code = code;
    }

    public enum Code {
        INCORRECT_FFPROBE_PATH,
        INCORRECT_FFMPEG_PATH,
        FAILED_TO_MOVE_TEMP_VIDEO,
        GENERAL_ERROR
    }
}
