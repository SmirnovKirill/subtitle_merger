package kirill.subtitlemerger.logic.utils;

import java.io.File;
import java.util.Optional;

/**
 * This class contains different helper methods that can be useful for our application.
 */
public class Utils {
    /**
     * This method returns the parent directory for the given file if it exists and is actually a directory. Method
     * doesn't throw any exceptions and its main purpose is to help saving the directory with the file to the settings.
     */
    public static Optional<File> getParentDirectory(File file) {
        File result = file.getParentFile();
        if (result == null) {
            return Optional.empty();
        }

        if (!result.isDirectory()) {
            return Optional.empty();
        }

        return Optional.of(result);
    }
}
