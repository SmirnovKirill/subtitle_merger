package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import kirill.subtitlemerger.gui.core.entities.MultiPartResult;
import lombok.AccessLevel;
import lombok.Getter;
import org.joda.time.LocalDateTime;

import java.util.List;
import java.util.stream.Collectors;

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
    private BooleanProperty haveSubtitleSizesToLoad;

    private int subtitleToHideCount;

    @Getter(AccessLevel.NONE)
    private BooleanProperty someSubtitlesHidden;

    @Getter(AccessLevel.NONE)
    private ObjectProperty<MultiPartResult> result;

    private List<GuiSubtitleStream> subtitleStreams;

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
        this.haveSubtitleSizesToLoad = new SimpleBooleanProperty(haveSubtitleSizesToLoad);
        this.subtitleToHideCount = subtitleToHideCount;
        this.someSubtitlesHidden = new SimpleBooleanProperty(someSubtitlesHidden);
        this.result = new SimpleObjectProperty<>();
        this.subtitleStreams = subtitleStreams;
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

    public boolean isSomeSubtitlesHidden() {
        return someSubtitlesHidden.get();
    }

    public BooleanProperty someSubtitlesHiddenProperty() {
        return someSubtitlesHidden;
    }

    public void setSomeSubtitlesHidden(boolean someSubtitlesHidden) {
        this.someSubtitlesHidden.set(someSubtitlesHidden);
    }

    public MultiPartResult getResult() {
        return result.get();
    }

    public ObjectProperty<MultiPartResult> resultProperty() {
        return result;
    }

    public void setResult(MultiPartResult result) {
        this.result.set(result);
    }

    public void clearResult() {
        setResult(MultiPartResult.EMPTY);
    }

    public void setResultOnlySuccess(String text) {
        setResult(MultiPartResult.onlySuccess(text));
    }

    public void setResultOnlyWarn(String text) {
        setResult(MultiPartResult.onlyWarn(text));
    }

    public void setResultOnlyError(String text) {
        setResult(MultiPartResult.onlyError(text));
    }

    public List<GuiFfmpegSubtitleStream> getFfmpegSubtitleStreams() {
        return subtitleStreams.stream()
                .filter(stream -> stream instanceof GuiFfmpegSubtitleStream)
                .map(GuiFfmpegSubtitleStream.class::cast)
                .collect(Collectors.toList());
    }

    public List<GuiExternalSubtitleStream> getExternalSubtitleStreams() {
        return subtitleStreams.stream()
                .filter(stream -> stream instanceof GuiExternalSubtitleStream)
                .map(GuiExternalSubtitleStream.class::cast)
                .collect(Collectors.toList());
    }

    public void setExternalSubtitleStream(int index, String id, String filename, int size, boolean correctFormat) {
        GuiExternalSubtitleStream stream = getExternalSubtitleStreams().get(index);

        stream.setId(id);
        stream.setFileName(filename);
        stream.setSize(size);
        stream.setCorrectFormat(correctFormat);
    }

    public void unsetExternalSubtitleStream(String id) {
        GuiExternalSubtitleStream stream = GuiSubtitleStream.getById(id, getExternalSubtitleStreams());

        stream.setId(null);
        stream.setFileName(null);
        stream.setSize(GuiSubtitleStream.UNKNOWN_SIZE);
        stream.setCorrectFormat(false);
    }
}
