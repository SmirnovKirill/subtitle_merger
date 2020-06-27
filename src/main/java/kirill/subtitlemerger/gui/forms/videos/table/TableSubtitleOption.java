package kirill.subtitlemerger.gui.forms.videos.table;

import javafx.beans.property.*;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;

@CommonsLog
public class TableSubtitleOption {
    /* I had to make this constant instead of just using null because JavaFX's wrappers can hold only primitives. */
    static final int UNKNOWN_SIZE = -1;

    @Getter
    private String id;

    @Getter
    private TableVideo video;

    @Getter
    private TableSubtitleOptionType type;

    private ReadOnlyStringWrapper notValidReason;

    @Getter
    private String title;

    @Getter
    private boolean hideable;

    @Getter
    private boolean merged;

    private ReadOnlyIntegerWrapper size;

    private ReadOnlyStringWrapper failedToLoadReason;

    private BooleanProperty selectedAsUpper;

    private BooleanProperty selectedAsLower;

    private TableSubtitleOption(
            String id,
            TableVideo video,
            TableSubtitleOptionType type,
            String notValidReason,
            String title,
            boolean hideable,
            boolean merged,
            int size,
            String failedToLoadReason,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        this.id = id;
        this.video = video;
        this.type = type;
        this.notValidReason = new ReadOnlyStringWrapper(notValidReason);
        this.title = title;
        this.hideable = hideable;
        this.merged = merged;
        this.size = new ReadOnlyIntegerWrapper(size);
        this.failedToLoadReason = new ReadOnlyStringWrapper(failedToLoadReason);
        this.selectedAsUpper = new SimpleBooleanProperty(selectedAsUpper);
        this.selectedAsLower = new SimpleBooleanProperty(selectedAsLower);

        /*
         * All the selections' extra logic is implemented with listeners because there is no other way to ensure that
         * this code is invoked since the properties are exposed and can be modified from the outside.
         */
        this.selectedAsUpper.addListener((observable, oldValue, newValue) -> handleSelectedAsUpper(newValue, oldValue));
        this.selectedAsLower.addListener((observable, oldValue, newValue) -> handleSelectedAsLower(newValue, oldValue));
    }

    public static TableSubtitleOption createExternal(
            String id,
            TableVideo video,
            String notValidReason,
            String title,
            int size,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        return new TableSubtitleOption(
                id,
                video,
                TableSubtitleOptionType.EXTERNAL,
                notValidReason,
                title,
                false,
                false,
                size,
                null,
                selectedAsUpper,
                selectedAsLower
        );
    }

    public static TableSubtitleOption createBuiltIn(
            String id,
            TableVideo video,
            String notValidReason,
            String title,
            boolean hideable,
            boolean merged,
            Integer size,
            String failedToLoadReason,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        return new TableSubtitleOption(
                id,
                video,
                TableSubtitleOptionType.BUILT_IN,
                notValidReason,
                title,
                hideable,
                merged,
                ObjectUtils.firstNonNull(size, UNKNOWN_SIZE),
                failedToLoadReason,
                selectedAsUpper,
                selectedAsLower
        );
    }

    private void handleSelectedAsUpper(boolean selected, boolean previousValue) {
        if (selected == previousValue) {
            return;
        }

        if (selected) {
            for (TableSubtitleOption option : video.getOptions()) {
                if (Objects.equals(option.getId(), getId())) {
                    video.setUpperOption(option);
                    option.setSelectedAsLower(false);
                } else {
                    option.setSelectedAsUpper(false);
                }
            }
        } else {
            TableSubtitleOption upperOption = video.getUpperOption();
            if (upperOption != null && Objects.equals(upperOption.getId(), getId())) {
                video.setUpperOption(null);
            }
        }
    }

    private void handleSelectedAsLower(boolean selected, boolean previousValue) {
        if (selected == previousValue) {
            return;
        }

        if (selected) {
            for (TableSubtitleOption option : video.getOptions()) {
                if (Objects.equals(option.getId(), getId())) {
                    video.setLowerOption(option);
                    option.setSelectedAsUpper(false);
                } else {
                    option.setSelectedAsLower(false);
                }
            }
        } else {
            TableSubtitleOption lowerOption = video.getLowerOption();
            if (lowerOption != null && Objects.equals(lowerOption.getId(), getId())) {
                video.setLowerOption(null);
            }
        }
    }

    static TableSubtitleOption getById(String id, Collection<TableSubtitleOption> options) {
        TableSubtitleOption result = options.stream()
                .filter(option -> Objects.equals(option.getId(), id))
                .findFirst().orElse(null);
        if (result == null) {
            log.error("no table subtitle options with id " + id + ", most likely a bug");
            throw new IllegalStateException();
        }

        return result;
    }

    public void markAsValid() {
        notValidReason.set(null);
    }

    public void loadedSuccessfully(int size, String notValidReason) {
        if (type != TableSubtitleOptionType.BUILT_IN) {
            log.error("not a built-in subtitle option can't be loaded, most likely a bug");
            throw new IllegalStateException();
        }

        this.size.set(size);
        failedToLoadReason.set(null);
        if (!StringUtils.isBlank(notValidReason)) {
            this.notValidReason.set(notValidReason);
            if (isSelectedAsUpper()) {
                setSelectedAsUpper(false);
            }
            if (isSelectedAsLower()) {
                setSelectedAsLower(false);
            }
        }

        video.optionLoadedSuccessfully();
    }

    public void failedToLoad(String failedToLoadReason) {
        if (type != TableSubtitleOptionType.BUILT_IN) {
            log.error("not a built-in subtitle option can't be loaded, most likely a bug");
            throw new IllegalStateException();
        }

        size.set(UNKNOWN_SIZE);
        this.failedToLoadReason.set(failedToLoadReason);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getNotValidReason() {
        return notValidReason.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyStringProperty notValidReasonProperty() {
        return notValidReason.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public int getSize() {
        return size.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyIntegerProperty sizeProperty() {
        return size.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getFailedToLoadReason() {
        return failedToLoadReason.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyStringProperty failedToLoadReasonProperty() {
        return failedToLoadReason.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean isSelectedAsUpper() {
        return selectedAsUpper.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty selectedAsUpperProperty() {
        return selectedAsUpper;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setSelectedAsUpper(boolean selectedAsUpper) {
        this.selectedAsUpper.set(selectedAsUpper);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean isSelectedAsLower() {
        return selectedAsLower.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty selectedAsLowerProperty() {
        return selectedAsLower;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setSelectedAsLower(boolean selectedAsLower) {
        this.selectedAsLower.set(selectedAsLower);
    }
}
