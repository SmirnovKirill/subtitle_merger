package kirill.subtitlemerger.logic.videos.entities;

import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;

@AllArgsConstructor
@Getter
public class MergedSubtitleInfo {
    private SubtitlesAndOutput subtitlesAndOutput;

    private String upperOptionId;

    private Charset upperEncoding;

    private String lowerOptionId;

    private Charset lowerEncoding;
}
