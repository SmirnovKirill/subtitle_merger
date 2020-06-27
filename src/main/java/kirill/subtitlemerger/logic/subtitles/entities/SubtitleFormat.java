package kirill.subtitlemerger.logic.subtitles.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@Getter
public enum SubtitleFormat {
    SUB_RIP(Arrays.asList("subrip", "srt"), Collections.singletonList("srt")),
    SUB_STATION_ALPHA(Arrays.asList("ass", "ssa"), Arrays.asList("ass", "ssa"));

    private List<String> ffmpegCodecs;

    private List<String> extensions;
}
