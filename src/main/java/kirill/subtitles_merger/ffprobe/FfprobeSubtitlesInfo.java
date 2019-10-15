package kirill.subtitles_merger.ffprobe;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class FfprobeSubtitlesInfo {
    private List<FfpProbeSubtitleStream> ffpProbeSubtitleStreams;
}
