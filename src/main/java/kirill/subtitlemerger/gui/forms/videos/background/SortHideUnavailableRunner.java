package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableFileInfo;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableWithFiles;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.settings.SortBy;
import kirill.subtitlemerger.logic.settings.SortDirection;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class SortHideUnavailableRunner implements BackgroundRunner<TableFilesToShowInfo> {
    private List<TableFileInfo> allFilesInfo;

    private TableWithFiles.Mode mode;

    private boolean hideUnavailable;

    private SortBy sortBy;

    private SortDirection sortDirection;

    @Override
    public TableFilesToShowInfo run(BackgroundManager backgroundManager) {
        List<TableFileInfo> filesToShowInfo = null;
        if (hideUnavailable) {
            filesToShowInfo = VideoTabBackgroundUtils.getOnlyAvailableFilesInfo(allFilesInfo, backgroundManager);
        }

        filesToShowInfo = VideoTabBackgroundUtils.getSortedFilesInfo(
                filesToShowInfo != null ? filesToShowInfo : allFilesInfo,
                sortBy,
                sortDirection,
                backgroundManager
        );

        return new TableFilesToShowInfo(
                filesToShowInfo,
                VideoTabBackgroundUtils.getAllSelectableCount(filesToShowInfo, mode, backgroundManager),
                VideoTabBackgroundUtils.getSelectedAvailableCount(filesToShowInfo, backgroundManager),
                VideoTabBackgroundUtils.getSelectedUnavailableCount(filesToShowInfo, backgroundManager)
        );
    }
}
