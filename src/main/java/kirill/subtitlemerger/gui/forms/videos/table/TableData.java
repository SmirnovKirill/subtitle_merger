package kirill.subtitlemerger.gui.forms.videos.table;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class TableData {
    private TableMode mode;

    private List<TableVideo> videos;

    private int selectableCount;

    private int selectedAvailableCount;

    private int selectedUnavailableCount;

    private TableSortBy sortBy;

    private TableSortDirection sortDirection;
}
