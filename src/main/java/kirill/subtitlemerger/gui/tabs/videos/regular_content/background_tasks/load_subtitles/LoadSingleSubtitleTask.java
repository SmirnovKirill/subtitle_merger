package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.load_subtitles;

import javafx.beans.property.BooleanProperty;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;

import java.util.Collections;

public class LoadSingleSubtitleTask extends LoadSubtitlesTask {
    private int ffmpegIndex;

    private FileInfo fileInfo;

    private GuiFileInfo guiFileInfo;

    public LoadSingleSubtitleTask(
            int ffmpegIndex,
            FileInfo fileInfo,
            GuiFileInfo guiFileInfo,
            Ffmpeg ffmpeg,
            BooleanProperty cancelTaskPaneVisible
    ) {
        super(ffmpeg, cancelTaskPaneVisible);

        this.ffmpegIndex = ffmpegIndex;
        this.fileInfo = fileInfo;
        this.guiFileInfo = guiFileInfo;
    }

    @Override
    protected Void call() {
        allSubtitleCount = 1;

        load(ffmpegIndex, Collections.singletonList(guiFileInfo), Collections.singletonList(fileInfo));

        return null;
    }
}
