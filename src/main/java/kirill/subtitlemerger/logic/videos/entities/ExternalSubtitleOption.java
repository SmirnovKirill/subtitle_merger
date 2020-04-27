package kirill.subtitlemerger.logic.videos.entities;

import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;
import lombok.Getter;

import java.io.File;

@Getter
public class ExternalSubtitleOption extends SubtitleOption {
    private File file;

    public ExternalSubtitleOption(
            File file,
            SubtitlesAndInput subtitlesAndInput,
            boolean selectedAsUpper,
            boolean selectedAsLower
    ) {
        super(
                "file-" + file.getAbsolutePath(),
                subtitlesAndInput,
                null,
                selectedAsUpper,
                selectedAsLower
        );

        this.file = file;
    }
}