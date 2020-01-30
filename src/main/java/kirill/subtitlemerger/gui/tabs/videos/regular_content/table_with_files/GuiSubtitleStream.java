package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.*;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class GuiSubtitleStream {
    /*
     * The word "ffmpeg" there emphasizes the fact that it's not a regular index, but an index got from ffmpeg.
     * For example the first subtitle stream may have index 2 because the first two indices are assigned to the video
     * and audio streams.
     */
    private int ffmpegIndex;

    private String unavailabilityReason;

    @Getter(AccessLevel.NONE)
    private StringProperty failedToLoadReason;

    private String language;

    private String title;

    private boolean extra;

    @Getter(AccessLevel.NONE)
    private IntegerProperty size;

    @Getter(AccessLevel.NONE)
    private BooleanProperty selectedAsUpper;

    @Getter(AccessLevel.NONE)
    private BooleanProperty selectedAsLower;

    public GuiSubtitleStream(
            int ffmpegIndex,
            String unavailabilityReason,
            String failedToLoadReason,
            String language,
            String title,
            boolean extra,
            int size,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        this.ffmpegIndex = ffmpegIndex;
        this.unavailabilityReason = unavailabilityReason;
        this.failedToLoadReason = new SimpleStringProperty(failedToLoadReason);
        this.language = language;
        this.title = title;
        this.extra = extra;
        this.size = new SimpleIntegerProperty(size);
        this.selectedAsUpper = new SimpleBooleanProperty(selectedAsUpper);
        this.selectedAsLower = new SimpleBooleanProperty(selectedAsLower);
    }

    public String getFailedToLoadReason() {
        return failedToLoadReason.get();
    }

    public StringProperty failedToLoadReasonProperty() {
        return failedToLoadReason;
    }

    public void setFailedToLoadReason(String failedToLoadReason) {
        this.failedToLoadReason.set(failedToLoadReason);
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
