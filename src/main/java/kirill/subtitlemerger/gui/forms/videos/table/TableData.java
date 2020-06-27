package kirill.subtitlemerger.gui.forms.videos.table;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class TableData {
    private List<TableVideo> videos;

    private TableMode mode;

    private int selectableCount;

    private int selectedAvailableCount;

    private int selectedUnavailableCount;

    private TableSortBy sortBy;

    private TableSortDirection sortDirection;
}
