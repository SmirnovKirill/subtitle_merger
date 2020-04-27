package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableData;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableVideoInfo;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableWithVideos;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class RemoveFilesRunner implements BackgroundRunner<RemoveFilesRunner.Result> {
    private List<VideoInfo> originalFilesInfo;

    private TableWithVideos.Mode mode;

    private List<TableVideoInfo> originalAllTableFilesInfo;

    private List<TableVideoInfo> originalTableFilesToShowInfo;

    private Settings settings;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        List<String> selectedFileIds =getSelectedFileIds(originalTableFilesToShowInfo, backgroundManager);

        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Removing files...");

        List<VideoInfo> filesInfo = originalFilesInfo.stream()
                .filter(fileInfo -> !selectedFileIds.contains(fileInfo.getId()))
                .collect(Collectors.toList());
        List<TableVideoInfo> allTableFilesInfo = originalAllTableFilesInfo.stream()
                .filter(fileInfo -> !selectedFileIds.contains(fileInfo.getId()))
                .collect(Collectors.toList());
        List<TableVideoInfo> tableFilesToShowInfo = originalTableFilesToShowInfo.stream()
                .filter(fileInfo -> !selectedFileIds.contains(fileInfo.getId()))
                .collect(Collectors.toList());

        return new Result(
                originalFilesInfo.size() - filesInfo.size(),
                filesInfo,
                allTableFilesInfo,
                VideoBackgroundUtils.getTableData(
                        mode,
                        tableFilesToShowInfo,
                        settings.getSortBy(),
                        settings.getSortDirection(),
                        backgroundManager
                )
        );
    }

    private static List<String> getSelectedFileIds(
            List<TableVideoInfo> filesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting list of files to remove...");

        return filesInfo.stream()
                .filter(TableVideoInfo::isSelected)
                .map(TableVideoInfo::getId)
                .collect(Collectors.toList());
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private int removedCount;

        private List<VideoInfo> filesInfo;

        private List<TableVideoInfo> allTableFilesInfo;

        private TableData tableData;
    }
}
