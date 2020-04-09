package kirill.subtitlemerger.logic.utils.file_validation;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

@AllArgsConstructor
@Getter
public class OutputFileInfo {
    private File file;

    private File parent;

    private IncorrectOutputFileReason incorrectFileReason;
}
