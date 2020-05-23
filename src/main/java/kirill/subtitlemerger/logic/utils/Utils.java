package kirill.subtitlemerger.logic.utils;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.videos.entities.BuiltInSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.ExternalSubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.SubtitleOption;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains different helper methods that can be useful for our application.
 */
@CommonsLog
public class Utils {
    /**
     * Returns the language code if the language is not empty or the string "unknown language" otherwise.
     */
    public static String languageToString(LanguageAlpha3Code language) {
        return language != null ? language.toString() : "unknown language";
    }

    /**
     * Languages are considered to be equal if their codes are the same or if they represent the same language and are
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

    /**
     * Returns the shortened version of the string if necessary. The string will be shortened only if its size is larger
     * than or equal to charsBeforeEllipsis + charsAfterEllipsis + 3. These 3 extra characters are needed because if
     * less than 3 characters are shortened it would look weird.
     *
     * @param string the string to process
     * @param charsBeforeEllipsis the number of characters before the ellipsis in the shortened string
     * @param charsAfterEllipsis the number of characters after the ellipsis in the shortened string
     * @return a shortened version of the string if it was too long or the original string otherwise
     */
    public static String getShortenedString(String string, int charsBeforeEllipsis, int charsAfterEllipsis) {
        if (string.length() < charsBeforeEllipsis + charsAfterEllipsis + 3) {
            return string;
        }

        return string.substring(0, charsBeforeEllipsis)
                + "..."
                + string.substring(string.length() - charsAfterEllipsis);
    }

    /**
     * This method is helpful for displaying English texts.
     *
     * @param count the number of items
     * @param oneItemText the text to return when there is only one item, this text can't use any format arguments
     *                   because there is always only one item
     * @param zeroOrSeveralItemsText the text to return when there are zero or several items, this text can use the
     *                               format argument %d inside
     * @return a text depending on the count.
     */
    public static String getTextDependingOnCount(int count, String oneItemText, String zeroOrSeveralItemsText) {
        if (count == 1) {
            return oneItemText;
        } else {
            return String.format(zeroOrSeveralItemsText, count);
        }
    }

    /**
     * Returns the textual representation of the size.
     *
     * @param size the size to represent.
     * @param keepShort if set to true the result will be for example "100 KB" instead of "100.00 KB", "99.9 KB" instead
     *                  of "99.91 KB" and so on - the number of digits after the decimal point will be reduced depending
     *                  on the whole part so that in general there are no more than four symbols in the textual
     *                  representation (plus the size suffix). Otherwise there will always be 2 digits after the point.
     * @return a textual representation of the size (for example 21.39 KB).
     */
    public static String getSizeTextual(long size, boolean keepShort) {
        List<String> suffixes = Arrays.asList("B", "KB", "MB", "GB", "TB");

        BigDecimal sizeBigDecimal = new BigDecimal(size);

        BigDecimal divisor = BigDecimal.ONE;
        int suffixIndex = 0;
        while (suffixIndex < suffixes.size() - 1) {
            if (sizeBigDecimal.divide(divisor, 2, RoundingMode.HALF_UP).compareTo(new BigDecimal(1024)) < 0) {
                break;
            }

            divisor = divisor.multiply(new BigDecimal(1024));
            suffixIndex++;
        }

        int scale = getScale(sizeBigDecimal, divisor, keepShort);

        return sizeBigDecimal.divide(divisor, scale, RoundingMode.HALF_UP) + " " + suffixes.get(suffixIndex);
    }

    private static int getScale(BigDecimal size, BigDecimal divisor, boolean keepShort) {
        if (!keepShort) {
            return 2;
        }

        BigInteger wholePart = size.divide(divisor, 0, RoundingMode.FLOOR).toBigInteger();
        if (wholePart.compareTo(BigInteger.valueOf(9999)) >= 0) {
            /*
             * If we got here it means that the size is more than 9999 terabytes because otherwise as soon as the size
             * is equal to or more than 1024 it's automatically divided by 1024.
             */
            log.error("it's impossible to keep short the size that big: " + size);
            throw new IllegalArgumentException();
        }

        int preliminaryResult;
        if (wholePart.compareTo(BigInteger.valueOf(100)) >= 0) {
            preliminaryResult = 0;
        } else if (wholePart.compareTo(BigInteger.valueOf(10)) >= 0) {
            preliminaryResult = 1;
        } else {
            preliminaryResult = 2;
        }

        /*
         * There are two border cases - when the whole part is 99 or 9. Because after adding the fractional part and
         * rounding the whole part may start to have more digits than before.
         */
        if (wholePart.compareTo(BigInteger.valueOf(99)) != 0 && wholePart.compareTo(BigInteger.valueOf(9)) != 0) {
            return preliminaryResult;
        }

        BigInteger wholePartAfterwards = size.divide(divisor, preliminaryResult, RoundingMode.HALF_UP).toBigInteger();
        if (wholePartAfterwards.compareTo(wholePart) > 0) {
            return preliminaryResult - 1;
        } else {
            return preliminaryResult;
        }
    }

    public static String getMergedSubtitleFilePath(
            Video video,
            SubtitleOption upperOption,
            SubtitleOption lowerOption
    ) {
        return FilenameUtils.removeExtension(video.getFile().getAbsolutePath()) + "_"
                + getOptionTitleForFile(upperOption) + "-" + getOptionTitleForFile(lowerOption) + ".srt";
    }

    private static String getOptionTitleForFile(SubtitleOption option) {
        if (option instanceof ExternalSubtitleOption) {
            return "external";
        } else if (option instanceof BuiltInSubtitleOption) {
            LanguageAlpha3Code language = ((BuiltInSubtitleOption) option).getLanguage();
            return language != null ? language.toString() : "unknown";
        } else {
            log.error("unexpected subtitle option type: " + option.getClass() + ", most likely a bug");
            throw new IllegalStateException();
        }
    }
}
