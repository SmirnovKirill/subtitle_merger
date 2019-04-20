package kirill.subtitles_merger;

import java.util.Objects;

public class SubtitlesElementLine {
    private String text;

    private String subtitlesOriginName;

    public SubtitlesElementLine(String text, String subtitlesOriginName) {
        this.text = text;
        this.subtitlesOriginName = subtitlesOriginName;
    }

    public String getText() {
        return text;
    }

    public String getSubtitlesOriginName() {
        return subtitlesOriginName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SubtitlesElementLine that = (SubtitlesElementLine) o;

        return Objects.equals(text, that.text) &&
                Objects.equals(subtitlesOriginName, that.subtitlesOriginName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, subtitlesOriginName);
    }
}
