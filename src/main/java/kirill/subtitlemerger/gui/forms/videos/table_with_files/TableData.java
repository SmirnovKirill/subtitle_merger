package kirill.subtitlemerger.gui.forms.videos.table_with_files;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class TableData {
    private TableWithVideos.Mode mode;

    private List<TableVideoInfo> videosInfo;

    private int allSelectableCount;

    private int selectedAvailableCount;

    private int selectedUnavailableCount;

    private TableSortBy sortBy;

    private TableSortDirection sortDirection;
}
