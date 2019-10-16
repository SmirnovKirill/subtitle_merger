package kirill.subtitles_merger;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class FullFileInfo {
    private BriefFileInfo briefInfo;

    private FullFileUnavailabilityReason unavailabilityReason;

    private List<FullSingleSubtitlesInfo> allSubtitles;
}
