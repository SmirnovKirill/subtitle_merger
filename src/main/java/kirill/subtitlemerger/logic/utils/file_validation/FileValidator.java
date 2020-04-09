package kirill.subtitlemerger.logic.utils.file_validation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public class FileValidator {
    public static final int PATH_LENGTH_LIMIT = 4096;

    public static Optional<InputFileInfo> getInputFileInfo(
            String path,
            Collection<String> allowedExtensions,
            boolean allowEmpty,
            long maxAllowedSize,
            boolean loadContent
    ) {
        if (StringUtils.isBlank(path)) {
            return Optional.empty();
        }

        if (path.length() > PATH_LENGTH_LIMIT) {
            return Optional.of(
                    new InputFileInfo(null, null, IncorrectInputFileReason.PATH_IS_TOO_LONG, null)
            );
        }

        try {
            Path.of(path);
        } catch (InvalidPathException e) {
            return Optional.of(
                    new InputFileInfo(null, null, IncorrectInputFileReason.INVALID_PATH, null)
            );
        }

        File file = new File(path);
        if (file.isDirectory()) {
            return Optional.of(
                    new InputFileInfo(file, null, IncorrectInputFileReason.IS_A_DIRECTORY, null)
            );
        }

        if (!file.exists()) {
            return Optional.of(
                    new InputFileInfo(file, null, IncorrectInputFileReason.DOES_NOT_EXIST, null)
            );
        }

        File parent = file.getParentFile();
        if (parent == null || !parent.isDirectory()) {
            return Optional.of(
                    new InputFileInfo(
                            file,
                            null,
                            IncorrectInputFileReason.FAILED_TO_GET_PARENT_DIRECTORY,
                            null
                    )
            );
        }

        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        if (!allowedExtensions.contains(extension)) {
            return Optional.of(
                    new InputFileInfo(file, parent, IncorrectInputFileReason.EXTENSION_IS_NOT_VALID, null)
            );
        }

        if (!allowEmpty && file.length() == 0) {
            return Optional.of(new InputFileInfo(file, parent, IncorrectInputFileReason.FILE_IS_EMPTY, null));
        }

        if (file.length() > maxAllowedSize) {
            return Optional.of(new InputFileInfo(file, parent, IncorrectInputFileReason.FILE_IS_TOO_BIG, null));
        }

        byte[] content = null;
        if (loadContent) {
            try {
                content = FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                return Optional.of(
                        new InputFileInfo(file, parent, IncorrectInputFileReason.FAILED_TO_READ_CONTENT, null)
                );
            }
        }

        return Optional.of(new InputFileInfo(file, parent, null, content));
    }

    public static Optional<OutputFileInfo> getOutputFileInfo(
            String path,
            Collection<String> allowedExtensions,
            boolean allowNonExistent
    ) {
        if (StringUtils.isBlank(path)) {
            return Optional.empty();
        }

        if (path.length() > PATH_LENGTH_LIMIT) {
            return Optional.of(new OutputFileInfo(null, null, IncorrectOutputFileReason.PATH_IS_TOO_LONG));
        }

        try {
            Path.of(path);
        } catch (InvalidPathException e) {
            return Optional.of(new OutputFileInfo(null, null, IncorrectOutputFileReason.INVALID_PATH));
        }

        File file = new File(path);
        if (file.isDirectory()) {
            return Optional.of(new OutputFileInfo(file, null, IncorrectOutputFileReason.IS_A_DIRECTORY));
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent = null;
        }

        if (!allowNonExistent && !file.exists()) {
            return Optional.of(new OutputFileInfo(file, parent, IncorrectOutputFileReason.DOES_NOT_EXIST));
        }

        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        if (!allowedExtensions.contains(extension)) {
            return Optional.of(new OutputFileInfo(file, parent, IncorrectOutputFileReason.EXTENSION_IS_NOT_VALID));
        }

        return Optional.of(new OutputFileInfo(file, parent, null));
    }
}