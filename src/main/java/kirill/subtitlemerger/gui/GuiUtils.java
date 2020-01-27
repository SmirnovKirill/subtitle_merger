package kirill.subtitlemerger.gui;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

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

    public static void startTask(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public static Tooltip generateTooltip(String text) {
        Tooltip result = new Tooltip(text);

        result.setShowDelay(Duration.ZERO);
        result.setShowDuration(Duration.INDEFINITE);

        return result;
    }

    public static void loadWithUncheckedException(FXMLLoader loader) {
        try {
            loader.load();
        } catch (IOException e) {
            log.error("failed to parse fxml: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }
    }
}
