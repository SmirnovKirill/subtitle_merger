package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import kirill.subtitlemerger.logic.work_with_files.entities.ExternalSubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.entities.FfmpegSubtitleStream;

import java.util.List;
import java.util.stream.Collectors;

public abstract class GuiSubtitleStream {
    public static final int UNKNOWN_SIZE = -1;

    private IntegerProperty size;

    private BooleanProperty selectedAsUpper;

    private BooleanProperty selectedAsLower;

    public GuiSubtitleStream(
            Integer size,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
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

    public abstract String getUniqueId();
}
