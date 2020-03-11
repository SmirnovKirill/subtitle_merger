package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunnerManager;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class SortHideUnavailableRunner implements BackgroundRunner<TableFilesToShowInfo> {
    private List<TableFileInfo> allFilesInfo;

    private TableWithFiles.Mode mode;

    private boolean hideUnavailable;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    @Override
    public TableFilesToShowInfo run(BackgroundRunnerManager runnerManager) {
        List<TableFileInfo> filesToShowInfo = null;
        if (hideUnavailable) {
            filesToShowInfo = BackgroundHelperMethods.getOnlyAvailableFilesInfo(allFilesInfo, runnerManager);
        }

        filesToShowInfo = BackgroundHelperMethods.getSortedFilesInfo(
                filesToShowInfo != null ? filesToShowInfo : allFilesInfo,
                sortBy,
                sortDirection,
                runnerManager
        );

        return new TableFilesToShowInfo(
                filesToShowInfo,
                BackgroundHelperMethods.getAllSelectableCount(filesToShowInfo, mode, runnerManager),
                BackgroundHelperMethods.getSelectedAvailableCount(filesToShowInfo, runnerManager),
                BackgroundHelperMethods.getSelectedUnavailableCount(filesToShowInfo, runnerManager)
        );
    }
}
