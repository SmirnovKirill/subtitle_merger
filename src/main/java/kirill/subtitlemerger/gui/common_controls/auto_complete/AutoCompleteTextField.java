package kirill.subtitlemerger.gui.common_controls.auto_complete;

import javafx.beans.NamedArg;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.util.StringConverter;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Our own implementation of a text field with auto-completion. The popup with suggested options (with a maximum of
 * maxItemsInPopup) is always displayed if the text field has focus. Once the focus is lost the entered value is
 * validated and if it's incorrect then the text field's value is reset to the previous validated one so it means that
 * once the correct value is set it's impossible to unset it.
 * It's possible to set a handler (valueSetHandler) that will be invoked
 * 1) each time the item is selected directly (with a keyboard or a mouse) even if the new value is equal to the
 * previous one;
 * 2) each time the focus is lost and the value in the text field is correct and differs from the previous value;
 * 3) each time the setText() is called and the text is correct, it doesn't have to be different from the previous
 * value.
 */
/*
 * At first I wanted to use a JavaFX's ComboBox with editable=true. But it turned out that it has a lot of unexpected
 * bugs and limitations - for example when the list is opened, ctrl+a and arrow keys stop working or when you press the
 * space key it closes down the opened list and so on. Then I though that maybe it's better to use a ListView in a popup
 * window but again, ListViews have their own problems:
 * 1) they don't shrink if there are few elements:
 * https://stackoverflow.com/questions/17429508/how-do-you-get-javafx-listview-to-be-the-height-of-its-items;
 * 2) when they are displayed ctrl+a and arrow keys stop working (and that's the reason why there is the exact same
 * problem with ComboBoxes since they use ListViews internally);
 * 3) if you try to bind the preferred width to the text field's width it will be a little bit wider.
 * So I've decided to make a popup with a ScrollPane inside that has a list of labels that change their text and
 * visibility dynamically.
 */
@CommonsLog
public class AutoCompleteTextField<T> extends TextField {
    private static final String LABELS_WRAPPER_CLASS = "labels-wrapper";

    private static final String UTILITY_LABEL_CLASS = "utility-label";

    private static final String LABEL_KEYBOARD_FOCUS_CLASS = "label-keyboard-focus";

    private AutoCompleteData<T> autoCompleteData;

    /**
     * Max height of the popup with items. If the value is not set or is way too big (>600 px), the default value
     * (200 px) is used.
     */
    private DoubleProperty maxPopupHeight;

    private StringProperty noMatchingItemsText;

    private ObjectProperty<Consumer<T>> valueSetHandler;

    private ScrollPane scrollPane;

    private Popup popup;

    /*
     * We listen to changes of the text property and it's important to separate cases when the property is modified from
     * the outside (not by the code from this class) from cases where we modify the text here internally.
     */
    private boolean textChangedInternally;

    /*
     * This variable stores the latest getBoundsInLocal() value when the text field is not focused. Because once it gets
     * focus it starts to have negative coordinates and bigger height because of the border and that will lead to
     * incorrect coordinates of the popup.
     */
    private Bounds boundInLocalNotFocused;

    /*
     * This variable indicates that the popup can be displayed (because the text field is focused) but at the same time
     * it shouldn't be displayed because there is not enough space (for example if the application window is moved so
     * that the left part of the text field is hidden). This variable will help to display the popup back once there is
     * once again enough space.
     */
    private boolean popupHasNoSpace;

    public AutoCompleteTextField(
            @NamedArg("maxItemsInPopup")
                    int maxItemsInPopup
    ) {
        autoCompleteData = new AutoCompleteData<>(maxItemsInPopup);
        maxPopupHeight = new SimpleDoubleProperty();
        noMatchingItemsText = new SimpleStringProperty();
        valueSetHandler = new SimpleObjectProperty<>();
        scrollPane = getScrollPane(maxPopupHeight, autoCompleteData, noMatchingItemsText, this::handleItemSelected);
        popup = getAutoCompletePopup(scrollPane);

        focusedProperty().addListener(observable -> handleFocusChanged(isFocused()));
        textProperty().addListener(observable -> handleTextChanged(getText()));
        setPopupDimensionsListeners();
        addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> handleKeyPressed(event, autoCompleteData, this::handleItemSelected, scrollPane)
        );
    }

    /**
     * This method is called when the user selects the item from the list directly (with a keyboard or a mouse).
     */
    private void handleItemSelected(T value) {
        autoCompleteData.setValue(value);

        textChangedInternally = true;
        setText(autoCompleteData.itemToString(value));
        textChangedInternally = false;
        positionCaret(getText().length());

        Consumer<T> valueSetHandler = getValueSetHandler();
        if (valueSetHandler != null) {
            valueSetHandler.accept(value);
        }
    }

    private static <T> ScrollPane getScrollPane(
            DoubleProperty maxPopupHeight,
            AutoCompleteData<T> autoCompleteData,
            StringProperty noMatchingItemsText,
            Consumer<T> selectValueHandler
    ) {
        ScrollPane result = new ScrollPane() {
            /*
             * It's better not to give the ability to focus for the following reasons:
             * 1) when the pane has focus the arrows keys stop working like with the focused ListViews;
             * 2) when the pane has focus all labels become blurry;
             * 3) there will be no need to write css rules to remove the blue focus borders.
             */
            @Override
            public void requestFocus() {
            }
        };
        result.setMinWidth(Region.USE_PREF_SIZE);
        result.setMaxWidth(Region.USE_PREF_SIZE);
        result.setMaxHeight(getMaxPopupHeight(maxPopupHeight));
        result.setFitToWidth(true); //So that the VBox fills all the pane, otherwise the background won't look nice.
        result.setFitToHeight(true); //The same reason as above.
        result.getStylesheets().add("/gui/javafx/common_controls/auto_complete_text_field.css");
        result.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        result.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        result.setContent(getLabelsWrapper(autoCompleteData, noMatchingItemsText, selectValueHandler));

        return result;
    }

    private static double getMaxPopupHeight(DoubleProperty maxPopupHeight) {
        double result = maxPopupHeight.get();
        if (result <= 0 || result > 600) {
            return 200;
        }

        return result;
    }

    private static<T> VBox getLabelsWrapper(
            AutoCompleteData<T> autoCompleteData,
            StringProperty noMatchingItemsText,
            Consumer<T> selectItemHandler
    ) {
        VBox result = new VBox();

        result.getStyleClass().add(LABELS_WRAPPER_CLASS);

        Label noMatchingItemsLabel = new Label();
        GuiUtils.bindVisibleAndManaged(noMatchingItemsLabel, autoCompleteData.noMatchingItemsProperty());
        noMatchingItemsLabel.getStyleClass().add(UTILITY_LABEL_CLASS);
        noMatchingItemsLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> ObjectUtils.firstNonNull(noMatchingItemsText.get(), "No items found"),
                        noMatchingItemsText
                )
        );
        result.getChildren().add(noMatchingItemsLabel);

        for (int i = 0; i < autoCompleteData.getLabelsInfo().size(); i++) {
            result.getChildren().add(getItemLabel(i, autoCompleteData, selectItemHandler));
        }

        Label moreItemsLabel = new Label();
        GuiUtils.bindVisibleAndManaged(moreItemsLabel, autoCompleteData.moreItemsCountProperty().greaterThan(0));
        moreItemsLabel.getStyleClass().add(UTILITY_LABEL_CLASS);
        moreItemsLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> autoCompleteData.getMoreItemsCount() + " more...",
                        autoCompleteData.moreItemsCountProperty()
                )
        );

        result.getChildren().add(moreItemsLabel);

        return result;
    }

    private static <T> Label getItemLabel(int i, AutoCompleteData<T> autoCompleteData, Consumer<T> selectItemHandler) {
        Label result = new Label();

        AutoCompleteData.LabelInfo<T> labelInfo = autoCompleteData.getLabelsInfo().get(i);

        result.setOnMouseClicked(event -> selectItemHandler.accept(labelInfo.getValue()));
        result.setMaxWidth(Double.MAX_VALUE);
        GuiUtils.bindVisibleAndManaged(result, labelInfo.visibleProperty());
        labelInfo.keyboardFocusProperty().addListener(observable -> {
            if (!labelInfo.isKeyboardFocus()) {
                result.getStyleClass().remove(LABEL_KEYBOARD_FOCUS_CLASS);
            } else {
                result.getStyleClass().add(LABEL_KEYBOARD_FOCUS_CLASS);
            }
        });
        result.textProperty().bind(labelInfo.textProperty());

        return result;
    }

    private static Popup getAutoCompletePopup(ScrollPane scrollPane) {
        Popup result = new Popup();

        /*
         * I've discovered this flag after a long fight with popups. I'm not 100% sure but as far as I understand the
         * problem is this - when you try to set popup's x coordinate directly,
         * javafx.stage.PopupWindow.updateWindow(PopupWindow.java:784) in JavaFX 14.0.1 sets the width from the cached
         * value first. I don't know whether it's possible to force an update of that value but the regular setWidth()
         * doesn't help (nor does the setWidth() of the content pane). Anyway, the value is taken from the cache, and
         * the value can be really big if for example the application window has been maximized before. And a big width
         * prevent the x coordinate to be set to the desired value because JavaFX thinks that with this width and this
         * x coordinate the popup will exceed the screen, which is true for the cached width but isn't for the width
         * calculated by the application. This flag disables that check and everything starts to work fine.
         */
        result.setAutoFix(false);
        result.getContent().add(scrollPane);

        return result;
    }

    private void handleFocusChanged(boolean focused) {
        if (focused) {
            setPopupDimensions();
            autoCompleteData.searchAndUpdate(getText());
            popup.show(getScene().getWindow());
        } else {
            popupHasNoSpace = false;
            popup.hide();

            T validatedValue = autoCompleteData.getValidatedValue(getText());
            if (validatedValue == null) {
                textChangedInternally = true;
                setText(autoCompleteData.itemToString(autoCompleteData.getValue()));
                textChangedInternally = false;
            } else if (!Objects.equals(validatedValue, autoCompleteData.getValue())) {
                autoCompleteData.setValue(validatedValue);

                Consumer<T> valueSetHandler = getValueSetHandler();
                if (valueSetHandler != null) {
                    valueSetHandler.accept(validatedValue);
                }
            }
        }
    }

    private void setPopupDimensions() {
        /* It can happen only during the application start. */
        if (boundInLocalNotFocused == null) {
            return;
        }

        /*
         * I guess it would be more correct to use getBoundsInParent().getHeight() instead of
         * boundInLocalNotFocused.getHeight() but then we would have to listen to the changes of these bounds as well
         * and they are changed a lot, plus there shouldn't be any transformations that happen when calculating the
         * bounds in parent.
         */
        double newX = localToScreen(boundInLocalNotFocused).getMinX();
        double newY = localToScreen(boundInLocalNotFocused).getMinY() + boundInLocalNotFocused.getHeight();

        double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        double newWidth = Math.min(getWidth(), screenWidth - newX);
        double maxPopupHeight = getMaxPopupHeight(maxPopupHeightProperty());

        if (newX < 0 || newWidth == 0 || newY < 0 || newY + maxPopupHeight > screenHeight) {
            if (!popupHasNoSpace) {
                popup.hide();
                popupHasNoSpace = true;
            }

            return;
        }

        if (popupHasNoSpace) {
            popupHasNoSpace = false;
            popup.show(getScene().getWindow());
        }

        scrollPane.setPrefWidth(newWidth);
        popup.setX(newX);
        popup.setY(newY);
    }

    private void handleTextChanged(String text) {
        if (isFocused()) {
            autoCompleteData.searchAndUpdate(text);
        } else {
            if (textChangedInternally) {
                return;
            }

            T validatedValue = autoCompleteData.getValidatedValue(text);
            if (validatedValue == null) {
                textChangedInternally = true;
                setText(autoCompleteData.itemToString(autoCompleteData.getValue()));
                textChangedInternally = false;
            } else {
                /*
                 * Note that the condition to enter this block is different from one in handleFocusChanged(). That's
                 * because if the text was modified explicitly outside this class (by calling setText() for example)
                 * it's better to call the callback even if the new value is the same as the previous one. But it's
                 * better not to do so in cases with losing focus because otherwise each time the focus is lost the
                 * callback is called and that's not right.
                 */
                autoCompleteData.setValue(validatedValue);

                Consumer<T> valueSetHandler = getValueSetHandler();
                if (valueSetHandler != null) {
                    valueSetHandler.accept(validatedValue);
                }
            }
        }
    }

    /**
     * This method calls the setPopupDimensions() each time the text field is redrawn or moved. It also listens to
     * boundsInLocalProperty() changes.
     */
    private void setPopupDimensionsListeners() {
        /*
         * It has to be a change listener, not an invalidation listener, because isNeedsLayout() will not make the
         * property valid since it doesn't actually work with the property.
         */
        needsLayoutProperty().addListener((observable, oldNeedsLayout, needsLayout) -> {
            if (needsLayout) {
                setPopupDimensions();
            }
        });

        /*
         * We can start to listen to changes only once the scene is set.
         */
        ChangeListener<Number> windowMovedListener = (observable, oldCoordinate, coordinate) -> setPopupDimensions();
        sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.getWindow().xProperty().removeListener(windowMovedListener);
                oldValue.getWindow().yProperty().removeListener(windowMovedListener);
            }

            if (newValue != null) {
                newValue.getWindow().xProperty().addListener(windowMovedListener);
                newValue.getWindow().yProperty().addListener(windowMovedListener);
            }
        });

        boundsInLocalProperty().addListener((observable, oldBounds, bounds) -> {
            if (!isFocused()) {
                boundInLocalNotFocused = bounds;
            }
        });
    }

    private static <T> void handleKeyPressed(
            KeyEvent event,
            AutoCompleteData<T> autoCompleteData,
            Consumer<T> selectItemHandler,
            ScrollPane scrollPane
    ) {
        if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
            if (autoCompleteData.isNoMatchingItems()) {
                return;
            }

            List<Node> scrollPaneNodes = ((Parent) scrollPane.getContent()).getChildrenUnmodifiable();
            if (event.getCode() == KeyCode.UP) {
                autoCompleteData.moveKeyboardFocusUp();

                int itemNodeIndex;
                if (autoCompleteData.getKeyboardFocusIndex() == null) {
                    /* 1 and not 0 because the first node is always a "no matching items" label. */
                    itemNodeIndex = 1;
                } else {
                    /* +1 because the first node is always a "no matching items" label. */
                    itemNodeIndex = autoCompleteData.getKeyboardFocusIndex() + 1;
                }

                ensureVisible(scrollPaneNodes.get(itemNodeIndex), scrollPane, KeyboardFocusDirection.UP);
            } else {
                autoCompleteData.moveKeyboardFocusDown();

                int itemNodeIndex;
                if (autoCompleteData.getKeyboardFocusIndex() == null) {
                    /*
                     * If the "more items" label is visible then we have to make sure it's visible, otherwise make sure
                     * that the list item is visible.
                     */
                    if (autoCompleteData.getMoreItemsCount() > 0) {
                        itemNodeIndex = scrollPaneNodes.size() - 1;
                    } else {
                        itemNodeIndex = scrollPaneNodes.size() - 2;
                    }
                } else {
                    /* +1 because the first node is always a "no matching items" label. */
                    itemNodeIndex = autoCompleteData.getKeyboardFocusIndex() + 1;
                }

                ensureVisible(scrollPaneNodes.get(itemNodeIndex), scrollPane, KeyboardFocusDirection.DOWN);
            }

            /* Otherwise the cursor will be jumping back and forth while the user is scrolling through the items. */
            event.consume();
        } else if (event.getCode() == KeyCode.LEFT) {
            /* Otherwise if the user presses the right arrow afterwards he or she will select an item. */
            autoCompleteData.unsetKeyboardFocus();
        } else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.ENTER) {
            Integer keyboardFocusIndex = autoCompleteData.getKeyboardFocusIndex();
            if (keyboardFocusIndex != null) {
                selectItemHandler.accept(autoCompleteData.getLabelsInfo().get(keyboardFocusIndex).getValue());
            }
        }
    }

    /**
     * This method makes sure that the specified node is visible on the scroll pane after navigating with the keyboard
     * arrows. The behaviour is different depending on which direction the user was moving (what arrow was pressed) - if
     * he or she wanted to move lower and the node is not visible then the scroll will be moved so that the node is at
     * the bottom of the view port, otherwise if the user wanted to move higher, the node will end up at the top of the
     * view port.
     */
    private static void ensureVisible(Node node, ScrollPane pane, KeyboardFocusDirection keyboardFocusDirection) {
        Bounds nodeBounds = node.getBoundsInParent();
        Bounds viewPortBounds = getViewportBounds(pane.getViewportBounds());

        /*
         * I could've added another condition - && !nodeBounds.contains(viewPortBounds) for cases where nodes are bigger
         * than the viewport. But there will be other problems with such nodes. So in general it's just easier to think
         * that there will be no such nodes.
         */
        if (!viewPortBounds.contains(nodeBounds)) {
            /*
             * I think that in order to use the scroll pane's setVvalue correctly it's easier to treat it as the ratio
             * of pixels we want to skip to the maximum amount of pixels we can skip for the current pane.
             */
            double maxPixelsToSkip = pane.getContent().getBoundsInParent().getHeight() - viewPortBounds.getHeight();

            if (keyboardFocusDirection == KeyboardFocusDirection.UP) {
                /* To display the node at the top of the viewport we need to skip all the pixels before the node. */
                double skipRatio = nodeBounds.getMinY() / maxPixelsToSkip;
                if (skipRatio > 1) {
                    skipRatio = 1;
                }

                pane.setVvalue(skipRatio);
            } else if (keyboardFocusDirection == KeyboardFocusDirection.DOWN) {
                /*
                 * We want the node to be displayed at the bottom of the view port and it's like displaying the next
                 * node at the top of the view port (thus nodeBounds.getMaxY()) and then moving the port up to its full
                 * height. Note that it's simply getMaxY(), not getMaxY() + 1, because geMaxY() is equal to the
                 * getMinY() of the next node.
                 */
                double skipRatio = (nodeBounds.getMaxY() - viewPortBounds.getHeight()) / maxPixelsToSkip;
                if (skipRatio < 0) {
                    skipRatio = 0;
                }

                pane.setVvalue(skipRatio);
            } else {
                log.error("unexpected keyboard focus direction: " + keyboardFocusDirection + ", most likely a bug");
                throw new IllegalStateException();
            }
        }
    }

    /**
     * This method converts the original view port bounds so that they have coordinates in the same system all other
     * elements are. It looks pretty complicated so here is an example - imagine that a scroll pane has a 500 px VBox as
     * its content and the view port's height is 100 px. Now imagine that the scroll bar is as low as possible meaning
     * the view port is showing the lowest 100 px of the VBox. It would make sense for the view port to have minY = 400
     * and maxY = 500, right? Instead, minY will be -400 and maxY will be -300. Why is that? Because in JavaFX 14.0.1
     * javafx.scene.control.skin.ScrollPaneSkin.updatePosY(ScrollPaneSkin.java:1190) has the following code:
     * double minY = Math.min((- posY / (vsb.getMax() - vsb.getMin()) * (nodeHeight - contentHeight)), 0);
     * So for some reason the y coordinate is always negative and is kind of reversed. This method fixes that and
     * returns bounds where the y coordinate is not negative.
     */
    private static Bounds getViewportBounds(Bounds originalBounds) {
        return new BoundingBox(
                originalBounds.getMinX(),
                -originalBounds.getMinY(),
                originalBounds.getWidth(),
                originalBounds.getHeight()
        );
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public double getMaxPopupHeight() {
        return maxPopupHeight.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public DoubleProperty maxPopupHeightProperty() {
        return maxPopupHeight;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setMaxPopupHeight(double maxPopupHeight) {
        this.maxPopupHeight.set(maxPopupHeight);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getNoMatchingItemsText() {
        return noMatchingItemsText.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty noMatchingItemsTextProperty() {
        return noMatchingItemsText;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setNoMatchingItemsText(String noMatchingItemsText) {
        this.noMatchingItemsText.set(noMatchingItemsText);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public List<T> getItems() {
        return autoCompleteData.getAllItems();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<List<T>> itemsProperty() {
        return autoCompleteData.allItemsProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setItems(List<T> items) {
        autoCompleteData.setAllItems(items);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringConverter<T> getConverter() {
        return autoCompleteData.getConverter();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<StringConverter<T>> converterProperty() {
        return autoCompleteData.converterProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setConverter(StringConverter<T> converter) {
        autoCompleteData.setConverter(converter);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public Consumer<T> getValueSetHandler() {
        return valueSetHandler.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<Consumer<T>> valueSetHandlerProperty() {
        return valueSetHandler;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setValueSetHandler(Consumer<T> valueSetHandler) {
        this.valueSetHandler.set(valueSetHandler);
    }

    private enum KeyboardFocusDirection {
        UP,
        DOWN
    }
}
