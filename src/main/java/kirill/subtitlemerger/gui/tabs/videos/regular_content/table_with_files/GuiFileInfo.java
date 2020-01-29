package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.AccessLevel;
import lombok.Getter;
import org.joda.time.LocalDateTime;

import java.util.List;

@Getter
public class GuiFileInfo {
    private String pathToDisplay;

    private String fullPath;

    @Getter(AccessLevel.NONE)
    private BooleanProperty errorFrame;

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

    @Getter(AccessLevel.NONE)
    private BooleanProperty haveSubtitleSizesToLoad;

    @Getter(AccessLevel.NONE)
    private IntegerProperty subtitleToHideCount;

    @Getter(AccessLevel.NONE)
    private BooleanProperty someSubtitlesHidden;

    private List<GuiSubtitleStream> subtitleStreams;

    public GuiFileInfo(
            String pathToDisplay,
            String fullPath,
            boolean errorFrame,
            boolean selected,
            LocalDateTime lastModified,
            LocalDateTime added,
            long size,
            String unavailabilityReason,
            String error,
            boolean haveSubtitleSizesToLoad,
            int subtitleToHideCount,
            boolean someSubtitlesHidden,
            List<GuiSubtitleStream> subtitleStreams
    ) {
        this.pathToDisplay = pathToDisplay;
        this.fullPath = fullPath;
        this.errorFrame = new SimpleBooleanProperty(errorFrame);
        this.selected = new SimpleBooleanProperty(selected);
        this.lastModified = lastModified;
        this.added = added;
        this.size = size;
        this.unavailabilityReason = unavailabilityReason;
        this.error = error;
        this.haveSubtitleSizesToLoad = new SimpleBooleanProperty(haveSubtitleSizesToLoad);
        this.subtitleToHideCount = new SimpleIntegerProperty(subtitleToHideCount);
        this.someSubtitlesHidden = new SimpleBooleanProperty(someSubtitlesHidden);
        this.subtitleStreams = subtitleStreams;
    }

    public boolean isErrorFrame() {
        return errorFrame.get();
    }

    public BooleanProperty errorFrameProperty() {
        return errorFrame;
    }

    public void setErrorFrame(boolean errorFrame) {
        this.errorFrame.set(errorFrame);
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

    public boolean isHaveSubtitleSizesToLoad() {
        return haveSubtitleSizesToLoad.get();
    }

    public BooleanProperty haveSubtitleSizesToLoadProperty() {
        return haveSubtitleSizesToLoad;
    }

    public void setHaveSubtitleSizesToLoad(boolean haveSubtitleSizesToLoad) {
        this.haveSubtitleSizesToLoad.set(haveSubtitleSizesToLoad);
    }

    public int getSubtitleToHideCount() {
        return subtitleToHideCount.get();
    }

    public IntegerProperty subtitleToHideCountProperty() {
        return subtitleToHideCount;
    }

    public void setSubtitleToHideCount(int subtitleToHideCount) {
        this.subtitleToHideCount.set(subtitleToHideCount);
    }

    public boolean isSomeSubtitlesHidden() {
        return someSubtitlesHidden.get();
    }

    public BooleanProperty someSubtitlesHiddenProperty() {
        return someSubtitlesHidden;
    }

    public void setSomeSubtitlesHidden(boolean someSubtitlesHidden) {
        this.someSubtitlesHidden.set(someSubtitlesHidden);
    }
}
