package kirill.subtitlemerger.gui.videos_tab.table_with_files;

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

    private ObjectProperty<UnavailabilityReason> unavailabilityReason;

    private BooleanProperty selectedAsUpper;

    private BooleanProperty selectedAsLower;

    public TableSubtitleOption(
            String id,
            String title,
            boolean hideable,
            boolean removable,
            boolean sizeAlwaysKnown,
            int size,
            String failedToLoadReason,
            UnavailabilityReason unavailabilityReason,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        this.id = new SimpleStringProperty(id);
        this.title = new SimpleStringProperty(title);
        this.hideable = hideable;
        this.removable = removable;
        this.sizeAlwaysKnown = sizeAlwaysKnown;
        this.size = new SimpleIntegerProperty(size);
        this.failedToLoadReason = new SimpleStringProperty(failedToLoadReason);
        this.unavailabilityReason = new SimpleObjectProperty<>(unavailabilityReason);
        this.selectedAsUpper = new SimpleBooleanProperty(selectedAsUpper);
        this.selectedAsLower = new SimpleBooleanProperty(selectedAsLower);
    }

    public String getId() {
        return id.get();
    }

    StringProperty idProperty() {
        return id;
    }

    void setId(String id) {
        this.id.set(id);
    }

    public String getTitle() {
        return title.get();
    }

    StringProperty titleProperty() {
        return title;
    }

    void setTitle(String title) {
        this.title.set(title);
    }

    public int getSize() {
        return size.get();
    }

    IntegerProperty sizeProperty() {
        return size;
    }

    void setSize(int size) {
        this.size.set(size);
    }

    public String getFailedToLoadReason() {
        return failedToLoadReason.get();
    }

    StringProperty failedToLoadReasonProperty() {
        return failedToLoadReason;
    }

    void setFailedToLoadReason(String failedToLoadReason) {
        this.failedToLoadReason.set(failedToLoadReason);
    }

    public UnavailabilityReason getUnavailabilityReason() {
        return unavailabilityReason.get();
    }

    ObjectProperty<UnavailabilityReason> unavailabilityReasonProperty() {
        return unavailabilityReason;
    }

    void setUnavailabilityReason(UnavailabilityReason unavailabilityReason) {
        this.unavailabilityReason.set(unavailabilityReason);
    }

    public boolean isSelectedAsUpper() {
        return selectedAsUpper.get();
    }

    BooleanProperty selectedAsUpperProperty() {
        return selectedAsUpper;
    }

    void setSelectedAsUpper(boolean selectedAsUpper) {
        this.selectedAsUpper.set(selectedAsUpper);
    }

    public boolean isSelectedAsLower() {
        return selectedAsLower.get();
    }

    BooleanProperty selectedAsLowerProperty() {
        return selectedAsLower;
    }

    void setSelectedAsLower(boolean selectedAsLower) {
        this.selectedAsLower.set(selectedAsLower);
    }

    public static TableSubtitleOption getById(String id, Collection<TableSubtitleOption> subtitleOptions) {
        return subtitleOptions.stream()
                .filter(option -> Objects.equals(option.getId(), id))
                .findFirst().orElseThrow(IllegalStateException::new);
    }

    //todo there shouldn't be any enums, gui should be unaware of the logic

    public enum UnavailabilityReason {
        NOT_ALLOWED_CODEC,
        INCORRECT_FORMAT
    }
}
