package kirill.subtitlemerger.gui.forms.videos.table_with_files;

import javafx.beans.property.*;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Collection;
import java.util.Objects;

@CommonsLog
public class TableSubtitleOption {
    public static final int UNKNOWN_SIZE = -1;

    private StringProperty id;

    private StringProperty title;

    @Getter
    private boolean hideable;

    @Getter
    private boolean removable;

    @Getter
    private boolean sizeAlwaysKnown;

    private IntegerProperty size;

    private StringProperty failedToLoadReason;

    private StringProperty notValidReason;

    private BooleanProperty selectedAsUpper;

    private BooleanProperty selectedAsLower;

    @Getter
    private String format;

    public TableSubtitleOption(
            String id,
            String title,
            boolean hideable,
            boolean removable,
            boolean sizeAlwaysKnown,
            int size,
            String failedToLoadReason,
            String notValidReason,
            boolean selectedAsUpper,
            boolean selectedAsLower,
            String format
    ) {
        this.id = new SimpleStringProperty(id);
        this.title = new SimpleStringProperty(title);
        this.hideable = hideable;
        this.removable = removable;
        this.sizeAlwaysKnown = sizeAlwaysKnown;
        this.size = new SimpleIntegerProperty(size);
        this.failedToLoadReason = new SimpleStringProperty(failedToLoadReason);
        this.notValidReason = new SimpleStringProperty(notValidReason);
        this.selectedAsUpper = new SimpleBooleanProperty(selectedAsUpper);
        this.selectedAsLower = new SimpleBooleanProperty(selectedAsLower);
        this.format = format;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getId() {
        return id.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty idProperty() {
        return id;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setId(String id) {
        this.id.set(id);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getTitle() {
        return title.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty titleProperty() {
        return title;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setTitle(String title) {
        this.title.set(title);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public int getSize() {
        return size.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public IntegerProperty sizeProperty() {
        return size;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setSize(int size) {
        this.size.set(size);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getFailedToLoadReason() {
        return failedToLoadReason.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty failedToLoadReasonProperty() {
        return failedToLoadReason;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setFailedToLoadReason(String failedToLoadReason) {
        this.failedToLoadReason.set(failedToLoadReason);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getNotValidReason() {
        return notValidReason.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty notValidReasonProperty() {
        return notValidReason;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setNotValidReason(String notValidReason) {
        this.notValidReason.set(notValidReason);
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

    public static TableSubtitleOption getById(String id, Collection<TableSubtitleOption> subtitleOptions) {
        return subtitleOptions.stream()
                .filter(option -> Objects.equals(option.getId(), id))
                .findFirst().orElseThrow(IllegalStateException::new);
    }
}
