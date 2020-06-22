package kirill.subtitlemerger.gui.utils;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.utils.entities.FormInfo;
import kirill.subtitlemerger.logic.utils.Utils;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@CommonsLog
public class GuiUtils {
    /**
     * Uses the FXML loader to load a form based on the provided fxml file.
     *
     * @param path the path to the fxml file.
     * @return a wrapper containing the root node and its controller.
     * @throws IllegalStateException if something goes wrong during the process.
     */
    public static FormInfo loadForm(String path) {
        FXMLLoader fxmlLoader = new FXMLLoader(GuiUtils.class.getResource(path));

        Parent node;
        try {
            node = fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml " + path + ": " + ExceptionUtils.getStackTrace(e) + ", most likely a bug");
            throw new IllegalStateException();
        } catch (ClassCastException e) {
            log.error("root object is not a node (not a parent to be more precise), most likely a bug");
            throw new IllegalStateException();
        }

        Object controller;
        try {
            controller = Objects.requireNonNull(fxmlLoader.getController());
        } catch (NullPointerException e) {
            log.error("controller is not set, most likely a bug");
            throw new IllegalStateException();
        } catch (ClassCastException e) {
            log.error("controller has an incorrect class, most likely a bug");
            throw new IllegalStateException();
        }

        return new FormInfo(node, controller);
    }

    /**
     * Uses the FXML loader to initialize a given control based on the provided fxml file.
     *
     * @param control the control to initialize
     * @param path the path to the fxml file. Note that it shouldn't contain a fx:controller inside because the
     *            controller for the control is the control itself.
     * @throws IllegalStateException if something goes wrong during the process.
     */
    public static void initializeControl(Object control, String path) {
        FXMLLoader fxmlLoader = new FXMLLoader(GuiUtils.class.getResource(path));

        fxmlLoader.setRoot(control);
        fxmlLoader.setController(control);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            log.error("failed to load fxml " + path + ": " + ExceptionUtils.getStackTrace(e) + ", most likely a bug");
            throw new IllegalStateException();
        } catch (ClassCastException e) {
            log.error("controller has an incorrect class, most likely a bug");
            throw new IllegalStateException();
        }
    }

    public static void setVisibleAndManaged(Node node, boolean value) {
        node.setVisible(value);
        node.setManaged(value);
    }

    public static void bindVisibleAndManaged(Node node, ObservableValue<? extends Boolean> value) {
        node.visibleProperty().bind(value);
        node.managedProperty().bind(value);
    }

    /**
     * Configures the text field so that the value processor is invoked each time the Enter button is pressed or the
     * focus is lost.
     */
    public static void setTextEnteredHandler(TextField textField, Consumer<String> valueProcessor) {
        textField.focusedProperty().addListener(observable -> {
            if (textField.isFocused()) {
                return;
            }

            valueProcessor.accept(textField.getText());
        });

        textField.setOnKeyPressed(keyEvent -> {
            if (!keyEvent.getCode().equals(KeyCode.ENTER)) {
                return;
            }

            valueProcessor.accept(textField.getText());
        });
    }

    public static Button getImageButton(String text, String imageUrl, int width, int height) {
        Button result = new Button(text);

        result.setGraphic(getImageView(imageUrl, width, height));
        result.getStyleClass().add(GuiConstants.IMAGE_BUTTON_CLASS);
        result.setFocusTraversable(false);

        return result;
    }

    public static ImageView getImageView(String imageUrl, int width, int height) {
        ImageView result = new ImageView(new Image(imageUrl));

        result.setFitWidth(width);
        result.setFitHeight(height);

        return result;
    }

    /**
     * Generates a tooltip that is shown indefinitely and without delays.
     */
    public static Tooltip getTooltip(String text) {
        Tooltip result = new Tooltip(text);

        setTooltipProperties(result);

        return result;
    }

    private static void setTooltipProperties(Tooltip tooltip) {
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setShowDuration(Duration.INDEFINITE);
    }

    /**
     * Generates a tooltip that is shown indefinitely and without delays.
     */
    public static Tooltip getTooltip(ObservableValue<? extends String> text) {
        Tooltip result = new Tooltip();

        result.textProperty().bind(text);
        setTooltipProperties(result);

        return result;
    }

    public static Region getFixedWidthSpacer(int width) {
        Region result = new Region();

        setFixedWidth(result, width);

        return result;
    }

    public static void setFixedWidth(Region region, int width) {
        region.setMinWidth(width);
        region.setMaxWidth(width);
    }

    public static Region getFixedHeightSpacer(int height) {
        Region result = new Region();

        result.setMinHeight(height);
        result.setMaxHeight(height);

        return result;
    }

    /**
     * This method is helpful for displaying English texts.
     *
     * @param count the observable value with the number of items
     * @param oneItemText the text to return when there is only one item, this text can't use any format arguments
     *                   because there is always only one item
     * @param zeroOrSeveralItemsText the text to return when there are zero or several items, this text can use the
     *                               format argument %d inside
     * @return a binding that returns the text depending on the count.
     */
    /*
     * This method is placed here and not in the regular Utils class because it uses observable values that are part of
     * JavaFX and thus gui-specific.
     */
    public static StringBinding getTextDependingOnCount(
            IntegerExpression count,
            String oneItemText,
            String zeroOrSeveralItemsText
    ) {
        return Bindings.createStringBinding(
                () -> Utils.getTextDependingOnCount(count.getValue(), oneItemText, zeroOrSeveralItemsText),
                count
        );
    }
}
