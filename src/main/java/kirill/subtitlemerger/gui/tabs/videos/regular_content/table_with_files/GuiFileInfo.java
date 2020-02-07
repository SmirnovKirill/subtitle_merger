package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.joda.time.LocalDateTime;

import java.util.Arrays;
import java.util.List;

@Getter
public class GuiFileInfo {
    private String pathToDisplay;

    private String fullPath;

    @Getter(AccessLevel.NONE)
    private BooleanProperty selected;

    private LocalDateTime lastModified;

    private long size;

    private String unavailabilityReason;

    @Getter(AccessLevel.NONE)
    private StringProperty errorMessage;

    @Getter(AccessLevel.NONE)
    private StringProperty successMessage;

    @Getter(AccessLevel.NONE)
    private BooleanProperty haveSubtitleSizesToLoad;

    @Getter(AccessLevel.NONE)
    private IntegerProperty subtitleToHideCount;

    @Getter(AccessLevel.NONE)
    private BooleanProperty someSubtitlesHidden;

    private List<GuiSubtitleStream> subtitleStreams;

    private List<GuiExternalSubtitleFile> externalSubtitleFiles;

    public GuiFileInfo(
            String pathToDisplay,
            String fullPath,
            boolean selected,
            LocalDateTime lastModified,
            long size,
            String unavailabilityReason,
            boolean haveSubtitleSizesToLoad,
            int subtitleToHideCount,
            boolean someSubtitlesHidden,
            List<GuiSubtitleStream> subtitleStreams

    ) {
        this.pathToDisplay = pathToDisplay;
        this.fullPath = fullPath;
        this.selected = new SimpleBooleanProperty(selected);
        this.lastModified = lastModified;
        this.size = size;
        this.unavailabilityReason = unavailabilityReason;
        this.errorMessage = new SimpleStringProperty();
        this.successMessage = new SimpleStringProperty();
        this.haveSubtitleSizesToLoad = new SimpleBooleanProperty(haveSubtitleSizesToLoad);
        this.subtitleToHideCount = new SimpleIntegerProperty(subtitleToHideCount);
        this.someSubtitlesHidden = new SimpleBooleanProperty(someSubtitlesHidden);
        this.subtitleStreams = subtitleStreams;
        this.externalSubtitleFiles = Arrays.asList(
                new GuiExternalSubtitleFile(),
                new GuiExternalSubtitleFile()
        );
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage.set(errorMessage);
    }

    public String getSuccessMessage() {
        return successMessage.get();
    }

    public StringProperty successMessageProperty() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage.set(successMessage);
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
