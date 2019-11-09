package kirill.subtitlesmerger.logic.data;

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
