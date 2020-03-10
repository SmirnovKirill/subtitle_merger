package kirill.subtitlemerger.gui.utils;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class GuiUtilsTest {
    private static final long KB = 1024;

    private static final long MB = 1024 * KB;

    private static final long GB = 1024 * MB;

    private static final long TB = 1024 * GB;

    @Test
    public void testFileSizeTextual() {
        assertThat(GuiHelperMethods.getFileSizeTextual(0)).isEqualTo("0.00 B");
        assertThat(GuiHelperMethods.getFileSizeTextual(KB - 1)).isEqualTo("1023.00 B");

        assertThat(GuiHelperMethods.getFileSizeTextual(KB)).isEqualTo("1.00 KB");
        assertThat(GuiHelperMethods.getFileSizeTextual(KB + 1)).isEqualTo("1.00 KB");
        assertThat(GuiHelperMethods.getFileSizeTextual(KB + 10)).isEqualTo("1.01 KB");
        assertThat(GuiHelperMethods.getFileSizeTextual(1023 * KB)).isEqualTo("1023.00 KB");

        assertThat(GuiHelperMethods.getFileSizeTextual(MB)).isEqualTo("1.00 MB");
        assertThat(GuiHelperMethods.getFileSizeTextual(13 * MB - 14 * KB - 1023)).isEqualTo("12.99 MB");
        assertThat(GuiHelperMethods.getFileSizeTextual(1023 * MB)).isEqualTo("1023.00 MB");

        assertThat(GuiHelperMethods.getFileSizeTextual(GB)).isEqualTo("1.00 GB");
        assertThat(GuiHelperMethods.getFileSizeTextual(13 * GB - 14 * MB)).isEqualTo("12.99 GB");
        assertThat(GuiHelperMethods.getFileSizeTextual(1023L * GB)).isEqualTo("1023.00 GB");

        assertThat(GuiHelperMethods.getFileSizeTextual(TB)).isEqualTo("1.00 TB");
        assertThat(GuiHelperMethods.getFileSizeTextual(2047 * TB)).isEqualTo("2047.00 TB");
    }

    @Test
    public void testShortenedString() {
        assertThat(
                GuiHelperMethods.getShortenedStringIfNecessary("test", 2, 2)
        ).isEqualTo("test");

        assertThat(
                GuiHelperMethods.getShortenedStringIfNecessary("testlonger", 2, 2)
        ).isEqualTo("te...er");

        assertThat(
                GuiHelperMethods.getShortenedStringIfNecessary("testlonger", 2, 5)
        ).isEqualTo("te...onger");

        assertThat(
                GuiHelperMethods.getShortenedStringIfNecessary("testlonger", 2, 6)
        ).isEqualTo("testlonger");

        assertThat(
                GuiHelperMethods.getShortenedStringIfNecessary("testlonger", 3, 5)
        ).isEqualTo("testlonger");
    }
}
