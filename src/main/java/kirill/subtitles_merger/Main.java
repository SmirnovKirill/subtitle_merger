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

import static java.util.stream.Collectors.toSet;

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
        FileUtils.writeStringToFile(new File(properties.getProperty(PATH_TO_MERGED_SUBTITLES_PROPERTY)), mergedSubtitles.toString());
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
        List<String> sortedSources = getSortedSources(upperSubtitles, lowerSubtitles);

        Subtitles result = makeInitialMerge(upperSubtitles, lowerSubtitles);
        result = getExtendedSubtitles(result, sortedSources);
        orderSubtitles(result, sortedSources);
        result = getCombinedSubtitles(result);

        return result;
    }

    private static List<String> getSortedSources(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        return Arrays.asList(
                upperSubtitles.getElements().get(0).getLines().get(0).getSubtitlesOriginName(),
                lowerSubtitles.getElements().get(0).getLines().get(0).getSubtitlesOriginName()
        );
    }

    //Самый первый и простой этап объединения - делам список всех упомянутых точек времени и на каждом отрезке смотрим есть ли текст
    //в объединяемых субтитров, если есть, то объединяем.
    private static Subtitles makeInitialMerge(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
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

    //Метод устраняет "скачки". Если субтитры в данном блоке времени есть только в одном источнике, нужно посмотреть соседние блоки чтобы
    //взять оттуда значение из другого источника.
    private static Subtitles getExtendedSubtitles(Subtitles mergedSubtitles, List<String> sortedSources) {
        Subtitles result = new Subtitles();

        int i = 0;
        for (SubtitlesElement subtitlesElement : mergedSubtitles.getElements()) {
            Set<String> sources = subtitlesElement.getLines().stream()
                    .map(SubtitlesElementLine::getSubtitlesOriginName)
                    .collect(toSet());

            if (sources.size() != 1 && sources.size() != 2) {
                throw new IllegalStateException();
            }

            boolean shouldExtend = sources.size() != 2 && (
                    linesStartToHaveSecondSource(subtitlesElement.getLines(), i, Direction.FORWARD, mergedSubtitles)
                            || linesStartToHaveSecondSource(subtitlesElement.getLines(), i, Direction.BACKWARD, mergedSubtitles)
            );

            if (!shouldExtend) {
                result.getElements().add(subtitlesElement);
                i++;
                continue;
            }

            SubtitlesElement postProcessedElement = new SubtitlesElement();
            postProcessedElement.setNumber(subtitlesElement.getNumber());
            postProcessedElement.setFrom(subtitlesElement.getFrom());
            postProcessedElement.setTo(subtitlesElement.getTo());
            postProcessedElement.getLines().addAll(subtitlesElement.getLines());

            String source = sources.iterator().next();
            String missingSource = sortedSources.stream()
                    .filter(currentSource -> !Objects.equals(currentSource, source))
                    .findFirst().orElseThrow(IllegalStateException::new);

            if (i == 0) {
                for (SubtitlesElement currentSubtitlesElement : mergedSubtitles.getElements()) {
                    List<SubtitlesElementLine> linesFromMissingSource = currentSubtitlesElement.getLines().stream()
                            .filter(currentElement -> Objects.equals(currentElement.getSubtitlesOriginName(), missingSource))
                            .collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(linesFromMissingSource)) {
                        postProcessedElement.getLines().addAll(linesFromMissingSource);
                        break;
                    }
                }
            } else {
                for (int j = i - 1; j >= 0; j--) {
                    List<SubtitlesElementLine> linesFromMissingSource = mergedSubtitles.getElements().get(j).getLines().stream()
                            .filter(currentElement -> Objects.equals(currentElement.getSubtitlesOriginName(), missingSource))
                            .collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(linesFromMissingSource)) {
                        postProcessedElement.getLines().addAll(linesFromMissingSource);
                        break;
                    }
                }
            }

            result.getElements().add(postProcessedElement);

            i++;
        }

        return result;
    }

    private static boolean linesStartToHaveSecondSource(List<SubtitlesElementLine> lines, int elementIndex, Direction direction, Subtitles subtitles) {
        if (elementIndex == subtitles.getElements().size() - 1) {
            return false; //Значит искомые строки были и так последними.
        }

        String linesSource = lines.get(0).getSubtitlesOriginName();

        List<Integer> range = new ArrayList<>();
        if (direction == Direction.FORWARD) {
            for (int i = elementIndex + 1; i < subtitles.getElements().size(); i++) {
                range.add(i);
            }
        } else if (direction == Direction.BACKWARD) {
            for (int i = elementIndex - 1; i >= 0; i--) {
                range.add(i);
            }
        } else {
            throw new IllegalStateException();
        }

        for (int i : range) {
            SubtitlesElement subtitlesElement = subtitles.getElements().get(i);

            Set<String> currentElementSources = subtitlesElement.getLines().stream().map(SubtitlesElementLine::getSubtitlesOriginName).collect(toSet());

            if (currentElementSources.size() == 1) {
                if (currentElementSources.iterator().next().equals(linesSource)) {
                    if (!Objects.equals(subtitlesElement.getLines(), lines)) {
                        return false; //Значит пошел новый текст из того же источника, поэтому исходный текст был до этого только в одном источнике.
                    }
                } else {
                    return false; //Если пошел текст из нового источника а из исходного при этом нет, значит исходный текст был до этого только в одном источнике.
                }
            } else {
                List<SubtitlesElementLine> linesFromOriginalSource = subtitlesElement.getLines().stream()
                        .filter(currentLine -> Objects.equals(currentLine.getSubtitlesOriginName(), linesSource))
                        .collect(Collectors.toList());
                return Objects.equals(linesFromOriginalSource, lines); //Если строки из того же источника равны исходным, то значит к исходному тексту
                //добавился новый язык и надо вернуть true (т.е. исходный текст не был один на всем протяжении), а иначе false, т.к. сменился текст и из исходного источника и
                //из нового.
            }
        }

        return false;
    }

    //Сортирует тексты каждого субтитра на основе сортированного списка источников
    private static void orderSubtitles(Subtitles mergedSubtitles, List<String> sortedSources) {
        for (SubtitlesElement subtitlesElement : mergedSubtitles.getElements()) {
            List<SubtitlesElementLine> orderedLines = new ArrayList<>();

            for (String source : sortedSources) {
                orderedLines.addAll(subtitlesElement.getLines().stream().filter(currentLine -> Objects.equals(currentLine.getSubtitlesOriginName(), source)).collect(Collectors.toList()));
            }

            subtitlesElement.setLines(orderedLines);
        }
    }

    //Метод объединяет повторяющиеся субтитры если они одинаковые и идут строго подряд.
    private static Subtitles getCombinedSubtitles(Subtitles mergedSubtitles) {
        Subtitles result = new Subtitles();

        int elementNumber = 1;

        SubtitlesElement currentStartElement = mergedSubtitles.getElements().get(0);
        LocalTime currentTo = mergedSubtitles.getElements().get(0).getTo();

        for (int i = 1; i < mergedSubtitles.getElements().size(); i++) {
            SubtitlesElement currentElement = mergedSubtitles.getElements().get(i);

            if (!Objects.equals(currentElement.getLines(), currentStartElement.getLines()) || !Objects.equals(currentTo, currentElement.getFrom())) {
                result.getElements().add(
                        new SubtitlesElement(
                                elementNumber++,
                                currentStartElement.getFrom(),
                                currentTo,
                                new ArrayList<>(currentStartElement.getLines())
                        )
                );

                currentStartElement = currentElement;
                currentTo = currentElement.getTo();

            } else {
                currentTo = currentElement.getTo();
            }
        }

        SubtitlesElement lastElement = mergedSubtitles.getElements().get(mergedSubtitles.getElements().size() - 1);

        if (!Objects.equals(lastElement.getLines(), currentStartElement.getLines()) || !Objects.equals(currentTo, lastElement.getFrom())) {
            result.getElements().add(
                    new SubtitlesElement(
                            elementNumber,
                            lastElement.getFrom(),
                            lastElement.getTo(),
                            new ArrayList<>(lastElement.getLines())
                    )
            );
        } else {
            result.getElements().add(
                    new SubtitlesElement(
                            elementNumber,
                            currentStartElement.getFrom(),
                            lastElement.getTo(),
                            new ArrayList<>(currentStartElement.getLines())
                    )
            );
        }

        return result;
    }

    private enum ParsingStage {
        HAVE_NOT_STARTED,
        PARSED_NUMBER,
        PARSED_DURATION,
        PARSED_FIRST_LINE,
    }

    private enum Direction {
        FORWARD,
        BACKWARD
    }
}