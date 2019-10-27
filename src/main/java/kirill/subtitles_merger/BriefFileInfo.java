package kirill.subtitles_merger;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

/**
 * This class contains basic information about the file, basically all information we can get without using ffmpeg
 * (because it takes much time). In particular this class contains the list with subtitles streams and their
 * information taken with ffprobe.
 */
@AllArgsConstructor
@Getter
class BriefFileInfo {
    private File file;

    /**
     * We will keep track of all files from the selected directory even if they can't be used for subtitles merging
     * (for better diagnostics).
     * Enum contains the reason why this file can't be used for subtitles merging.
     */
    private BriefFileUnavailabilityReason unavailabilityReason;

    private VideoFormat videoContainer;

    private List<BriefSubtitlesStreamInfo> subtitlesStreams;
}
