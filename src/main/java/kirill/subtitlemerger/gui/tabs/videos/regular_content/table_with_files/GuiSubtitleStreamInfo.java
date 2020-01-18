package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class GuiSubtitleStreamInfo {
    private String unavailabilityReason;

    private String language;

    private String title;

    @Getter(AccessLevel.NONE)
    private IntegerProperty size;

    public GuiSubtitleStreamInfo(String unavailabilityReason, String language, String title, int size) {
        this.unavailabilityReason = unavailabilityReason;
        this.language = language;
        this.title = title;
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
