package kirill.subtitlemerger.gui.application_specific.videos_tab.background_tasks;

import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.utils.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.application_specific.videos_tab.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;

import java.util.List;

class BackgroundTaskUtils {
    static void clearFileInfoResults(List<GuiFileInfo> filesInfo, BackgroundTask<?> task) {
        task.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        task.updateMessage("clearing state...");

        for (GuiFileInfo fileInfo : filesInfo) {
            Platform.runLater(fileInfo::clearResult);
        }
    }

    static String guiTextFrom(FfmpegException e) {
        if (e.getCode() == FfmpegException.Code.GENERAL_ERROR) {
            return "ffmpeg returned an error";
        }

        throw new IllegalStateException();
    }
}
