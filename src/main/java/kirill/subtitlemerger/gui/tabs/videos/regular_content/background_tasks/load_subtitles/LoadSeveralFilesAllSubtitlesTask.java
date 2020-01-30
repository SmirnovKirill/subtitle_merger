package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.load_subtitles;

import javafx.beans.property.BooleanProperty;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

public class LoadSeveralFilesAllSubtitlesTask extends LoadSubtitlesTask {
    private List<FileInfo> allFilesInfo;

    private List<GuiFileInfo> displayedGuiFilesInfo;

    public LoadSeveralFilesAllSubtitlesTask(
            List<FileInfo> allFilesInfo,
            List<GuiFileInfo> displayedGuiFilesInfo,
            Ffmpeg ffmpeg,
            BooleanProperty cancelTaskPaneVisible
    ) {
        super(ffmpeg, cancelTaskPaneVisible);

        this.allFilesInfo = allFilesInfo;
        this.displayedGuiFilesInfo = displayedGuiFilesInfo;
    }

    @Override
    protected Void call() {
        BackgroundTask.clearState(displayedGuiFilesInfo, this);

        updateMessage("getting list of files to work with...");
        List<GuiFileInfo> guiFilesInfoToWorkWith = getGuiFilesInfoToWorkWith(displayedGuiFilesInfo);

        updateMessage("calculating number of subtitles to load...");
        allSubtitleCount = getAllSubtitleCount(guiFilesInfoToWorkWith, allFilesInfo);

        load(null, guiFilesInfoToWorkWith, allFilesInfo);

        return null;
    }

    private static List<GuiFileInfo> getGuiFilesInfoToWorkWith(List<GuiFileInfo> displayedGuiFilesInfo) {
        return displayedGuiFilesInfo.stream().filter(GuiFileInfo::isSelected).collect(Collectors.toList());
    }

    private static int getAllSubtitleCount(List<GuiFileInfo> guiFilesToWorkWith, List<FileInfo> allFiles) {
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
