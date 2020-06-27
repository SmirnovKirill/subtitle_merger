package kirill.subtitlemerger.logic.utils.file_validation;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class FileValidator {
    public static final int PATH_LENGTH_LIMIT = 4096;

    public static InputFileInfo getInputFileInfo(String path, InputFileValidationOptions validationOptions) {
        File file = new File(path);

        if (StringUtils.isBlank(path)) {
            return new InputFileInfo(file, InputFileNotValidReason.PATH_IS_EMPTY, null);
        }

        if (path.length() > PATH_LENGTH_LIMIT) {
            return new InputFileInfo(file, InputFileNotValidReason.PATH_IS_TOO_LONG, null);
        }

        try {
            Path.of(path);
        } catch (InvalidPathException e) {
            return new InputFileInfo(file, InputFileNotValidReason.INVALID_PATH, null);
        }

        if (file.isDirectory()) {
            return new InputFileInfo(file, InputFileNotValidReason.IS_A_DIRECTORY, null);
        }

        if (!file.exists()) {
            return new InputFileInfo(file, InputFileNotValidReason.DOES_NOT_EXIST, null);
        }

        if (!CollectionUtils.isEmpty(validationOptions.getAllowedExtensions())) {
            String extension = FilenameUtils.getExtension(file.getAbsolutePath());
            if (StringUtils.isBlank(extension)) {
                return new InputFileInfo(file, InputFileNotValidReason.NO_EXTENSION, null);
            }
            if (!validationOptions.getAllowedExtensions().contains(extension)) {
                return new InputFileInfo(file, InputFileNotValidReason.NOT_ALLOWED_EXTENSION, null);
            }
        }

        if (!validationOptions.isAllowEmpty() && file.length() == 0) {
            return new InputFileInfo(file, InputFileNotValidReason.FILE_IS_EMPTY, null);
        }

        if (validationOptions.getMaxAllowedSize() != null && file.length() > validationOptions.getMaxAllowedSize()) {
            return new InputFileInfo(file, InputFileNotValidReason.FILE_IS_TOO_BIG, null);
        }

        byte[] content = null;
        if (validationOptions.isLoadContent()) {
            try {
                content = FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                return new InputFileInfo(file, InputFileNotValidReason.FAILED_TO_READ_CONTENT, null);
            }
        }

        return new InputFileInfo(file, null, content);
    }

    public static OutputFileInfo getOutputFileInfo(String path, OutputFileValidationOptions validationOptions) {
        File file = new File(path);

        if (StringUtils.isBlank(path)) {
            return new OutputFileInfo(file, OutputFileNotValidReason.PATH_IS_EMPTY);
        }

        if (path.length() > PATH_LENGTH_LIMIT) {
            return new OutputFileInfo(file, OutputFileNotValidReason.PATH_IS_TOO_LONG);
        }

        try {
            Path.of(path);
        } catch (InvalidPathException e) {
            return new OutputFileInfo(file, OutputFileNotValidReason.INVALID_PATH);
        }

        if (file.isDirectory()) {
            return new OutputFileInfo(file, OutputFileNotValidReason.IS_A_DIRECTORY);
        }

        if (!validationOptions.isAllowNonExistent() && !file.exists()) {
            return new OutputFileInfo(file, OutputFileNotValidReason.DOES_NOT_EXIST);
        }

        if (!CollectionUtils.isEmpty(validationOptions.getAllowedExtensions())) {
            String extension = FilenameUtils.getExtension(file.getAbsolutePath());
            if (StringUtils.isBlank(extension)) {
                return new OutputFileInfo(file, OutputFileNotValidReason.NO_EXTENSION);
            }
            if (!validationOptions.getAllowedExtensions().contains(extension)) {
                return new OutputFileInfo(file, OutputFileNotValidReason.NOT_ALLOWED_EXTENSION);
            }
        }

        return new OutputFileInfo(file, null);
    }
}
