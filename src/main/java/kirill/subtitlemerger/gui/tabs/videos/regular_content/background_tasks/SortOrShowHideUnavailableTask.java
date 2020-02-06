package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.beans.property.BooleanProperty;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;

import java.util.List;

public class SortOrShowHideUnavailableTask extends BackgroundTask<List<GuiFileInfo>> {
    private List<GuiFileInfo> allGuiFilesInfo;

    private boolean hideUnavailable;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    public SortOrShowHideUnavailableTask(
            List<GuiFileInfo> allGuiFilesInfo,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            BooleanProperty cancelTaskPaneVisible
    ) {
        super(cancelTaskPaneVisible);
        this.allGuiFilesInfo = allGuiFilesInfo;
        this.hideUnavailable = hideUnavailable;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    @Override
    protected List<GuiFileInfo> call() {
        return getFilesInfoToShow(
                allGuiFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                this
        );
    }
}
