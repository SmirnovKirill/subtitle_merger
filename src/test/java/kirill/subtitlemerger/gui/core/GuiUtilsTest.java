package kirill.subtitlemerger.gui.core;

import kirill.subtitlemerger.gui.core.GuiUtils;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class GuiUtilsTest {
    private static final long KB = 1024;

    private static final long MB = 1024 * KB;

    private static final long GB = 1024 * MB;

    private static final long TB = 1024 * GB;

    @Test
    public void testFileSizeTextual() {
        assertThat(GuiUtils.getFileSizeTextual(0)).isEqualTo("0.00 B");
        assertThat(GuiUtils.getFileSizeTextual(KB - 1)).isEqualTo("1023.00 B");

        assertThat(GuiUtils.getFileSizeTextual(KB)).isEqualTo("1.00 KB");
        assertThat(GuiUtils.getFileSizeTextual(KB + 1)).isEqualTo("1.00 KB");
        assertThat(GuiUtils.getFileSizeTextual(KB + 10)).isEqualTo("1.01 KB");
        assertThat(GuiUtils.getFileSizeTextual(1023 * KB)).isEqualTo("1023.00 KB");

        assertThat(GuiUtils.getFileSizeTextual(MB)).isEqualTo("1.00 MB");
        assertThat(GuiUtils.getFileSizeTextual(13 * MB - 14 * KB - 1023)).isEqualTo("12.99 MB");
        assertThat(GuiUtils.getFileSizeTextual(1023 * MB)).isEqualTo("1023.00 MB");

        assertThat(GuiUtils.getFileSizeTextual(GB)).isEqualTo("1.00 GB");
        assertThat(GuiUtils.getFileSizeTextual(13 * GB - 14 * MB)).isEqualTo("12.99 GB");
        assertThat(GuiUtils.getFileSizeTextual(1023L * GB)).isEqualTo("1023.00 GB");

        assertThat(GuiUtils.getFileSizeTextual(TB)).isEqualTo("1.00 TB");
        assertThat(GuiUtils.getFileSizeTextual(2047 * TB)).isEqualTo("2047.00 TB");
    }

    @Test
    public void testShortenedString() {
        assertThat(
                GuiUtils.getShortenedStringIfNecessary("test", 2, 2)
        ).isEqualTo("test");

        assertThat(
                GuiUtils.getShortenedStringIfNecessary("testlonger", 2, 2)
        ).isEqualTo("te...er");

        assertThat(
                GuiUtils.getShortenedStringIfNecessary("testlonger", 2, 5)
        ).isEqualTo("te...onger");

        assertThat(
                GuiUtils.getShortenedStringIfNecessary("testlonger", 2, 6)
        ).isEqualTo("testlonger");

        assertThat(
                GuiUtils.getShortenedStringIfNecessary("testlonger", 3, 5)
        ).isEqualTo("testlonger");
    }
}
