package kirill.subtitlemerger.gui.tabs.videos.background;

import kirill.subtitlemerger.gui.tabs.videos.table_with_files.TableFileInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class TableFilesToShowInfo {
    private List<TableFileInfo> filesInfo;

    private int allSelectableCount;

    private int selectedAvailableCount;

    private int selectedUnavailableCount;
}
