package kirill.subtitlemerger.logic.files.entities;

import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;

@AllArgsConstructor
@Getter
public class MergedSubtitleInfo {
    private Subtitles subtitles;

    private String upperOptionId;

    private Charset upperEncoding;

    private String lowerOptionId;

    private Charset lowerEncoding;
}
