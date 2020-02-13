package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class GuiFfmpegSubtitleStream extends GuiSubtitleStream {
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

    public GuiFfmpegSubtitleStream(
            Integer size,
            boolean selectedAsUpper,
            boolean selectedAsLower,
            int ffmpegIndex,
            String unavailabilityReason,
            String failedToLoadReason,
            String language,
            String title,
            boolean extra
    ) {
        super(size, selectedAsUpper, selectedAsLower);

        this.ffmpegIndex = ffmpegIndex;
        this.unavailabilityReason = unavailabilityReason;
        this.failedToLoadReason = new SimpleStringProperty(failedToLoadReason);
        this.language = language;
        this.title = title;
        this.extra = extra;
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

    @Override
    public String getUniqueId() {
        return ffmpegIndex + "";
    }
}
