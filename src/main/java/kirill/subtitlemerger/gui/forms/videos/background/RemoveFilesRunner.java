package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.forms.videos.table.TableData;
import kirill.subtitlemerger.gui.forms.videos.table.TableMode;
import kirill.subtitlemerger.gui.forms.videos.table.TableVideo;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.settings.Settings;
import kirill.subtitlemerger.logic.videos.entities.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class RemoveFilesRunner implements BackgroundRunner<RemoveFilesRunner.Result> {
    private List<Video> originalFilesInfo;

    private TableMode mode;

    private List<TableVideo> originalAllTableFilesInfo;

    private List<TableVideo> originalTableFilesToShowInfo;

    private Settings settings;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        List<String> selectedFileIds =getSelectedFileIds(originalTableFilesToShowInfo, backgroundManager);

        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Removing files...");

        List<Video> filesInfo = originalFilesInfo.stream()
                .filter(fileInfo -> !selectedFileIds.contains(fileInfo.getId()))
                .collect(Collectors.toList());
        List<TableVideo> allTableFilesInfo = originalAllTableFilesInfo.stream()
                .filter(fileInfo -> !selectedFileIds.contains(fileInfo.getId()))
                .collect(Collectors.toList());
        List<TableVideo> tableFilesToShowInfo = originalTableFilesToShowInfo.stream()
                .filter(fileInfo -> !selectedFileIds.contains(fileInfo.getId()))
                .collect(Collectors.toList());

        return new Result(
                originalFilesInfo.size() - filesInfo.size(),
                filesInfo,
                allTableFilesInfo,
                VideosBackgroundUtils.getTableData(
                        mode,
                        tableFilesToShowInfo,
                        settings.getSortBy(),
                        settings.getSortDirection(),
                        backgroundManager
                )
        );
    }

    private static List<String> getSelectedFileIds(
            List<TableVideo> filesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Getting list of files to remove...");

        return filesInfo.stream()
                .filter(TableVideo::isSelected)
                .map(TableVideo::getId)
                .collect(Collectors.toList());
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private int removedCount;

        private List<Video> filesInfo;

        private List<TableVideo> allTableFilesInfo;

        private TableData tableData;
    }
}
