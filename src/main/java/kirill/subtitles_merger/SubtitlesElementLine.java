package kirill.subtitles_merger;

import java.util.Objects;

public class SubtitlesElementLine {
    private String text;

    private String source;

    public SubtitlesElementLine(String text, String source) {
        this.text = text;
        this.source = source;
    }

    public String getText() {
        return text;
    }

    public String getSource() {
        return source;
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
                Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, source);
    }
}
