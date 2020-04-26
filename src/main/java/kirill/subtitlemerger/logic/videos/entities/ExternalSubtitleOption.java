package kirill.subtitlemerger.logic.videos.entities;

import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.Getter;

import java.io.File;
import java.nio.charset.Charset;

@Getter
public class ExternalSubtitleOption extends SubtitleOption {
    private File file;

    private byte[] rawData;

    public ExternalSubtitleOption(
            File file,
            Subtitles subtitles,
            int size,
            Charset encoding,
            boolean selectedAsUpper,
            boolean selectedAsLower,
            byte[] rawData
    ) {
        super(
                "file-" + file.getAbsolutePath(),
                subtitles,
                size,
                encoding,
                null,
                selectedAsUpper,
                selectedAsLower
        );

        this.file = file;
        this.rawData = rawData;
    }

    public void changeEncoding(Charset encoding, Subtitles subtitles) {
        this.encoding = encoding;
        this.subtitles = subtitles;
    }
}