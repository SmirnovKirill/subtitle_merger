package kirill.subtitlemerger.logic.utils.file_validation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;

public class FileValidator {
    public static final int PATH_LENGTH_LIMIT = 4096;

    public static InputFileInfo getInputFileInfo(
            String path,
            Collection<String> allowedExtensions,
            boolean allowEmpty,
            long maxAllowedSize,
            boolean loadContent
    ) {
        if (StringUtils.isBlank(path)) {
            new InputFileInfo(null, null, InputFileNotValidReason.PATH_IS_EMPTY, null);
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

        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        if (!allowedExtensions.contains(extension)) {
            return new InputFileInfo(file, parent, InputFileNotValidReason.EXTENSION_IS_NOT_VALID, null);
        }

        if (!allowEmpty && file.length() == 0) {
            return new InputFileInfo(file, parent, InputFileNotValidReason.FILE_IS_EMPTY, null);
        }

        if (file.length() > maxAllowedSize) {
            return new InputFileInfo(file, parent, InputFileNotValidReason.FILE_IS_TOO_BIG, null);
        }

        byte[] content = null;
        if (loadContent) {
            try {
                content = FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                return new InputFileInfo(file, parent, InputFileNotValidReason.FAILED_TO_READ_CONTENT, null);
            }
        }

        return new InputFileInfo(file, parent, null, content);
    }

    public static OutputFileInfo getOutputFileInfo(
            String path,
            Collection<String> allowedExtensions,
            boolean allowNonExistent
    ) {
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

        if (!allowNonExistent && !file.exists()) {
            return new OutputFileInfo(file, parent, OutputFileNotValidReason.DOES_NOT_EXIST);
        }

        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        if (!allowedExtensions.contains(extension)) {
            return new OutputFileInfo(file, parent, OutputFileNotValidReason.EXTENSION_IS_NOT_ALLOWED);
        }

        return new OutputFileInfo(file, parent, null);
    }
}
