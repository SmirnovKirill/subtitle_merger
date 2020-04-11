package kirill.subtitlemerger.logic.files.entities;

import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.Getter;

import java.io.File;
import java.nio.charset.Charset;

@Getter
public class FileWithSubtitles extends SubtitleOption {
    private File file;

    private byte[] rawData;

    public FileWithSubtitles(
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