package kirill.subtitlesmerger.logic.merge_in_files.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public
class FullFileInfo {
    private BriefFileInfo briefInfo;

    private UnavailabilityReason unavailabilityReason;

    private List<FullSubtitlesStreamInfo> subtitlesStreams;

    public enum UnavailabilityReason {
        FAILED_BEFORE, // means that an error has happened before, at previous stage when we were obtaining brief info
        FFMPEG_FAILED
    }
}
