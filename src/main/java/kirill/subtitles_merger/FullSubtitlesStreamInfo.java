package kirill.subtitles_merger;

import kirill.subtitles_merger.logic.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
class FullSubtitlesStreamInfo {
    private BriefSubtitlesStreamInfo briefInfo;

    private FullSingleSubtitlesUnavailabilityReason unavailabilityReason;

    private Subtitles content;
}
