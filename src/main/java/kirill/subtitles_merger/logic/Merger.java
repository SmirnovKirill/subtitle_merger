package kirill.subtitles_merger.logic;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.LocalTime;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@CommonsLog
public class Merger {
    public static Subtitles mergeSubtitles(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        List<String> sortedSources = getSortedSources(upperSubtitles, lowerSubtitles);

        Subtitles result = makeInitialMerge(upperSubtitles, lowerSubtitles);
        result = getExtendedSubtitles(result, sortedSources);
        sortSubtitleLines(result, sortedSources);
        result = getCombinedSubtitles(result);

        return result;
    }

    private static List<String> getSortedSources(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        return Arrays.asList(
                upperSubtitles.getElements().get(0).getLines().get(0).getSource(),
                lowerSubtitles.getElements().get(0).getLines().get(0).getSource()
        );
    }

    /*
     * Самый первый и простой этап объединения - делаем список всех упомянутых точек времени и на каждом отрезке
     * смотрим есть ли текст в объединяемых субтитров, если есть, то объединяем.
     */
    private static Subtitles makeInitialMerge(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        List<SubtitlesElement> result = new ArrayList<>();

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

                result.add(mergedElement);
            }
        }

        List<LanguageAlpha3Code> languages = new ArrayList<>();
        if (!CollectionUtils.isEmpty(upperSubtitles.getLanguages())) {
            languages.addAll(upperSubtitles.getLanguages());
        }
        if (!CollectionUtils.isEmpty(lowerSubtitles.getLanguages())) {
            languages.addAll(lowerSubtitles.getLanguages());
        }

        return new Subtitles(result, languages);
    }

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

    /*
     * Метод устраняет "скачки". Они появляются если в процессе разбиения на маленькие отрезки какой-то блок сначала
     * идет только в одном источнике, а потом появляется в другом. Получается очень маленький промежуток времени
     * субтитры одни, проходят, потом добавляется второй источник и те субтитры "скачут" вверх. Метод исправляет
     * это путем добавления строк субтитров из второго источника к строкам субтитров из первого в моменты когда они
     * не идут вместе. При этом если какие-то строки все время идут только одни, то добавления не происходит.
     * Это часто бывает когда в английских субтитрах идет описание звуков, оно присутствует только в одном языке,
     * расширять и добавлять строки из второго языка не надо.
     */
    private static Subtitles getExtendedSubtitles(Subtitles merged, List<String> sortedSources) {
        List<SubtitlesElement> result = new ArrayList<>();

        int i = 0;
        for (SubtitlesElement subtitlesElement : merged.getElements()) {
            Set<String> sources = subtitlesElement.getLines().stream()
                    .map(SubtitlesElementLine::getSource)
                    .collect(toSet());

            if (sources.size() != 1 && sources.size() != 2) {
                throw new IllegalStateException();
            }

            if (sources.size() == 2 || currentLinesAlwaysGoInOneSource(subtitlesElement.getLines(), i, merged)) {
                result.add(subtitlesElement);
                i++;
                continue;
            }

            SubtitlesElement extendedElement = new SubtitlesElement();

            extendedElement.setNumber(subtitlesElement.getNumber());
            extendedElement.setFrom(subtitlesElement.getFrom());
            extendedElement.setTo(subtitlesElement.getTo());

            String otherSource = sortedSources.stream()
                    .filter(currentSource -> !sources.contains(currentSource))
                    .findFirst().orElseThrow(IllegalStateException::new);

            extendedElement.getLines().addAll(subtitlesElement.getLines());
            extendedElement.getLines().addAll(getClosestLinesFromOtherSource(i, otherSource, merged));

            result.add(extendedElement);

            i++;
        }

        return new Subtitles(result, merged.getLanguages());
    }

    /*
     * Возвращает true если строки субтитров с данным индексом везде присутствуют одни, без строк из другого источника.
     * Для этого первым делом отматываемся в самое начало когда данные строки появляются, потому что если просто
     * смотреть последующие элементы можно пропустить случай когда строки из другого источника были раньше.
     */
    private static boolean currentLinesAlwaysGoInOneSource(
            List<SubtitlesElementLine> lines,
            int elementIndex,
            Subtitles subtitles
    ) {
        String linesSource = lines.get(0).getSource();

        int startIndex = -1;
        for (int i = elementIndex; i >= 0; i--) {
            SubtitlesElement subtitlesElement = subtitles.getElements().get(i);

            List<SubtitlesElementLine> linesFromOriginalSource = subtitlesElement.getLines().stream()
                    .filter(currentLine -> Objects.equals(currentLine.getSource(), linesSource))
                    .collect(Collectors.toList());

            if (!Objects.equals(lines, linesFromOriginalSource)) {
                break;
            } else {
                startIndex = i;
            }
        }

        if (startIndex == -1) {
            throw new IllegalStateException();
        }

        for (int i = startIndex; i < subtitles.getElements().size(); i++) {
            SubtitlesElement subtitlesElement = subtitles.getElements().get(i);

            if (!Objects.equals(subtitlesElement.getLines(), lines)) {
                /*
                 * Если попали сюда, то значит строки поменялись. Либо они поменялись потому что добавился новый
                 * источник, либо потому что изменились строки исходного источника. Если изменились строки исходного
                 * источника, надо вернуть true, значит что исходные строки все время шли одни, а если строки исходого
                 * источника те же но получилось что все строки не совпадают с исходными, значит добавился
                 * новый источник и надо вернуть false.
                 */
                List<SubtitlesElementLine> linesFromOriginalSource = subtitlesElement.getLines().stream()
                        .filter(currentLine -> Objects.equals(currentLine.getSource(), linesSource))
                        .collect(Collectors.toList());
                return !Objects.equals(linesFromOriginalSource, lines);
            }
        }

        return true;
    }

    private static List<SubtitlesElementLine> getClosestLinesFromOtherSource(
            int elementIndex,
            String otherSource,
            Subtitles subtitles
    ) {
        List<SubtitlesElementLine> result;

        SubtitlesElement firstMatchingElementForward = null;
        for (int i = elementIndex + 1; i < subtitles.getElements().size(); i++) {
            SubtitlesElement currentElement = subtitles.getElements().get(i);
            if (currentElement.getLines().stream().anyMatch(line -> Objects.equals(line.getSource(), otherSource))) {
                firstMatchingElementForward = currentElement;
                break;
            }
        }

        SubtitlesElement firstMatchingElementBackward = null;
        for (int i = elementIndex - 1; i >= 0; i--) {
            SubtitlesElement currentElement = subtitles.getElements().get(i);
            if (currentElement.getLines().stream().anyMatch(line -> Objects.equals(line.getSource(), otherSource))) {
                firstMatchingElementBackward = currentElement;
                break;
            }
        }

        if (firstMatchingElementForward == null && firstMatchingElementBackward == null) {
            throw new IllegalStateException();
        } else if (firstMatchingElementForward != null && firstMatchingElementBackward != null) {
            SubtitlesElement mainElement = subtitles.getElements().get(elementIndex);

            int millisBeforeNext = firstMatchingElementForward.getFrom().getMillisOfDay()
                    - mainElement.getTo().getMillisOfDay();
            int millisAfterPrevious = mainElement.getFrom().getMillisOfDay()
                    - firstMatchingElementBackward.getTo().getMillisOfDay();
            if (millisBeforeNext < millisAfterPrevious) {
                result = firstMatchingElementForward.getLines();
            } else {
                result = firstMatchingElementBackward.getLines();
            }
        } else if (firstMatchingElementForward != null) {
            result = firstMatchingElementForward.getLines();
        } else {
            result = firstMatchingElementBackward.getLines();
        }

        return result.stream()
                .filter(currentLine -> Objects.equals(currentLine.getSource(), otherSource))
                .collect(Collectors.toList());
    }

    private static void sortSubtitleLines(Subtitles merged, List<String> sortedSources) {
        for (SubtitlesElement subtitlesElement : merged.getElements()) {
            List<SubtitlesElementLine> orderedLines = new ArrayList<>();

            for (String source : sortedSources) {
                orderedLines.addAll(
                        subtitlesElement.getLines().stream()
                                .filter(currentLine -> Objects.equals(currentLine.getSource(), source))
                                .collect(Collectors.toList())
                );
            }

            subtitlesElement.setLines(orderedLines);
        }
    }

    /*
     * Метод объединяет повторяющиеся элементы субтитров если они одинаковые и идут строго подряд.
     */
    private static Subtitles getCombinedSubtitles(Subtitles merged) {
        List<SubtitlesElement> result = new ArrayList<>();

        for (int i = 0; i < merged.getElements().size(); i++) {
            SubtitlesElement currentElement = merged.getElements().get(i);

            boolean shouldAddCurrentElement = true;

            if (result.size() > 0) {
                SubtitlesElement lastAddedElement = result.get(result.size() - 1);

                boolean canCombine = Objects.equals(lastAddedElement.getLines(), currentElement.getLines())
                        && Objects.equals(lastAddedElement.getTo(), currentElement.getFrom());
                if (canCombine) {
                    lastAddedElement.setTo(currentElement.getTo());
                    shouldAddCurrentElement = false;
                }
            }

            if (shouldAddCurrentElement) {
                currentElement.setNumber(result.size() + 1);
                result.add(currentElement);
            }
        }

        return new Subtitles(result, merged.getLanguages());
    }
}
