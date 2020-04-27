package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.gui.GuiConstants;
import kirill.subtitlemerger.gui.GuiContext;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableData;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableVideoInfo;
import kirill.subtitlemerger.gui.forms.videos.table_with_files.TableWithVideos;
import kirill.subtitlemerger.gui.utils.background.BackgroundManager;
import kirill.subtitlemerger.gui.utils.background.BackgroundRunner;
import kirill.subtitlemerger.logic.utils.Utils;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import kirill.subtitlemerger.logic.videos.entities.VideoInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
public class AddFilesRunner implements BackgroundRunner<AddFilesRunner.Result> {
    private List<VideoInfo> filesInfo;

    private List<File> filesToAdd;

    private List<TableVideoInfo> allTableFilesInfo;

    private boolean hideUnavailable;

    private GuiContext context;

    @Override
    public Result run(BackgroundManager backgroundManager) {
        List<VideoInfo> filesToAddInfo = VideoBackgroundUtils.getVideosInfo(
                filesToAdd,
                context.getFfprobe(),
                backgroundManager
        );
        removeAlreadyAdded(filesToAddInfo, filesInfo, backgroundManager);

        if (filesToAddInfo.size() + filesInfo.size() > GuiConstants.TABLE_FILE_LIMIT) {
            return new Result(
                    String.format("There will be too many files (>%d)", GuiConstants.TABLE_FILE_LIMIT),
                    filesToAdd.size(),
                    0,
                    null,
                    null,
                    null
            );
        }

        filesInfo.addAll(filesToAddInfo);

        List<TableVideoInfo> tableFilesToAddInfo = VideoBackgroundUtils.tableVideosInfoFrom(
                filesToAddInfo,
                true,
                true,
                context.getSettings(),
                backgroundManager
        );
        allTableFilesInfo.addAll(tableFilesToAddInfo);

        List<TableVideoInfo> filesToShowInfo = null;
        if (hideUnavailable) {
            filesToShowInfo = VideoBackgroundUtils.getOnlyAvailableFilesInfo(allTableFilesInfo, backgroundManager);
        }

        filesToShowInfo = VideoBackgroundUtils.getSortedVideosInfo(
                filesToShowInfo != null ? filesToShowInfo : allTableFilesInfo,
                context.getSettings().getSortBy(),
                context.getSettings().getSortDirection(),
                backgroundManager
        );

        return new Result(
                null,
                filesToAdd.size(),
                filesToAddInfo.size(),
                filesInfo,
                allTableFilesInfo,
                VideoBackgroundUtils.getTableData(
                        TableWithVideos.Mode.SEPARATE_FILES,
                        filesToShowInfo,
                        context.getSettings().getSortBy(),
                        context.getSettings().getSortDirection(),
                        backgroundManager
                )
        );
    }

    private static void removeAlreadyAdded(
            List<VideoInfo> filesToAddInfo,
            List<VideoInfo> allFilesInfo,
            BackgroundManager backgroundManager
    ) {
        backgroundManager.setIndeterminateProgress();
        backgroundManager.updateMessage("Removing already added files...");

        Iterator<VideoInfo> iterator = filesToAddInfo.iterator();

        while (iterator.hasNext()) {
            VideoInfo fileToAddInfo = iterator.next();

            boolean alreadyAdded = allFilesInfo.stream()
                    .anyMatch(fileInfo -> Objects.equals(fileInfo.getFile(), fileToAddInfo.getFile()));
            if (alreadyAdded) {
                iterator.remove();
            }
        }
    }

    public static ActionResult getActionResult(Result taskResult) {
        String success;

        int filesToAdd = taskResult.getFilesToAddCount();
        int actuallyAdded = taskResult.getActuallyAddedCount();

        if (actuallyAdded == 0) {
            success = Utils.getTextDependingOnCount(
                    filesToAdd,
                    "File has been added already",
                    "All %d files have been added already"
            );
        } else if (filesToAdd == actuallyAdded) {
            success = Utils.getTextDependingOnCount(
                    actuallyAdded,
                    "File has been added successfully",
                    "All %d files have been added successfully"
            );
        } else {
            success = Utils.getTextDependingOnCount(
                    actuallyAdded,
                    String.format("1/%d files has been added successfully, ", filesToAdd),
                    String.format("%%d/%d files have been added successfully, ", filesToAdd)
            );
            success += (filesToAdd - actuallyAdded) + "/" + filesToAdd + " added before";
        }

        return new ActionResult(success, null, null);
    }

    @AllArgsConstructor
    @Getter
    public static class Result {
        private String addFailedReason;

        private int filesToAddCount;

        private int actuallyAddedCount;

        private List<VideoInfo> filesInfo;

        private List<TableVideoInfo> allTableFilesInfo;

        private TableData tableData;
    }
}
