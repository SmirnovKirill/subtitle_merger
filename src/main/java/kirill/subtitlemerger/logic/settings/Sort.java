package kirill.subtitlemerger.logic.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Sort {
    private SortBy sortBy;

    private SortDirection sortDirection;
}
