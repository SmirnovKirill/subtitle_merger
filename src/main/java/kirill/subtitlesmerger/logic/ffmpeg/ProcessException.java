package kirill.subtitlesmerger.logic.ffmpeg;

import lombok.Getter;

@Getter
class ProcessException extends Exception {
    private Code code;

    private String consoleOutput;

    ProcessException(Code code, String consoleOutput) {
        super();
        this.code = code;
        this.consoleOutput = consoleOutput;
    }

    enum Code {
        FAILED_TO_START,
        FAILED_TO_READ_OUTPUT,
        INTERRUPTED,
        PROCESS_KILLED,
        EXIT_VALUE_NOT_ZERO
    }
}
