package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.IntegerProperty;
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

    @Getter
    @Setter
    private boolean selectedAsUpper;

    @Getter
    @Setter  private boolean selectedAsLower;

    public GuiSubtitleStream(
            String id,
            Integer size,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        this.id = id;
        this.size = new SimpleIntegerProperty(size != null ? size : UNKNOWN_SIZE);
        this.selectedAsUpper = selectedAsUpper;
        this.selectedAsLower = selectedAsLower;
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

    public static <T extends GuiSubtitleStream> T getById(String id, List<T> guiStreams) {
        return guiStreams.stream()
                .filter(stream -> Objects.equals(stream.getId(), id))
                .findFirst().orElseThrow(IllegalStateException::new);
    }
}
