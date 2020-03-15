package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
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
            filesToShowInfo = VideoTabBackgroundUtils.getOnlyAvailableFilesInfo(allFilesInfo, runnerManager);
        }

        filesToShowInfo = VideoTabBackgroundUtils.getSortedFilesInfo(
                filesToShowInfo != null ? filesToShowInfo : allFilesInfo,
                sortBy,
                sortDirection,
                runnerManager
        );

        return new TableFilesToShowInfo(
                filesToShowInfo,
                VideoTabBackgroundUtils.getAllSelectableCount(filesToShowInfo, mode, runnerManager),
                VideoTabBackgroundUtils.getSelectedAvailableCount(filesToShowInfo, runnerManager),
                VideoTabBackgroundUtils.getSelectedUnavailableCount(filesToShowInfo, runnerManager)
        );
    }
}
