package kirill.subtitlemerger.gui.utils;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import kirill.subtitlemerger.gui.utils.custom_forms.ErrorPopupController;
import kirill.subtitlemerger.gui.utils.entities.NodeAndController;
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
public class GuiHelperMethods {
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

        result.setGraphic(createImageView(imageUrl, width, height));

        return result;
    }

    public static ImageView createImageView(String imageUrl, int width, int height) {
        ImageView result = new ImageView(new Image(imageUrl));

        result.setFitWidth(width);
        result.setFitHeight(height);

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

    public static Tooltip generateTooltip(StringBinding text) {
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

    public static String languageToString(LanguageAlpha3Code language) {
        return language != null ? language.toString() : "unknown language";
    }

    public static void initializeCustomControl(String path, Object root) {
        FXMLLoader fxmlLoader = new FXMLLoader(GuiHelperMethods.class.getResource(path));

        fxmlLoader.setRoot(root);
        fxmlLoader.setController(root);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml " + path + ": " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }
    }

    public static NodeAndController loadNodeAndController(String path) {
        FXMLLoader fxmlLoader = new FXMLLoader(GuiHelperMethods.class.getResource(path));

        Node node;
        try {
            node = fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml " + path + ": " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }

        Object controller;
        try {
            controller = Objects.requireNonNull(fxmlLoader.getController());
        } catch (NullPointerException e) {
            log.error("controller is not set");
            throw new IllegalStateException();
        } catch (ClassCastException e) {
            log.error("controller has an incorrect class");
            throw new IllegalStateException();
        }

        return new NodeAndController(node, controller);
    }

    public static void setVisibleAndManaged(Node node, boolean value) {
        node.setVisible(value);
        node.setManaged(value);
    }

    public static void bindVisibleAndManaged(Node node, BooleanBinding binding) {
        node.visibleProperty().bind(binding);
        node.managedProperty().bind(binding);
    }

    public static Region createFixedHeightSpacer(int height) {
        Region result = new Region();

        result.setMinHeight(height);
        result.setMaxHeight(height);

        return result;
    }

    public static Region createFixedWidthSpacer(int width) {
        Region result = new Region();

        setFixedWidth(result, width);

        return result;
    }

    public static void setFixedWidth(Region region, int width) {
        region.setMinWidth(width);
        region.setMaxWidth(width);
    }

    public static Stage createPopupStage(String title, Parent content, Stage ownerStage) {
        Stage result = new Stage();

        //todo set stage parameters in the same manner everywhere
        result.setTitle(title);
        result.initModality(Modality.APPLICATION_MODAL);
        result.initOwner(ownerStage);
        result.setResizable(false);

        Scene scene = new Scene(content);
        scene.getStylesheets().add("/gui/style.css");
        result.setScene(scene);

        return result;
    }

    public static void showErrorPopup(String message, Stage ownerStage) {
        NodeAndController nodeAndController = GuiHelperMethods.loadNodeAndController(
                "/gui/utils/custom_forms/errorPopup.fxml"
        );

        Stage popupStage = GuiHelperMethods.createPopupStage(
                "Error!",
                nodeAndController.getNode(),
                ownerStage
        );

        ErrorPopupController controller = nodeAndController.getController();
        controller.initialize(message, popupStage);

        popupStage.showAndWait();
    }
}
