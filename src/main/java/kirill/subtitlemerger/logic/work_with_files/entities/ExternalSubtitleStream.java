package kirill.subtitlemerger.logic.work_with_files.entities;

import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.Getter;

import java.io.File;

@Getter
public class ExternalSubtitleStream extends SubtitleStream {
    private File file;

    public ExternalSubtitleStream(SubtitleCodec codec, Subtitles subtitles, File file) {
        super(codec, subtitles);

        this.file = file;
    }

    @Override
    public String getUniqueId() {
        return file.getAbsolutePath();
    }
}
