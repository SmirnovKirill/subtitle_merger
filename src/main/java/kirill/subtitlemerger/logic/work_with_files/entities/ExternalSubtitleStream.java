package kirill.subtitlemerger.logic.work_with_files.entities;

import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.nio.charset.Charset;

@Getter
public class ExternalSubtitleStream extends SubtitleStream {
    private File file;

    private byte[] rawData;

    @Setter
    private Charset encoding;

    public ExternalSubtitleStream(
            SubtitleCodec codec,
            Subtitles subtitles,
            File file,
            byte[] rawData,
            Charset encoding
    ) {
        super("external-" + file.getAbsolutePath(), codec, subtitles);

        this.file = file;
        this.rawData = rawData;
        this.encoding = encoding;
    }
}
