package kirill.subtitlemerger.gui.core;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.extern.apachecommons.CommonsLog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@CommonsLog
public class GuiUtils {
    public static String getFileSizeTextual(long size) {
        List<String> sizes = Arrays.asList("B", "KB", "MB", "GB", "TB");

        BigDecimal divisor = new BigDecimal(1024);
        BigDecimal sizeBigDecimal = new BigDecimal(size);

        int power = 0;
        while (power < sizes.size() - 1) {
            if (sizeBigDecimal.divide(divisor.pow(power), 2, RoundingMode.HALF_UP).compareTo(divisor) < 0) {
                break;
            }

            power++;
        }

        return sizeBigDecimal.divide(divisor.pow(power), 2, RoundingMode.HALF_UP) + " " + sizes.get(power);
    }

    public static Tooltip generateTooltip(String text) {
        Tooltip result = new Tooltip(text);

        result.setShowDelay(Duration.ZERO);
        result.setShowDuration(Duration.INDEFINITE);

        return result;
    }

    /**
     * @param count number of items
     * @param oneItemText text to return when there is only one item, this text can't use any format arguments because
     *                    there is always only one item
     * @param zeroOrSeveralItemsText text to return when there are zero or several items, this text can use format
     *                               argument %d inside
     * @return text depending on the count.
     */
    public static String getTextDependingOnTheCount(int count, String oneItemText, String zeroOrSeveralItemsText) {
        if (count == 1) {
            return oneItemText;
        } else {
            return String.format(zeroOrSeveralItemsText, count);
        }
    }

    public static FileInfo findMatchingFileInfo(GuiFileInfo guiFileInfo, List<FileInfo> filesInfo) {
        return filesInfo.stream()
                .filter(fileInfo -> Objects.equals(fileInfo.getFile().getAbsolutePath(), guiFileInfo.getFullPath()))
                .findFirst().orElseThrow(IllegalStateException::new);
    }

    public static GuiSubtitleStream findMatchingGuiStream(int ffmpegIndex, List<GuiSubtitleStream> guiStreams) {
        return guiStreams.stream()
                .filter(stream -> stream.getFfmpegIndex() == ffmpegIndex)
                .findFirst().orElseThrow(IllegalStateException::new);
    }
}
