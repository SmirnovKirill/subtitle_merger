package kirill.subtitles_merger.ffmpeg;

import lombok.Getter;

@Getter
public class FfmpegException extends Exception {
    private Code code;

    public FfmpegException(Code code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public FfmpegException(Code code) {
        super();
        this.code = code;
    }

    public enum Code {
        INCORRECT_FFPROBE_PATH,
        INCORRECT_FFMPEG_PATH,
        GENERAL_ERROR
    }
}
