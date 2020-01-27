package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks.load_subtitles;

import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
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
            Ffmpeg ffmpeg
    ) {
        super(ffmpeg);

        this.ffmpegIndex = ffmpegIndex;
        this.fileInfo = fileInfo;
        this.guiFileInfo = guiFileInfo;
    }

    @Override
    protected Void call() {
        SubtitleStream subtitleStream = fileInfo.getSubtitleStreams().stream()
                .filter(stream -> stream.getFfmpegIndex() == ffmpegIndex)
                .findFirst().orElseThrow(IllegalStateException::new);
        allSubtitleCount = 1;
        loadedBeforeCount = subtitleStream.getSubtitles() != null ? 1 : 0;

        load(ffmpegIndex, Collections.singletonList(guiFileInfo), Collections.singletonList(fileInfo));

        return null;
    }
}
