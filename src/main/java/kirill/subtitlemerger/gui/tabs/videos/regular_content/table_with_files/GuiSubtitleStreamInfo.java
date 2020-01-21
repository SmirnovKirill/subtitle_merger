package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.IntegerProperty;
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

    public GuiSubtitleStreamInfo(
            int id,
            String unavailabilityReason,
            String language,
            String title,
            boolean extra,
            int size
    ) {
        this.id = id;
        this.unavailabilityReason = unavailabilityReason;
        this.language = language;
        this.title = title;
        this.extra = extra;
        this.size = new SimpleIntegerProperty(size);
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
}
