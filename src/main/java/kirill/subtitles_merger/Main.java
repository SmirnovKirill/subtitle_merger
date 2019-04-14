package kirill.subtitles_merger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import java.io.*;
import java.util.*;

public class Main {
    private static final Log LOG = LogFactory.getLog(Main.class);

    private static final String PATH_TO_UPPER_SUBTITLES = "";

    private static final String PATH_TO_LOWER_SUBTITLES = "";

    private static final String PATH_TO_MERGED_SUBTITLES = "";

    public static void main(String[] args) throws IOException {
        Subtitles upperSubtitles = parseSubtitles(PATH_TO_UPPER_SUBTITLES);
        Subtitles lowerSubtitles = parseSubtitles(PATH_TO_LOWER_SUBTITLES);

        Subtitles mergedSubtitles = mergeSubtitles(upperSubtitles, lowerSubtitles);
        writeSubTitlesToFile(mergedSubtitles, PATH_TO_MERGED_SUBTITLES);
    }

    public static Subtitles parseSubtitles(String path) throws IOException {
        if (!new File(path).exists()) {
            LOG.error("file " + path + " doesn't exist");
            throw new IllegalArgumentException();
        }

        return parseSubtitles(new FileInputStream(path));
    }

    public static Subtitles parseSubtitles(InputStream inputStream) throws IOException {
        Subtitles result = new Subtitles();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        SubtitlesElement currentElement = null;
        ParsingStage parsingStage = ParsingStage.HAVE_NOT_STARTED;

        String currentLine;
        while ((currentLine = bufferedReader.readLine()) != null) {
            currentLine = currentLine.replace("\uFEFF", "").trim(); //Этот спец символ может быть в начале самой первой строки, надо убрать а то не распарсится инт

            if (parsingStage == ParsingStage.HAVE_NOT_STARTED) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                currentElement = new SubtitlesElement();
                currentElement.setNumber(Integer.parseInt(currentLine));
                parsingStage = ParsingStage.PARSED_NUMBER;
            } else if (parsingStage == ParsingStage.PARSED_NUMBER) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                String fromText = currentLine.substring(0, currentLine.indexOf("-")).trim();
                String toText = currentLine.substring(currentLine.indexOf(">") + 1).trim();

                currentElement.setFrom(DateTimeFormat.forPattern("HH:mm:ss,SSS").parseLocalTime(fromText));
                currentElement.setTo(DateTimeFormat.forPattern("HH:mm:ss,SSS").parseLocalTime(toText));

                parsingStage = ParsingStage.PARSED_DURATION;
            } else if (parsingStage == ParsingStage.PARSED_DURATION) {
                if (StringUtils.isBlank(currentLine)) {
                    continue;
                }

                currentElement.getLines().add(currentLine);
                parsingStage = ParsingStage.PARSED_FIRST_LINE;
            } else {
                if (StringUtils.isBlank(currentLine)) {
                    result.getElements().add(currentElement);

                    currentElement = null;
                    parsingStage = ParsingStage.HAVE_NOT_STARTED;
                } else {
                    currentElement.getLines().add(currentLine);
                }
            }
        }

        if (parsingStage != ParsingStage.HAVE_NOT_STARTED && parsingStage != ParsingStage.PARSED_FIRST_LINE) {
            throw new IllegalStateException();
        }

        if (parsingStage == ParsingStage.PARSED_FIRST_LINE) {
            result.getElements().add(currentElement);
        }

        return result;
    }

    public static Subtitles mergeSubtitles(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        Subtitles result = new Subtitles();

        List<LocalTime> uniqueSortedPointsOfTime = getUniqueSortedPointsOfTime(upperSubtitles, lowerSubtitles);

        SubtitlesElement previousUpperElement = null;
        SubtitlesElement previousLowerElement = null;
        int subtitleNumber = 1;

        for (int i = 0; i < uniqueSortedPointsOfTime.size() - 1; i++) {
            LocalTime from = uniqueSortedPointsOfTime.get(i);
            LocalTime to = uniqueSortedPointsOfTime.get(i + 1);

            SubtitlesElement upperElement = findElementForPeriod(from, to, upperSubtitles).orElse(null);
            SubtitlesElement lowerElement = findElementForPeriod(from, to, lowerSubtitles).orElse(null);
            if (upperElement != null || lowerElement != null) {
                SubtitlesElement mergedElement = new SubtitlesElement();

                mergedElement.setNumber(subtitleNumber++);
                mergedElement.setFrom(from);
                mergedElement.setTo(to);

                if (upperElement != null) {
                    mergedElement.getLines().addAll(upperElement.getLines());
                    previousUpperElement = upperElement;
                } else if (previousUpperElement != null) {
                    mergedElement.getLines().addAll(previousUpperElement.getLines());
                }

                if (lowerElement != null) {
                    mergedElement.getLines().addAll(lowerElement.getLines());
                    previousLowerElement = lowerElement;
                } else if (previousLowerElement != null) {
                    mergedElement.getLines().addAll(previousLowerElement.getLines());
                }

                result.getElements().add(mergedElement);
            }
        }

        return result;
    }

    //Возвращает уникальные отсортированные моменты времени когда показываются субтитры из обоих объектов с субтитрами.
    private static List<LocalTime> getUniqueSortedPointsOfTime(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        Set<LocalTime> result = new TreeSet<>();

        for (SubtitlesElement subtitleLine : upperSubtitles.getElements()) {
            result.add(subtitleLine.getFrom());
            result.add(subtitleLine.getTo());
        }

        for (SubtitlesElement subtitleLine : lowerSubtitles.getElements()) {
            result.add(subtitleLine.getFrom());
            result.add(subtitleLine.getTo());
        }

        return new ArrayList<>(result);
    }

    private static Optional<SubtitlesElement> findElementForPeriod(LocalTime from, LocalTime to, Subtitles subTitles) {
        for (SubtitlesElement subtitleLine : subTitles.getElements()) {
            boolean fromInside = !from.isBefore(subtitleLine.getFrom()) && !from.isAfter(subtitleLine.getTo());
            boolean toInside = !to.isBefore(subtitleLine.getFrom()) && !to.isAfter(subtitleLine.getTo());
            if (fromInside && toInside) {
                return Optional.of(subtitleLine);
            }
        }

        return Optional.empty();
    }

    public static void writeSubTitlesToFile(Subtitles mergedSubtitles, String path) throws IOException {
        StringBuilder result = new StringBuilder();

        for (SubtitlesElement subtitlesElement : mergedSubtitles.getElements()) {
            result.append(subtitlesElement.getNumber());
            result.append("\n");

            result.append(DateTimeFormat.forPattern("HH:mm:ss,SSS").print(subtitlesElement.getFrom()));
            result.append(" --> ");
            result.append(DateTimeFormat.forPattern("HH:mm:ss,SSS").print(subtitlesElement.getTo()));
            result.append("\n");

            for (String line : subtitlesElement.getLines()) {
                result.append(line);
                result.append("\n");
            }

            result.append("\n");
        }

        FileUtils.writeStringToFile(new File(path), result.toString());
    }

    private enum ParsingStage {
        HAVE_NOT_STARTED,
        PARSED_NUMBER,
        PARSED_DURATION,
        PARSED_FIRST_LINE,
    }
}

