package kirill.subtitlesmerger.gui;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class GuiUtilsTest {
    private static final long KB = 1024;

    private static final long MB = 1024 * 1024;

    private static final long GB = 1024 * 1024 * 1024;

    private static final long TB = 1024 * 1024 * 1024 * 1024L;

    @Test
    public void testFileSizeTextual() {
        assertThat(GuiUtils.getFileSizeTextual(0)).isEqualTo("0.00 B");
        assertThat(GuiUtils.getFileSizeTextual(KB - 1)).isEqualTo("1023.00 B");

        assertThat(GuiUtils.getFileSizeTextual(KB)).isEqualTo("1.00 KB");
        assertThat(GuiUtils.getFileSizeTextual(KB + 1)).isEqualTo("1.00 KB");
        assertThat(GuiUtils.getFileSizeTextual(KB + 10)).isEqualTo("1.01 KB");
        assertThat(GuiUtils.getFileSizeTextual(KB * 1023)).isEqualTo("1023.00 KB");

        assertThat(GuiUtils.getFileSizeTextual(MB)).isEqualTo("1.00 MB");
        assertThat(GuiUtils.getFileSizeTextual(MB * 13 - KB * 14 - 1023)).isEqualTo("12.99 MB");
        assertThat(GuiUtils.getFileSizeTextual(MB * 1023)).isEqualTo("1023.00 MB");

        assertThat(GuiUtils.getFileSizeTextual(GB)).isEqualTo("1.00 GB");
        assertThat(GuiUtils.getFileSizeTextual(GB * 13 - MB * 14)).isEqualTo("12.99 GB");
        assertThat(GuiUtils.getFileSizeTextual(GB * 1023L)).isEqualTo("1023.00 GB");

        assertThat(GuiUtils.getFileSizeTextual(TB)).isEqualTo("1.00 TB");
        assertThat(GuiUtils.getFileSizeTextual(TB * 2047)).isEqualTo("2047.00 TB");
    }
}
