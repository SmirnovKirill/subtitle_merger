package kirill.subtitlemerger.logic.utils.file_validation;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

@AllArgsConstructor
@Getter
public class InputFileInfo {
    private File file;

    private InputFileNotValidReason notValidReason;

    private byte[] content;
}
