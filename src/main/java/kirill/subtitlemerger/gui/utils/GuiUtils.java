package kirill.subtitlemerger.gui.utils;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@CommonsLog
public class GuiUtils {
    /**
     * Set change listeners so that the onChange method will be invoked each time Enter button is pressed or the focus
     * is lost.
     */
    public static void setTextFieldChangeListeners(TextField textField, Consumer<String> onChange) {
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                return;
            }

            String value = textField.getText();
            onChange.accept(value);
        });

        textField.setOnKeyPressed(keyEvent -> {
            if (!keyEvent.getCode().equals(KeyCode.ENTER)) {
                return;
            }

            String value = textField.getText();
            onChange.accept(value);
        });
    }

    /**
     * Returns the shortened version of the string if necessary. String will be shortened only if its size is larger
     * than or equal to charsBeforeEllipsis + charsAfterEllipsis + 3. These 3 extra characters are needed because if
     * less than 3 characters are shortened it would look weird.
     *
     * @param string string to process
     * @param charsBeforeEllipsis number of characters before the ellipsis in the shortened string
     * @param charsAfterEllipsis number of characters after the ellipsis in the shortened string
     * @return the shortened version of the string if it was too long or the original string otherwise
     */
    public static String getShortenedStringIfNecessary(
            String string,
            int charsBeforeEllipsis,
            int charsAfterEllipsis
    ) {
        if (string.length() < charsBeforeEllipsis + charsAfterEllipsis + 3) {
            return string;
        }

        return string.substring(0, charsBeforeEllipsis)
                + "..."
                + string.substring(string.length() - charsAfterEllipsis);
    }

    public static Button createImageButton(String text, String imageUrl, int width, int height) {
        Button result = new Button(text);

        result.getStyleClass().add("image-button");

        ImageView imageView = new ImageView(new Image(imageUrl));
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        result.setGraphic(imageView);

        return result;
    }

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

        setTooltipProperties(result);

        return result;
    }

    public static Tooltip generateTooltip(StringProperty text) {
        Tooltip result = new Tooltip();

        result.textProperty().bind(text);
        setTooltipProperties(result);

        return result;
    }

    private static void setTooltipProperties(Tooltip tooltip) {
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setShowDuration(Duration.INDEFINITE);
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

    public static String languageToString(LanguageAlpha3Code language) {
        return language != null ? language.toString() : "unknown language";
    }

    public static <T> T loadFxmlAndReturnController(String path, Object root) {
        FXMLLoader fxmlLoader = new FXMLLoader(GuiUtils.class.getResource(path));

        fxmlLoader.setRoot(root);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml " + path + ": " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }

        try {
            return Objects.requireNonNull(fxmlLoader.getController());
        } catch (NullPointerException e) {
            log.error("controller is not set");
            throw new IllegalStateException();
        } catch (ClassCastException e) {
            log.error("controller has an incorrect class");
            throw new IllegalStateException();
        }
    }

    public static <T, S> NodeAndController<T, S> loadNodeAndController(String path) {
        FXMLLoader fxmlLoader = new FXMLLoader(GuiUtils.class.getResource(path));

        T node;
        try {
            node = fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml " + path + ": " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }

        S controller;
        try {
            controller = Objects.requireNonNull(fxmlLoader.getController());
        } catch (NullPointerException e) {
            log.error("controller is not set");
            throw new IllegalStateException();
        } catch (ClassCastException e) {
            log.error("controller has an incorrect class");
            throw new IllegalStateException();
        }

        return new NodeAndController<>(node, controller);
    }

    @AllArgsConstructor
    @Getter
    public static class NodeAndController<T, S> {
        private T node;

        private S controller;
    }
}
