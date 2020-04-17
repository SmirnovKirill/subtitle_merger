package kirill.subtitlemerger.logic.utils;

import com.neovisionaries.i18n.LanguageAlpha3Code;

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

    /**
     * Language are considered to be equal if their codes are the same or if they represent the same language and are
     * just synonyms (bibliographic and terminological versions).
     */
    public static boolean languagesEqual(LanguageAlpha3Code first, LanguageAlpha3Code second) {
        /* Also covers the case when they are both null. */
        if (first == second) {
            return true;
        }

        /* If first is null then second is surely not null. */
        if (first == null) {
            return false;
        }

        return first.getSynonym() == second;
    }
}
