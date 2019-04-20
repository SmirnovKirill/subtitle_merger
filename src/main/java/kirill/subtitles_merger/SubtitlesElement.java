package kirill.subtitles_merger;

import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

public class SubtitlesElement {
    private int number;

    private LocalTime from;

    private LocalTime to;

    private List<SubtitlesElementLine> lines = new ArrayList<>();

    public SubtitlesElement() {
    }

    public SubtitlesElement(int number, LocalTime from, LocalTime to, List<SubtitlesElementLine> lines) {
        this.number = number;
        this.from = from;
        this.to = to;
        this.lines = lines;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public LocalTime getFrom() {
        return from;
    }

    public void setFrom(LocalTime from) {
        this.from = from;
    }

    public LocalTime getTo() {
        return to;
    }

    public void setTo(LocalTime to) {
        this.to = to;
    }

    public List<SubtitlesElementLine> getLines() {
        return lines;
    }

    public void setLines(List<SubtitlesElementLine> lines) {
        this.lines = lines;
    }
}
