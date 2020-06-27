package kirill.subtitlemerger.logic.utils;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class UtilsTest {
    private static final long KB = 1024;

    private static final long MB = 1024 * KB;

    private static final long GB = 1024 * MB;

    private static final long TB = 1024 * GB;

    @Test
    public void testShortenedString() {
        assertThat(Utils.getShortenedString("test", 2, 2))
                .isEqualTo("test");

        assertThat(Utils.getShortenedString("testlonger", 2, 2))
                .isEqualTo("te...er");

        assertThat(Utils.getShortenedString("testlonger", 2, 5))
                .isEqualTo("te...onger");

        assertThat(Utils.getShortenedString("testlonger", 2, 6))
                .isEqualTo("testlonger");

        assertThat(Utils.getShortenedString("testlonger", 3, 5))
                .isEqualTo("testlonger");
    }

    @Test
    public void testSizeTextual() {
        assertThat(Utils.getSizeTextual(0, false)).isEqualTo("0.00 B");
        assertThat(Utils.getSizeTextual(KB - 1, false)).isEqualTo("1023.00 B");

        assertThat(Utils.getSizeTextual(KB, false)).isEqualTo("1.00 KB");
        assertThat(Utils.getSizeTextual(KB + 1, false)).isEqualTo("1.00 KB");
        assertThat(Utils.getSizeTextual(KB + 10, false)).isEqualTo("1.01 KB");
        assertThat(Utils.getSizeTextual(1023 * KB, false)).isEqualTo("1023.00 KB");

        assertThat(Utils.getSizeTextual(MB, false)).isEqualTo("1.00 MB");
        assertThat(Utils.getSizeTextual(13 * MB - 14 * KB - 1023, false)).isEqualTo("12.99 MB");
        assertThat(Utils.getSizeTextual(1023 * MB, false)).isEqualTo("1023.00 MB");

        assertThat(Utils.getSizeTextual(GB, false)).isEqualTo("1.00 GB");
        assertThat(Utils.getSizeTextual(13 * GB - 14 * MB, false)).isEqualTo("12.99 GB");
        assertThat(Utils.getSizeTextual(1023L * GB, false)).isEqualTo("1023.00 GB");

        assertThat(Utils.getSizeTextual(TB, false)).isEqualTo("1.00 TB");
        assertThat(Utils.getSizeTextual(2047 * TB, false)).isEqualTo("2047.00 TB");
    }

    @Test
    public void testSubtitleSizeTextualKeepShort() {
        assertThat(Utils.getSizeTextual(0, true)).isEqualTo("0.00 B");
        assertThat(Utils.getSizeTextual(KB - 1, true)).isEqualTo("1023 B");

        assertThat(Utils.getSizeTextual(KB, true)).isEqualTo("1.00 KB");
        assertThat(Utils.getSizeTextual(KB + 511, true)).isEqualTo("1.50 KB");

        assertThat(Utils.getSizeTextual(99 * KB + 1023, true)).isEqualTo("100 KB");

        /* 973/1024=0.9501953125 */
        assertThat(Utils.getSizeTextual(99 * KB + 973, true)).isEqualTo("100 KB");
        /* 972/1024=0.94921875 */
        assertThat(Utils.getSizeTextual(99 * KB + 972, true)).isEqualTo("99.9 KB");
    }
}
