package kirill.subtitlemerger.gui.forms.common.subtitle_preview;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.forms.common.BackgroundTaskFormController;
import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.utils.Utils;

import java.util.ArrayList;
import java.util.List;

class AbstractPreviewFormController extends BackgroundTaskFormController {
    private BooleanProperty linesTruncated;

    @FXML
    Label title;

    @FXML
    ListView<String> listView;

    Stage dialogStage;

    AbstractPreviewFormController() {
        linesTruncated = new SimpleBooleanProperty(false);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean isLinesTruncated() {
        return linesTruncated.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty linesTruncatedProperty() {
        return linesTruncated;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setLinesTruncated(boolean linesTruncated) {
        this.linesTruncated.set(linesTruncated);
    }

    static String getShortenedTitle(String title) {
        return Utils.getShortenedString(title, 0, 128);
    }

    static SplitText getSplitText(String text) {
        List<String> lines = new ArrayList<>();

        boolean linesTruncated = false;
        for (String line : LogicConstants.LINE_SEPARATOR_PATTERN.split(text)) {
            if (line.length() > 1000) {
                lines.add(line.substring(0, 1000));
                linesTruncated = true;
            } else {
                lines.add(line);
            }
        }

        return new SplitText(lines, linesTruncated);
    }

    /*
     * A special class created so that when the user clicks on the list view there are no errors. It does nothing and
     * that's the whole point.
     */
    static class NoSelectionModel<T> extends MultipleSelectionModel<T> {
        @Override
        public ObservableList<Integer> getSelectedIndices() {
            return FXCollections.emptyObservableList();
        }

        @Override
        public ObservableList<T> getSelectedItems() {
            return FXCollections.emptyObservableList();
        }

        @Override
        public void selectIndices(int index, int... indices) {}

        @Override
        public void selectAll() {}

        @Override
        public void selectFirst() {}

        @Override
        public void selectLast() {}

        @Override
        public void clearAndSelect(int index) {}

        @Override
        public void select(int index) {}

        @Override
        public void select(T obj) {}

        @Override
        public void clearSelection(int index) {}

        @Override
        public void clearSelection() {}

        @Override
        public boolean isSelected(int index) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void selectPrevious() {}

        @Override
        public void selectNext() {}
    }
}
