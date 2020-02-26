package kirill.subtitlemerger.logic.work_with_files.entities;

import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;

@AllArgsConstructor
@Getter
public class MergedSubtitleInfo {
    private Subtitles subtitles;

    private String upperStreamId;

    private Charset upperStreamEncoding;

    private String lowerStreamId;

    private Charset lowerStreamEncoding;
}
