package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

public abstract class GuiSubtitleStream {
    public static final int UNKNOWN_SIZE = -1;

    @Getter
    @Setter(value = AccessLevel.PROTECTED)
    private String id;

    private IntegerProperty size;

    private BooleanProperty selectedAsUpper;

    private BooleanProperty selectedAsLower;

    public GuiSubtitleStream(
            String id,
            Integer size,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        this.id = id;
        this.size = new SimpleIntegerProperty(size != null ? size : UNKNOWN_SIZE);
        this.selectedAsUpper = new SimpleBooleanProperty(selectedAsUpper);
        this.selectedAsLower = new SimpleBooleanProperty(selectedAsLower);
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

    public static <T extends GuiSubtitleStream> T getById(String id, List<T> guiStreams) {
        return guiStreams.stream()
                .filter(stream -> Objects.equals(stream.getId(), id))
                .findFirst().orElseThrow(IllegalStateException::new);
    }
}
