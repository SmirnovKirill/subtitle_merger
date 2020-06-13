package kirill.subtitlemerger.gui.common_controls.auto_complete;

import javafx.beans.property.*;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * A special class that works only with data, it was created to make the AutoCompleteTextField class easier by removing
 * the non-gui part (well, it's aware of the gui logic and how this class will be used but has no actual gui logic
 * inside).
 */
@CommonsLog
class AutoCompleteData<T> {
    private ObjectProperty<List<T>> allItems;

    private ObjectProperty<StringConverter<T>> converter;

    private int maxItemsCount;

    @Getter
    private List<LabelInfo<T>> labelsInfo;

    private BooleanProperty noMatchingItems;

    private IntegerProperty moreItemsCount;

    private Integer displayedItemsCount;

    @Getter
    private Integer keyboardFocusIndex;

    /**
     * Stores the current validated value.
     */
    @Getter
    @Setter
    private T value;

    AutoCompleteData(int maxItemsCount) {
        allItems = new SimpleObjectProperty<>();
        converter = new SimpleObjectProperty<>();
        this.maxItemsCount = maxItemsCount;

        labelsInfo = new ArrayList<>();
        for (int i = 0; i < maxItemsCount; i++) {
            labelsInfo.add(new LabelInfo<>());
        }

        noMatchingItems = new SimpleBooleanProperty();
        moreItemsCount = new SimpleIntegerProperty();
    }

    void searchAndUpdate(String text) {
        unsetKeyboardFocus();

        List<T> items = getMatchingItems(text);

        setNoMatchingItems(CollectionUtils.isEmpty(items));
        if (items.size() > maxItemsCount) {
            setMoreItemsCount(items.size() - maxItemsCount);
            items = items.subList(0, maxItemsCount);
        } else {
            setMoreItemsCount(0);
        }
        displayedItemsCount = items.size();

        for (int i = 0; i < this.labelsInfo.size(); i++) {
            LabelInfo<T> labelInfo = this.labelsInfo.get(i);
            if (i < items.size()) {
                labelInfo.setValue(items.get(i));
                labelInfo.setVisible(true);
                labelInfo.setText(itemToString(items.get(i)));
            } else {
                labelInfo.setValue(null);
                labelInfo.setVisible(false);
                labelInfo.setText(null);
            }
        }
    }

    void unsetKeyboardFocus() {
        if (keyboardFocusIndex == null) {
            return;
        }

        labelsInfo.get(keyboardFocusIndex).setKeyboardFocus(false);
        keyboardFocusIndex = null;
    }

    private List<T> getMatchingItems(String text) {
        List<T> items = getAllItems();
        if (CollectionUtils.isEmpty(items)) {
            return new ArrayList<>();
        }

        String processedText = text != null ? text.toLowerCase().trim() : "";
        if (StringUtils.isBlank(processedText)) {
            return new ArrayList<>(items);
        }

        return items.stream()
                .filter(item -> itemToString(item).toLowerCase().contains(processedText))
                .collect(toList());
    }

    String itemToString(T item) {
        StringConverter<T> converter = getConverter();
        if (converter == null) {
            return item.toString();
        } else {
            return ObjectUtils.firstNonNull(converter.toString(item), "");
        }
    }

    void moveKeyboardFocusUp() {
        if (isNoMatchingItems()) {
            log.error("can't move, there are no matching items, most likely a bug");
            throw new IllegalStateException();
        }

        if (keyboardFocusIndex == null) {
            labelsInfo.get(displayedItemsCount - 1).setKeyboardFocus(true);
            keyboardFocusIndex = displayedItemsCount - 1;
        } else if (keyboardFocusIndex == 0) {
            unsetKeyboardFocus();
        } else {
            labelsInfo.get(keyboardFocusIndex).setKeyboardFocus(false);
            labelsInfo.get(keyboardFocusIndex - 1).setKeyboardFocus(true);
            keyboardFocusIndex--;
        }
    }

    void moveKeyboardFocusDown() {
        if (isNoMatchingItems()) {
            log.error("can't move, there are no matching items, most likely a bug");
            throw new IllegalStateException();
        }

        if (keyboardFocusIndex == null) {
            labelsInfo.get(0).setKeyboardFocus(true);
            keyboardFocusIndex = 0;
        } else if (keyboardFocusIndex == displayedItemsCount - 1) {
            unsetKeyboardFocus();
        } else {
            labelsInfo.get(keyboardFocusIndex).setKeyboardFocus(false);
            labelsInfo.get(keyboardFocusIndex + 1).setKeyboardFocus(true);
            keyboardFocusIndex++;
        }
    }

    @Nullable
    T getValidatedValue(String text) {
        T result;
        StringConverter<T> converter = getConverter();
        if (converter == null) {
            return null;
        } else {
            result = converter.fromString(text);
        }

        List<T> allItems = getAllItems();
        if (allItems == null) {
            allItems = new ArrayList<>();
        }

        if (!allItems.contains(result)) {
            return null;
        }

        return result;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    List<T> getAllItems() {
        return allItems.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    ObjectProperty<List<T>> allItemsProperty() {
        return allItems;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    void setAllItems(List<T> allItems) {
        this.allItems.set(allItems);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    StringConverter<T> getConverter() {
        return converter.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    void setConverter(StringConverter<T> converter) {
        this.converter.set(converter);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    boolean isNoMatchingItems() {
        return noMatchingItems.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    BooleanProperty noMatchingItemsProperty() {
        return noMatchingItems;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    void setNoMatchingItems(boolean noMatchingItems) {
        this.noMatchingItems.set(noMatchingItems);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    int getMoreItemsCount() {
        return moreItemsCount.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    IntegerProperty moreItemsCountProperty() {
        return moreItemsCount;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    void setMoreItemsCount(int moreItemsCount) {
        this.moreItemsCount.set(moreItemsCount);
    }

    static class LabelInfo<T> {
        @Getter
        @Setter
        private T value;

        private BooleanProperty visible;

        private BooleanProperty keyboardFocus;

        private StringProperty text;

        LabelInfo() {
            visible = new SimpleBooleanProperty();
            keyboardFocus = new SimpleBooleanProperty();
            text = new SimpleStringProperty();
        }

        @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
        boolean isVisible() {
            return visible.get();
        }

        @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
        BooleanProperty visibleProperty() {
            return visible;
        }

        @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
        void setVisible(boolean visible) {
            this.visible.set(visible);
        }

        @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
        boolean isKeyboardFocus() {
            return keyboardFocus.get();
        }

        @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
        BooleanProperty keyboardFocusProperty() {
            return keyboardFocus;
        }

        @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
        void setKeyboardFocus(boolean keyboardFocus) {
            this.keyboardFocus.set(keyboardFocus);
        }

        @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
        String getText() {
            return text.get();
        }

        @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
        StringProperty textProperty() {
            return text;
        }

        @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
        void setText(String text) {
            this.text.set(text);
        }
    }
}
