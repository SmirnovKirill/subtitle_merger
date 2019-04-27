package kirill.subtitles_merger;

import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

class SubtitlesElement {
    private int number;

    private LocalTime from;

    private LocalTime to;

    private List<SubtitlesElementLine> lines = new ArrayList<>();

    SubtitlesElement() {
    }

    SubtitlesElement(int number, LocalTime from, LocalTime to, List<SubtitlesElementLine> lines) {
        this.number = number;
        this.from = from;
        this.to = to;
        this.lines = lines;
    }

    int getNumber() {
        return number;
    }

    void setNumber(int number) {
        this.number = number;
    }

    LocalTime getFrom() {
        return from;
    }

    void setFrom(LocalTime from) {
        this.from = from;
    }

    LocalTime getTo() {
        return to;
    }

    void setTo(LocalTime to) {
        this.to = to;
    }

    List<SubtitlesElementLine> getLines() {
        return lines;
    }

    void setLines(List<SubtitlesElementLine> lines) {
        this.lines = lines;
    }
}
