package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

public class GuiExternalSubtitleStream extends GuiSubtitleStream {
    @Getter
    private int index;

    private StringProperty fileName;

    private BooleanProperty correctFormat;

    public GuiExternalSubtitleStream(int index) {
        super(GuiSubtitleStream.UNKNOWN_SIZE, false, false);

        this.index = index;
        this.fileName = new SimpleStringProperty(null);
        this.correctFormat = new SimpleBooleanProperty(false);
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

    public boolean isCorrectFormat() {
        return correctFormat.get();
    }

    public BooleanProperty correctFormatProperty() {
        return correctFormat;
    }

    public void setCorrectFormat(boolean correctFormat) {
        this.correctFormat.set(correctFormat);
    }

    @Override
    public String getUniqueId() {
        return getFileName();
    }
}
