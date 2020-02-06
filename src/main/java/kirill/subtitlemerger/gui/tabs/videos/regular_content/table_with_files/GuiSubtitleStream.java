package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class GuiSubtitleStream {
    public static final int SIZE_NOT_SET = -1;

    /*
     * The word "ffmpeg" here emphasizes the fact that it's not a regular index, but an index got from ffmpeg.
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
            Integer size,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        this.ffmpegIndex = ffmpegIndex;
        this.unavailabilityReason = unavailabilityReason;
        this.failedToLoadReason = new SimpleStringProperty(failedToLoadReason);
        this.language = language;
        this.title = title;
        this.extra = extra;
        this.size = new SimpleIntegerProperty(size != null ? size : SIZE_NOT_SET);
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

    public String getUnavailabilityReason() {
        return unavailabilityReason;
    }

    public void setUnavailabilityReason(String unavailabilityReason) {
        this.unavailabilityReason = unavailabilityReason;
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
}
