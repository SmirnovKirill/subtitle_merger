package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.AccessLevel;
import lombok.Getter;
import org.joda.time.LocalDateTime;

import java.util.List;

@Getter
public class GuiFileInfo {
    private String pathToDisplay;

    private String fullPath;

    @Getter(AccessLevel.NONE)
    private BooleanProperty selected;

    private LocalDateTime lastModified;

    /*
     * Time when file was added to the table. Helps to keep the order when files are added after initial selection.
     */
    private LocalDateTime added;

    private long size;

    private String unavailabilityReason;

    private String error;

    private List<GuiSubtitleStreamInfo> subtitleStreamsInfo;

    public GuiFileInfo(
            String pathToDisplay,
            String fullPath,
            boolean selected,
            LocalDateTime lastModified,
            LocalDateTime added,
            long size,
            String unavailabilityReason,
            String error,
            List<GuiSubtitleStreamInfo> subtitleStreamsInfo
    ) {
        this.pathToDisplay = pathToDisplay;
        this.fullPath = fullPath;
        this.selected = new SimpleBooleanProperty(selected);
        this.lastModified = lastModified;
        this.added = added;
        this.size = size;
        this.unavailabilityReason = unavailabilityReason;
        this.error = error;
        this.subtitleStreamsInfo = subtitleStreamsInfo;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
}
