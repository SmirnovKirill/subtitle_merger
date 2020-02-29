package kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class GuiFfmpegSubtitleStream extends GuiSubtitleStream {
    private String unavailabilityReason;

    @Getter(AccessLevel.NONE)
    private StringProperty failedToLoadReason;

    private String language;

    private String title;

    private boolean extra;

    public GuiFfmpegSubtitleStream(
            String id,
            Integer size,
            boolean selectedAsUpper,
            boolean selectedAsLower,
            String unavailabilityReason,
            String failedToLoadReason,
            String language,
            String title,
            boolean extra
    ) {
        super(id, size, selectedAsUpper, selectedAsLower);

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
}
