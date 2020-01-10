package kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files;

import javafx.scene.Node;
import javafx.scene.control.TableCell;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TableWithFilesCell<T> extends TableCell<GuiFileInfo, T> {
    private CellNodeGenerator cellNodeGenerator;

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        GuiFileInfo fileInfo = getTableRow().getItem();

        if (empty || fileInfo == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        setGraphic(cellNodeGenerator.generateNode(fileInfo));
        setText(null);
    }

    @FunctionalInterface
    interface CellNodeGenerator {
        Node generateNode(GuiFileInfo fileInfo);
    }
}