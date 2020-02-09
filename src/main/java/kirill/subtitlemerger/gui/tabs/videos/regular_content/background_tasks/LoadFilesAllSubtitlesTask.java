package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.scene.control.ProgressBar;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LoadFilesAllSubtitlesTask extends LoadSubtitlesTask {
    private List<FileInfo> allFilesInfo;

    private List<GuiFileInfo> displayedGuiFilesInfo;

    public LoadFilesAllSubtitlesTask(
            List<FileInfo> allFilesInfo,
            List<GuiFileInfo> displayedGuiFilesInfo,
            Ffmpeg ffmpeg,
            Consumer<Result> onFinish
    ) {
        super(ffmpeg, onFinish);

        this.allFilesInfo = allFilesInfo;
        this.displayedGuiFilesInfo = displayedGuiFilesInfo;
    }

    @Override
    protected Result run() {
        LoadDirectoryFilesTask.clearFileInfoResults(displayedGuiFilesInfo, this);

        List<GuiFileInfo> guiFilesInfoToWorkWith = getGuiFilesInfoToWorkWith(displayedGuiFilesInfo, this);

        return load(getAllSubtitleCount(guiFilesInfoToWorkWith, allFilesInfo, this), guiFilesInfoToWorkWith, allFilesInfo);
    }

    private static List<GuiFileInfo> getGuiFilesInfoToWorkWith(
            List<GuiFileInfo> displayedGuiFilesInfo,
            LoadFilesAllSubtitlesTask task
    ) {
        task.updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);
        task.updateMessage("getting list of files to work with...");

        return displayedGuiFilesInfo.stream().filter(GuiFileInfo::isSelected).collect(Collectors.toList());
    }

    private static int getAllSubtitleCount(
            List<GuiFileInfo> guiFilesToWorkWith,
            List<FileInfo> allFiles,
            LoadFilesAllSubtitlesTask task
    ) {
        task.updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);
        task.updateMessage("calculating number of subtitles to load...");

        int result = 0;

        for (GuiFileInfo guiFileToWorkWith : guiFilesToWorkWith) {
            FileInfo fileToWorkWith = RegularContentController.findMatchingFileInfo(guiFileToWorkWith, allFiles);
            if (!CollectionUtils.isEmpty(fileToWorkWith.getSubtitleStreams())) {
                for (SubtitleStream subtitleStream : fileToWorkWith.getSubtitleStreams()) {
                    if (subtitleStream.getUnavailabilityReason() != null) {
                        continue;
                    }

                    result++;
                }
            }
        }

        return result;
    }
}
