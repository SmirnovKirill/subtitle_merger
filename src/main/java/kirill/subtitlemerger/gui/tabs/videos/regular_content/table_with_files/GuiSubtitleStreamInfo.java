package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class GuiSubtitleStreamInfo {
    private int id;

    private String unavailabilityReason;

    private String language;

    private String title;

    private boolean extra;

    @Getter(AccessLevel.NONE)
    private IntegerProperty size;

    @Getter(AccessLevel.NONE)
    private BooleanProperty selectedAsUpper;

    @Getter(AccessLevel.NONE)
    private BooleanProperty selectedAsLower;

    public GuiSubtitleStreamInfo(
            int id,
            String unavailabilityReason,
            String language,
            String title,
            boolean extra,
            int size,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        this.id = id;
        this.unavailabilityReason = unavailabilityReason;
        this.language = language;
        this.title = title;
        this.extra = extra;
        this.size = new SimpleIntegerProperty(size);
        this.selectedAsUpper = new SimpleBooleanProperty(selectedAsUpper);
        this.selectedAsLower = new SimpleBooleanProperty(selectedAsLower);
    }

    public int getSize() {
        return size.get();
    }

    public IntegerProperty sizeProperty() {
        return size;
    }

    public void setSize(int size) {
        this.size.set(size);
    }

    public boolean isSelectedAsUpper() {
        return selectedAsUpper.get();
    }

    public BooleanProperty selectedAsUpperProperty() {
        return selectedAsUpper;
    }

    public void setSelectedAsUpper(boolean selectedAsUpper) {
        this.selectedAsUpper.set(selectedAsUpper);
    }

    public boolean isSelectedAsLower() {
        return selectedAsLower.get();
    }

    public BooleanProperty selectedAsLowerProperty() {
        return selectedAsLower;
    }

    public void setSelectedAsLower(boolean selectedAsLower) {
        this.selectedAsLower.set(selectedAsLower);
    }
}
