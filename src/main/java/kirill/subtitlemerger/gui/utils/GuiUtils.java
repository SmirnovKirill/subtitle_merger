package kirill.subtitlemerger.gui.utils;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.StringProperty;
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
import kirill.subtitlemerger.gui.forms.common.AgreementPopupFormController;
import kirill.subtitlemerger.gui.forms.common.AgreementResult;
import kirill.subtitlemerger.gui.forms.common.ErrorPopupFormController;
import kirill.subtitlemerger.gui.utils.entities.FormInfo;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@CommonsLog
public class GuiUtils {
    /**
     * Uses FXML loader to load the form based on the provided fxml file.
     *
     * @param path path to the fxml file.
     * @return a wrapper containing root node and its controller.
     * @throws IllegalStateException if something goes wrong during the process.
     */
    public static FormInfo loadForm(String path) {
        FXMLLoader fxmlLoader = new FXMLLoader(GuiUtils.class.getResource(path));

        Parent node;
        try {
            node = fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml " + path + ": " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        } catch (ClassCastException e) {
            log.error("root object is not a node (not a parent to be more precise)");
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

        return new FormInfo(node, controller);
    }

    /**
     * Uses FXML loader to initialize the given control based on the provided fxml file.
     *
     * @param control the control to initialize
     * @param path path to the fxml file. Note that it shouldn't contain fx:controller inside because the controller
     *             for the control is the control itself.
     * @throws IllegalStateException if something goes wrong during the process.
     */
    public static void initializeControl(Object control, String path) {
        FXMLLoader fxmlLoader = new FXMLLoader(GuiUtils.class.getResource(path));

        fxmlLoader.setRoot(control);
        fxmlLoader.setController(control);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml " + path + ": " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        } catch (ClassCastException e) {
            log.error("controller has an incorrect class");
            throw new IllegalStateException();
        }
    }

    public static Stage generatePopupStage(String title, Parent node, Stage ownerStage) {
        Stage result = new Stage();

        result.initOwner(ownerStage);
        result.initModality(Modality.APPLICATION_MODAL);
        result.setTitle(title);
        result.setResizable(false);

        Scene scene = new Scene(node);
        scene.getStylesheets().add("/gui/javafx/style.css");
        result.setScene(scene);

        return result;
    }

    public static void showErrorPopup(String message, Stage ownerStage) {
        FormInfo nodeInfo = loadForm("/gui/javafx/forms/common/error_popup_form.fxml");

        Stage popupStage = generatePopupStage("Error!", nodeInfo.getRootNode(), ownerStage);

        ErrorPopupFormController controller = nodeInfo.getController();
        controller.initialize(message, popupStage);

        popupStage.showAndWait();
    }

    public static boolean showAgreementPopup(String message, String yesText, String noText, Stage ownerStage) {
        FormInfo nodeInfo = loadForm("/gui/javafx/forms/common/agreement_popup_form.fxml");

        Stage popupStage = generatePopupStage("Please confirm", nodeInfo.getRootNode(), ownerStage);

        AgreementPopupFormController controller = nodeInfo.getController();
        controller.initialize(message, null, yesText, noText, popupStage);

        popupStage.showAndWait();

        return controller.getResult() == AgreementResult.YES;
    }

    public static AgreementResult showAgreementPopup(
            String message,
            String applyToAllText,
            String yesText,
            String noText,
            Stage ownerStage
    ) {
        FormInfo nodeInfo = loadForm("/gui/javafx/forms/common/agreement_popup_form.fxml");

        Stage popupStage = generatePopupStage("Please confirm", nodeInfo.getRootNode(), ownerStage);

        AgreementPopupFormController controller = nodeInfo.getController();
        controller.initialize(message, applyToAllText, yesText, noText, popupStage);

        popupStage.showAndWait();

        return controller.getResult();
    }

    public static void setVisibleAndManaged(Node node, boolean value) {
        node.setVisible(value);
        node.setManaged(value);
    }

    public static void bindVisibleAndManaged(Node node, BooleanBinding binding) {
        node.visibleProperty().bind(binding);
        node.managedProperty().bind(binding);
    }

    /**
     * Returns the shortened version of a string if necessary. The string will be shortened only if its size is larger
     * than or equal to charsBeforeEllipsis + charsAfterEllipsis + 3. These 3 extra characters are needed because if
     * less than 3 characters are shortened it would look weird.
     *
     * @param string the string to process
     * @param charsBeforeEllipsis the number of characters before the ellipsis in the shortened string
     * @param charsAfterEllipsis the number of characters after the ellipsis in the shortened string
     * @return the shortened version of the string if it was too long or the original string otherwise
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
     * Sets the change listeners so that the value handler method will be invoked each time the Enter button is pressed
     * or the focus is lost.
     */
    public static void setTextFieldChangeListeners(TextField textField, Consumer<String> valueHandler) {
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                return;
            }

            String value = textField.getText();
            valueHandler.accept(value);
        });

        textField.setOnKeyPressed(keyEvent -> {
            if (!keyEvent.getCode().equals(KeyCode.ENTER)) {
                return;
            }

            String value = textField.getText();
            valueHandler.accept(value);
        });
    }

    public static Button generateImageButton(String text, String imageUrl, int width, int height) {
        Button result = new Button(text);

        result.getStyleClass().add("image-button");

        result.setGraphic(generateImageView(imageUrl, width, height));

        return result;
    }

    public static ImageView generateImageView(String imageUrl, int width, int height) {
        ImageView result = new ImageView(new Image(imageUrl));

        result.setFitWidth(width);
        result.setFitHeight(height);

        return result;
    }

    /**
     * Generates the tooltip that is shown indefinitely and without delays.
     */
    public static Tooltip generateTooltip(String text) {
        Tooltip result = new Tooltip(text);

        setTooltipProperties(result);

        return result;
    }

    private static void setTooltipProperties(Tooltip tooltip) {
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setShowDuration(Duration.INDEFINITE);
    }

    /**
     * Generates the tooltip that is shown indefinitely and without delays.
     */
    public static Tooltip generateTooltip(StringProperty text) {
        Tooltip result = new Tooltip();

        result.textProperty().bind(text);
        setTooltipProperties(result);

        return result;
    }

    /**
     * This method is helpful for displaying English texts.
     *
     * @param count the number of items
     * @param oneItemText the text to return when there is only one item, this text can't use any format arguments
     *                   because there is always only one item
     * @param zeroOrSeveralItemsText the text to return when there are zero or several items, this text can use format
     *                               argument %d inside
     * @return the text depending on the count.
     */
    public static String getTextDependingOnCount(int count, String oneItemText, String zeroOrSeveralItemsText) {
        if (count == 1) {
            return oneItemText;
        } else {
            return String.format(zeroOrSeveralItemsText, count);
        }
    }

    /**
     * Returns the language code if the language is not empty or the string "unknown language" otherwise.
     */
    public static String languageToString(LanguageAlpha3Code language) {
        return language != null ? language.toString() : "unknown language";
    }

    public static Region generateFixedHeightSpacer(int height) {
        Region result = new Region();

        result.setMinHeight(height);
        result.setMaxHeight(height);

        return result;
    }

    public static void setFixedWidth(Region region, int width) {
        region.setMinWidth(width);
        region.setMaxWidth(width);
    }

    /*
     * This method is used only be the TableWithFiles class but because it's a JavaFX's Control class it has a static
     * initializer that requires JavaFX environment and thus can't be used in unit tests.
     */
    /**
     * Returns the textual representation of the size.
     *
     * @param size the size to represent.
     * @param keepShort if set to true the result will be for example "100 KB" instead of "100.00 KB", "99.9 KB" instead
     *                  of "99.91 KB" and so on - the number of digits after the decimal point will be reduced depending
     *                  on the whole part so that in general there are no more than four symbols in the textual
     *                  representation (plus the size suffix). Otherwise there will be 2 digits after the point.
     * @return the textual representation of the size (for example 21.39 KB).
     */
    public static String getFileSizeTextual(long size, boolean keepShort) {
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
         * There are two border cases - when the whole part is 99 or 9. Because after adding fractional part and
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
}
