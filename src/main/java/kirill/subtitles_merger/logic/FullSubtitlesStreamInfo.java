package kirill.subtitles_merger.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public
class FullSubtitlesStreamInfo {
    private BriefSubtitlesStreamInfo briefInfo;

    private UnavailabilityReason unavailabilityReason;

    private Subtitles content;

    public enum UnavailabilityReason {
        FAILED_BEFORE // means that an error has happened before, at previous stage when we were obtaining brief info
    }
}
