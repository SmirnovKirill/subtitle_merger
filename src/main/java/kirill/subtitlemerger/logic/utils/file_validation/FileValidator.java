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
        if (StringUtils.isBlank(path)) {
            return new InputFileInfo(null, null, InputFileNotValidReason.PATH_IS_EMPTY, null);
        }

        if (path.length() > PATH_LENGTH_LIMIT) {
            return new InputFileInfo(null, null, InputFileNotValidReason.PATH_IS_TOO_LONG, null);
        }

        try {
            Path.of(path);
        } catch (InvalidPathException e) {
            return new InputFileInfo(null, null, InputFileNotValidReason.INVALID_PATH, null);
        }

        File file = new File(path);
        if (file.isDirectory()) {
            return new InputFileInfo(file, null, InputFileNotValidReason.IS_A_DIRECTORY, null);
        }

        if (!file.exists()) {
            return new InputFileInfo(file, null, InputFileNotValidReason.DOES_NOT_EXIST, null);
        }

        File parent = file.getParentFile();
        if (parent == null || !parent.isDirectory()) {
            return new InputFileInfo(file, null, InputFileNotValidReason.FAILED_TO_GET_PARENT, null);
        }

        if (!CollectionUtils.isEmpty(validationOptions.getAllowedExtensions())) {
            String extension = FilenameUtils.getExtension(file.getAbsolutePath());
            if (StringUtils.isBlank(extension)) {
                return new InputFileInfo(file, parent, InputFileNotValidReason.NO_EXTENSION, null);
            }
            if (!validationOptions.getAllowedExtensions().contains(extension)) {
                return new InputFileInfo(file, parent, InputFileNotValidReason.NOT_ALLOWED_EXTENSION, null);
            }
        }

        if (!validationOptions.isAllowEmpty() && file.length() == 0) {
            return new InputFileInfo(file, parent, InputFileNotValidReason.FILE_IS_EMPTY, null);
        }

        if (validationOptions.getMaxAllowedSize() != null && file.length() > validationOptions.getMaxAllowedSize()) {
            return new InputFileInfo(file, parent, InputFileNotValidReason.FILE_IS_TOO_BIG, null);
        }

        byte[] content = null;
        if (validationOptions.isLoadContent()) {
            try {
                content = FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                return new InputFileInfo(file, parent, InputFileNotValidReason.FAILED_TO_READ_CONTENT, null);
            }
        }

        return new InputFileInfo(file, parent, null, content);
    }

    public static OutputFileInfo getOutputFileInfo(String path, OutputFileValidationOptions validationOptions) {
        if (StringUtils.isBlank(path)) {
            return new OutputFileInfo(null, null, OutputFileNotValidReason.PATH_IS_EMPTY);
        }

        if (path.length() > PATH_LENGTH_LIMIT) {
            return new OutputFileInfo(null, null, OutputFileNotValidReason.PATH_IS_TOO_LONG);
        }

        try {
            Path.of(path);
        } catch (InvalidPathException e) {
            return new OutputFileInfo(null, null, OutputFileNotValidReason.INVALID_PATH);
        }

        File file = new File(path);
        if (file.isDirectory()) {
            return new OutputFileInfo(file, null, OutputFileNotValidReason.IS_A_DIRECTORY);
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent = null;
        }

        if (!validationOptions.isAllowNonExistent() && !file.exists()) {
            return new OutputFileInfo(file, parent, OutputFileNotValidReason.DOES_NOT_EXIST);
        }

        if (!CollectionUtils.isEmpty(validationOptions.getAllowedExtensions())) {
            String extension = FilenameUtils.getExtension(file.getAbsolutePath());
            if (StringUtils.isBlank(extension)) {
                return new OutputFileInfo(file, parent, OutputFileNotValidReason.NO_EXTENSION);
            }
            if (!validationOptions.getAllowedExtensions().contains(extension)) {
                return new OutputFileInfo(file, parent, OutputFileNotValidReason.NOT_ALLOWED_EXTENSION);
            }
        }

        return new OutputFileInfo(file, parent, null);
    }
}
