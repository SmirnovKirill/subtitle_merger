package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStreamInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;

import java.util.Collections;

public class LoadSingleSubtitleTask extends LoadSubtitlesTask {
    private int subtitleId;

    private FileInfo fileInfo;

    private GuiFileInfo guiFileInfo;

    public LoadSingleSubtitleTask(
            int subtitleId,
            FileInfo fileInfo,
            GuiFileInfo guiFileInfo,
            Ffmpeg ffmpeg
    ) {
        super(ffmpeg);

        this.subtitleId = subtitleId;
        this.fileInfo = fileInfo;
        this.guiFileInfo = guiFileInfo;
    }

    @Override
    protected Void call() {
        SubtitleStreamInfo subtitleStreamInfo = fileInfo.getSubtitleStreamsInfo().stream()
                .filter(stream -> stream.getId() == subtitleId)
                .findFirst().orElseThrow(IllegalStateException::new);
        allSubtitleCount = 1;
        loadedBeforeCount = subtitleStreamInfo.getSubtitles() != null ? 1 : 0;

        load(null, Collections.singletonList(guiFileInfo), Collections.singletonList(fileInfo));

        return null;
    }
}
