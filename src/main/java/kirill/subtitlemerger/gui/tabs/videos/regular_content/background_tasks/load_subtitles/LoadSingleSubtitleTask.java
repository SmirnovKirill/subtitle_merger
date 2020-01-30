package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.load_subtitles;

import javafx.beans.property.BooleanProperty;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;

import java.util.Collections;
import java.util.List;

public class LoadSingleSubtitleTask extends LoadSubtitlesTask {
    private int ffmpegIndex;

    private FileInfo fileInfo;

    private GuiFileInfo guiFileInfo;

    private List<GuiFileInfo> displayedGuiFilesInfo;

    public LoadSingleSubtitleTask(
            int ffmpegIndex,
            FileInfo fileInfo,
            GuiFileInfo guiFileInfo,
            List<GuiFileInfo> displayedGuiFilesInfo,
            Ffmpeg ffmpeg,
            BooleanProperty cancelTaskPaneVisible
    ) {
        super(ffmpeg, cancelTaskPaneVisible);

        this.ffmpegIndex = ffmpegIndex;
        this.fileInfo = fileInfo;
        this.guiFileInfo = guiFileInfo;
        this.displayedGuiFilesInfo = displayedGuiFilesInfo;
    }

    @Override
    protected Void call() {
        BackgroundTask.clearState(displayedGuiFilesInfo, this);

        SubtitleStream subtitleStream = fileInfo.getSubtitleStreams().stream()
                .filter(stream -> stream.getFfmpegIndex() == ffmpegIndex)
                .findFirst().orElseThrow(IllegalStateException::new);
        allSubtitleCount = 1;
        loadedBeforeCount = subtitleStream.getSubtitles() != null ? 1 : 0;

        load(ffmpegIndex, Collections.singletonList(guiFileInfo), Collections.singletonList(fileInfo));

        return null;
    }
}
