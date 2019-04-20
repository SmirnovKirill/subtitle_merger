package kirill.subtitles_merger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static final Log LOG = LogFactory.getLog(Main.class);

    private static final String PATH_TO_UPPER_SUBTITLES_PROPERTY = "pathToUpperSubtitles";

    private static final String PATH_TO_LOWER_SUBTITLES_PROPERTY = "pathToLowerSubtitles";

    private static final String PATH_TO_MERGED_SUBTITLES_PROPERTY = "pathToMergedSubtitles";

    public static void main(String[] args) throws IOException {
        Properties properties = getProperties();

        Subtitles upperSubtitles = parseSubtitles(properties.getProperty(PATH_TO_UPPER_SUBTITLES_PROPERTY), "upper");
        Subtitles lowerSubtitles = parseSubtitles(properties.getProperty(PATH_TO_LOWER_SUBTITLES_PROPERTY), "lower");

        Subtitles mergedSubtitles = mergeSubtitles(upperSubtitles, lowerSubtitles);
        writeSubTitlesToFile(mergedSubtitles, properties.getProperty(PATH_TO_MERGED_SUBTITLES_PROPERTY));
    }

    private static Properties getProperties() throws IOException {
        Properties result = new Properties();

        result.load(Main.class.getResourceAsStream("/config.properties"));

        return result;
    }

    private static Subtitles parseSubtitles(String path, String subtitlesName) throws IOException {
        if (!new File(path).exists()) {
            LOG.error("file " + path + " doesn't exist");
            throw new IllegalArgumentException();
        }

        return parseSubtitles(new FileInputStream(path), subtitlesName);
    }

    static Subtitles parseSubtitles(InputStream inputStream, String subtitlesName) throws IOException {
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

                currentElement.getLines().add(new SubtitlesElementLine(currentLine, subtitlesName));
                parsingStage = ParsingStage.PARSED_FIRST_LINE;
            } else {
                if (StringUtils.isBlank(currentLine)) {
                    result.getElements().add(currentElement);

                    currentElement = null;
                    parsingStage = ParsingStage.HAVE_NOT_STARTED;
                } else {
                    currentElement.getLines().add(new SubtitlesElementLine(currentLine, subtitlesName));
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

    static Subtitles mergeSubtitles(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        Subtitles result = new Subtitles();

        List<LocalTime> uniqueSortedPointsOfTime = getUniqueSortedPointsOfTime(upperSubtitles, lowerSubtitles);

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
                }

                if (lowerElement != null) {
                    mergedElement.getLines().addAll(lowerElement.getLines());
                }

                result.getElements().add(mergedElement);
            }
        }

        return postProcessMergedSubtitles(result);
    }

    //Нужна пост обработка полученных субтитров, чтобы не было "скачков". Если субтитры в данном блоке времени есть только в одном источнике, нужно посмотреть соседние блоки чтобы
    //взять оттуда значение из другого источника.
    private static Subtitles postProcessMergedSubtitles(Subtitles mergedSubtitles) {
        Subtitles result = new Subtitles();

        //Важно сохранить порядок следования субтитров.
        Set<String> allSources = new LinkedHashSet<>();
        for (SubtitlesElement subtitlesElement : mergedSubtitles.getElements()) {
            Set<String> currentSource = subtitlesElement.getLines().stream()
                            .map(SubtitlesElementLine::getSubtitlesOriginName)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            if (currentSource.size() == 2) {
                allSources = currentSource;
                break;
            }
        }

        if (allSources.size() != 2) {
            throw new IllegalStateException();
        }

        int i = 0;
        for (SubtitlesElement subtitlesElement : mergedSubtitles.getElements()) {
            Set<String> sources = subtitlesElement.getLines().stream()
                    .map(SubtitlesElementLine::getSubtitlesOriginName)
                    .collect(Collectors.toSet());

            if (sources.size() == 2) {
                result.getElements().add(subtitlesElement);
                i++;
                continue;
            }

            if (sources.size() != 1) {
                throw new IllegalStateException();
            }

            SubtitlesElement postProcessedElement = new SubtitlesElement();
            postProcessedElement.setNumber(subtitlesElement.getNumber());
            postProcessedElement.setFrom(subtitlesElement.getFrom());
            postProcessedElement.setTo(subtitlesElement.getTo());

            List<SubtitlesElementLine> lines = new ArrayList<>();
            lines.addAll(subtitlesElement.getLines());

            String source = sources.iterator().next();
            String missingSource = allSources.stream()
                    .filter(currentSource -> !Objects.equals(currentSource, source))
                    .findFirst().orElseThrow(IllegalStateException::new);

            if (i == 0) {
                for (SubtitlesElement currentSubtitlesElement : mergedSubtitles.getElements()) {
                    List<SubtitlesElementLine> linesFromMissingSource = currentSubtitlesElement.getLines().stream()
                            .filter(currentElement -> Objects.equals(currentElement.getSubtitlesOriginName(), missingSource))
                            .collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(linesFromMissingSource)) {
                        lines.addAll(linesFromMissingSource);
                        break;
                    }
                }
            } else {
                for (int j = i - 1; j >= 0; j--) {
                    List<SubtitlesElementLine> linesFromMissingSource = mergedSubtitles.getElements().get(j).getLines().stream()
                            .filter(currentElement -> Objects.equals(currentElement.getSubtitlesOriginName(), missingSource))
                            .collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(linesFromMissingSource)) {
                        lines.addAll(linesFromMissingSource);
                        break;
                    }
                }
            }

            for (String currentSource : allSources) {
                postProcessedElement.getLines().addAll(lines.stream().filter(currentLine -> Objects.equals(currentLine.getSubtitlesOriginName(), currentSource)).collect(Collectors.toList()));
            }

            result.getElements().add(postProcessedElement);

            i++;
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

    private static void writeSubTitlesToFile(Subtitles mergedSubtitles, String path) throws IOException {
        StringBuilder result = new StringBuilder();

        for (SubtitlesElement subtitlesElement : mergedSubtitles.getElements()) {
            result.append(subtitlesElement.getNumber());
            result.append("\n");

            result.append(DateTimeFormat.forPattern("HH:mm:ss,SSS").print(subtitlesElement.getFrom()));
            result.append(" --> ");
            result.append(DateTimeFormat.forPattern("HH:mm:ss,SSS").print(subtitlesElement.getTo()));
            result.append("\n");

            for (SubtitlesElementLine line : subtitlesElement.getLines()) {
                result.append(line.getText());
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

