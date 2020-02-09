package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.function.Consumer;

public class LoadSingleFileAllSubtitlesTask extends LoadSubtitlesTask {
    private FileInfo fileInfo;

    private GuiFileInfo guiFileInfo;

    public LoadSingleFileAllSubtitlesTask(
            FileInfo fileInfo,
            GuiFileInfo guiFileInfo,
            Ffmpeg ffmpeg,
            Consumer<Result> onFinish
    ) {
        super(ffmpeg, onFinish);

        this.fileInfo = fileInfo;
        this.guiFileInfo = guiFileInfo;
    }

    @Override
    protected Result run() {
        return load(
                getAllSubtitleCount(fileInfo),
                Collections.singletonList(guiFileInfo),
                Collections.singletonList(fileInfo)
        );
    }

    private static int getAllSubtitleCount(FileInfo fileInfo) {
        int result = 0;

        if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
            for (SubtitleStream subtitleStream : fileInfo.getSubtitleStreams()) {
                if (subtitleStream.getUnavailabilityReason() != null) {
                    continue;
                }

                result++;
            }
        }

        return result;
    }
}

