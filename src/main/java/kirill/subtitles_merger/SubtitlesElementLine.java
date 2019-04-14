package kirill.subtitles_merger;

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
}
