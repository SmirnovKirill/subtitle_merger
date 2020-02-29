package kirill.subtitlemerger.gui.application_specific.videos_tab.background_tasks;

import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.utils.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFileInfo;

import java.util.List;
import java.util.function.Consumer;

public class SortOrShowHideUnavailableTask extends BackgroundTask<List<GuiFileInfo>> {
    private List<GuiFileInfo> allGuiFilesInfo;

    private boolean hideUnavailable;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    private Consumer<List<GuiFileInfo>> onFinish;

    public SortOrShowHideUnavailableTask(
            List<GuiFileInfo> allGuiFilesInfo,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            Consumer<List<GuiFileInfo>> onFinish
    ) {
        this.allGuiFilesInfo = allGuiFilesInfo;
        this.hideUnavailable = hideUnavailable;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.onFinish = onFinish;
    }

    @Override
    protected List<GuiFileInfo> run() {
        return LoadDirectoryFilesTask.getFilesInfoToShow(
                allGuiFilesInfo,
                hideUnavailable,
                sortBy,
                sortDirection,
                this
        );
    }

    @Override
    protected void onFinish(List<GuiFileInfo> result) {
        onFinish.accept(result);
    }
}
