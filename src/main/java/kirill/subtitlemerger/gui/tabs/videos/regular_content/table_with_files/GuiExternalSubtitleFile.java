package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.*;

public class GuiExternalSubtitleFile {
    private StringProperty fileName;

    private IntegerProperty size;

    private BooleanProperty selectedAsUpper;

    private BooleanProperty selectedAsLower;

    public GuiExternalSubtitleFile() {
        this.fileName = new SimpleStringProperty(null);
        this.size = new SimpleIntegerProperty(-1);
        this.selectedAsUpper = new SimpleBooleanProperty(false);
        this.selectedAsLower = new SimpleBooleanProperty(false);
    }

    public String getFileName() {
        return fileName.get();
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
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
