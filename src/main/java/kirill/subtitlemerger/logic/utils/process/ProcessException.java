package kirill.subtitlemerger.logic.utils.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ProcessException extends Exception {
    private Code code;

    private String consoleOutput;

    public enum Code {
        FAILED_TO_START,
        FAILED_TO_READ_OUTPUT,
        INTERRUPTED,
        PROCESS_KILLED,
        EXIT_VALUE_NOT_ZERO
    }
}
