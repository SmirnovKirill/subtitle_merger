package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import kirill.subtitlemerger.gui.util.background.BackgroundRunner;
import kirill.subtitlemerger.gui.util.background.BackgroundRunnerManager;
import kirill.subtitlemerger.logic.core.SubtitleMerger;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.MergedSubtitleInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleOption;
import lombok.AllArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
public class MergedPreviewRunner implements BackgroundRunner<MergedSubtitleInfo> {
    private SubtitleOption upperOption;

    private SubtitleOption lowerOption;

    private FileInfo fileInfo;

    public MergedSubtitleInfo run(BackgroundRunnerManager runnerManager) {
        if (fileInfo.getMergedSubtitleInfo() != null) {
            if (mergedMatchesCurrentSelection(fileInfo.getMergedSubtitleInfo(), upperOption, lowerOption)) {
                return fileInfo.getMergedSubtitleInfo();
            }
        }

        runnerManager.updateMessage("merging subtitles...");
        Subtitles merged = SubtitleMerger.mergeSubtitles(upperOption.getSubtitles(), lowerOption.getSubtitles());

        return new MergedSubtitleInfo(
                merged,
                upperOption.getId(),
                upperOption.getEncoding(),
                lowerOption.getId(),
                lowerOption.getEncoding()
        );
    }

    private static boolean mergedMatchesCurrentSelection(
            MergedSubtitleInfo mergedSubtitleInfo,
            SubtitleOption upperOption,
            SubtitleOption lowerOption
    ) {
        if (!Objects.equals(mergedSubtitleInfo.getUpperOptionId(), upperOption.getId())) {
            return false;
        }

        if (!Objects.equals(mergedSubtitleInfo.getUpperEncoding(), upperOption.getEncoding())) {
            return false;
        }

        if (!Objects.equals(mergedSubtitleInfo.getLowerOptionId(), lowerOption.getId())) {
            return false;
        }

        if (!Objects.equals(mergedSubtitleInfo.getLowerEncoding(), lowerOption.getEncoding())) {
            return false;
        }

        return true;
    }
}
