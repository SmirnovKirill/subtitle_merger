package kirill.subtitlemerger.gui.utils;

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
import java.util.Objects;
import java.util.function.Consumer;

@CommonsLog
public class GuiUtils {
    /**
     * Uses the FXML loader to load the form based on the provided fxml file.
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
     * Uses the FXML loader to initialize the given control based on the provided fxml file.
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
            log.error("failed to load fxml " + path + ": " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        } catch (ClassCastException e) {
            log.error("controller has an incorrect class");
            throw new IllegalStateException();
        }
    }

    public static Stage generatePopupStage(String title, Parent rootNode, Stage ownerStage) {
        Stage result = new Stage();

        result.initOwner(ownerStage);
        result.initModality(Modality.APPLICATION_MODAL);
        result.setTitle(title);
        result.setResizable(false);

        Scene scene = new Scene(rootNode);
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

    /**
     * A brief version of the agreement popup without an "apply to all" option.
     */
    public static boolean showAgreementPopup(String message, String yesText, String noText, Stage ownerStage) {
        AgreementResult result = showAgreementPopup(message, null, yesText, noText, ownerStage);
        return result == AgreementResult.YES;
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
}
