package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class GuiSubtitleStream {
    /*
     * The word "ffmpeg" there emphasizes the fact that it's not a regular index, but an index got from ffmpeg.
     * For example the first subtitle stream may have index 2 because the first two indices are assigned to the video
     * and audio streams.
     */
    private int ffmpegIndex;

    private String unavailabilityReason;

    @Setter
    private String failedToLoadReason;

    private String language;

    private String title;

    private boolean extra;

    @Setter
    private Integer size;

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
            Integer size,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        this.ffmpegIndex = ffmpegIndex;
        this.unavailabilityReason = unavailabilityReason;
        this.failedToLoadReason = failedToLoadReason;
        this.language = language;
        this.title = title;
        this.extra = extra;
        this.size = size;
        this.selectedAsUpper = new SimpleBooleanProperty(selectedAsUpper);
        this.selectedAsLower = new SimpleBooleanProperty(selectedAsLower);
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
