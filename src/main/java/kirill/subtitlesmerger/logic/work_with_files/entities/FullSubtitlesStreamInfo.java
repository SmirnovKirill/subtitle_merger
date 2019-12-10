package kirill.subtitlesmerger.logic.work_with_files.entities;

import kirill.subtitlesmerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public
class FullSubtitlesStreamInfo {
    private BriefSubtitlesStreamInfo briefInfo;

    private UnavailabilityReason unavailabilityReason;

    private Subtitles subtitles;

    public enum UnavailabilityReason {
        FAILED_BEFORE // means that an error has happened before, at previous stage when we were obtaining brief info
    }
}
