package kirill.subtitles_merger.logic;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitles_merger.logic.data.Subtitles;
import kirill.subtitles_merger.logic.data.Subtitle;
import kirill.subtitles_merger.logic.data.SubtitleLine;
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
        result = getExpandedSubtitles(result, sortedSources);
        sortSubtitleLines(result, sortedSources);
        result = getCombinedSubtitles(result);

        return result;
    }

    private static List<String> getSortedSources(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        return Arrays.asList(
                upperSubtitles.getSubtitles().get(0).getLines().get(0).getSource(),
                lowerSubtitles.getSubtitles().get(0).getLines().get(0).getSource()
        );
    }

    /**
     * The first and the simplest merging stage - we make a list of all mentioned points of time and for each segment
     * we see whether there is text in any of the merging subtitles and if there is we add this segment and
     * its text.
     */
    private static Subtitles makeInitialMerge(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        List<Subtitle> result = new ArrayList<>();

        List<LocalTime> uniqueSortedPointsOfTime = getUniqueSortedPointsOfTime(upperSubtitles, lowerSubtitles);

        int subtitleNumber = 1;
        for (int i = 0; i < uniqueSortedPointsOfTime.size() - 1; i++) {
            LocalTime from = uniqueSortedPointsOfTime.get(i);
            LocalTime to = uniqueSortedPointsOfTime.get(i + 1);

            Subtitle upperElement = findElementForPeriod(from, to, upperSubtitles).orElse(null);
            Subtitle lowerElement = findElementForPeriod(from, to, lowerSubtitles).orElse(null);
            if (upperElement != null || lowerElement != null) {
                Subtitle mergedElement = new Subtitle();

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

        for (Subtitle subtitleLine : upperSubtitles.getSubtitles()) {
            result.add(subtitleLine.getFrom());
            result.add(subtitleLine.getTo());
        }

        for (Subtitle subtitleLine : lowerSubtitles.getSubtitles()) {
            result.add(subtitleLine.getFrom());
            result.add(subtitleLine.getTo());
        }

        return new ArrayList<>(result);
    }

    private static Optional<Subtitle> findElementForPeriod(LocalTime from, LocalTime to, Subtitles subTitles) {
        for (Subtitle subtitleLine : subTitles.getSubtitles()) {
            boolean fromInside = !from.isBefore(subtitleLine.getFrom()) && !from.isAfter(subtitleLine.getTo());
            boolean toInside = !to.isBefore(subtitleLine.getFrom()) && !to.isAfter(subtitleLine.getTo());
            if (fromInside && toInside) {
                return Optional.of(subtitleLine);
            }
        }

        return Optional.empty();
    }

    /*
     * This method fixes "jumps" that appear after splitting into small segments. If for example for some segment
     * there are lines from only one source (upper) and on the next segment lines from the other source (lower)
     * are added it looks like the jump of the upper subtitles because for some period of time they go alone and
     * later when lines from the other source are added they are not alone anymore and are moved to the top. This method
     * kind of "expands" subtitles so they start and end together at the same time if there
     * appear together somewhere. If some lines are taken from the only one source no expanding happens - this is the
     * common case for english subtitles when there are descriptions of the sounds that usually are not present for
     * other languages, so no expanding happens there.
     */
    private static Subtitles getExpandedSubtitles(Subtitles merged, List<String> sortedSources) {
        List<Subtitle> result = new ArrayList<>();

        int i = 0;
        for (Subtitle subtitle : merged.getSubtitles()) {
            Set<String> sources = subtitle.getLines().stream()
                    .map(SubtitleLine::getSource)
                    .collect(toSet());

            if (sources.size() != 1 && sources.size() != 2) {
                throw new IllegalStateException();
            }

            if (sources.size() == 2 || currentLinesAlwaysGoInOneSource(subtitle.getLines(), i, merged)) {
                result.add(subtitle);
                i++;
                continue;
            }

            Subtitle extendedElement = new Subtitle();

            extendedElement.setNumber(subtitle.getNumber());
            extendedElement.setFrom(subtitle.getFrom());
            extendedElement.setTo(subtitle.getTo());

            String otherSource = sortedSources.stream()
                    .filter(currentSource -> !sources.contains(currentSource))
                    .findFirst().orElseThrow(IllegalStateException::new);

            extendedElement.getLines().addAll(subtitle.getLines());
            extendedElement.getLines().addAll(getClosestLinesFromOtherSource(i, otherSource, merged));

            result.add(extendedElement);

            i++;
        }

        return new Subtitles(result, merged.getLanguages());
    }

    /**
     * Checks whether lines of the subtitles with the given index always go "alone", without lines from the other
     * source. To do this we have to go back to the index where these lines first appear and start from there
     * because if we simply start from the given index we will get incorrect result when lines from the other source
     * go before the provided index.
     */
    private static boolean currentLinesAlwaysGoInOneSource(
            List<SubtitleLine> lines,
            int elementIndex,
            Subtitles subtitles
    ) {
        String linesSource = lines.get(0).getSource();

        int startIndex = -1;
        for (int i = elementIndex; i >= 0; i--) {
            Subtitle subtitle = subtitles.getSubtitles().get(i);

            List<SubtitleLine> linesFromOriginalSource = subtitle.getLines().stream()
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

        for (int i = startIndex; i < subtitles.getSubtitles().size(); i++) {
            Subtitle subtitle = subtitles.getSubtitles().get(i);

            if (!Objects.equals(subtitle.getLines(), lines)) {
                /*
                 * If we got here it means that lines have changed. It can be because the lines from the original source
                 * have changed and in that case we return true because it means that with all previous indices lines
                 * from the source were the same and there weren't lines from the other source. Or if lines from the
                 * original source stay the same it means that lines from the other source have been added so the method
                 * has to return false.
                 */
                List<SubtitleLine> linesFromOriginalSource = subtitle.getLines().stream()
                        .filter(currentLine -> Objects.equals(currentLine.getSource(), linesSource))
                        .collect(Collectors.toList());
                return !Objects.equals(linesFromOriginalSource, lines);
            }
        }

        return true;
    }

    private static List<SubtitleLine> getClosestLinesFromOtherSource(
            int elementIndex,
            String otherSource,
            Subtitles subtitles
    ) {
        List<SubtitleLine> result;

        Subtitle firstMatchingElementForward = null;
        for (int i = elementIndex + 1; i < subtitles.getSubtitles().size(); i++) {
            Subtitle currentElement = subtitles.getSubtitles().get(i);
            if (currentElement.getLines().stream().anyMatch(line -> Objects.equals(line.getSource(), otherSource))) {
                firstMatchingElementForward = currentElement;
                break;
            }
        }

        Subtitle firstMatchingElementBackward = null;
        for (int i = elementIndex - 1; i >= 0; i--) {
            Subtitle currentElement = subtitles.getSubtitles().get(i);
            if (currentElement.getLines().stream().anyMatch(line -> Objects.equals(line.getSource(), otherSource))) {
                firstMatchingElementBackward = currentElement;
                break;
            }
        }

        if (firstMatchingElementForward == null && firstMatchingElementBackward == null) {
            throw new IllegalStateException();
        } else if (firstMatchingElementForward != null && firstMatchingElementBackward != null) {
            Subtitle mainElement = subtitles.getSubtitles().get(elementIndex);

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
        for (Subtitle subtitle : merged.getSubtitles()) {
            List<SubtitleLine> orderedLines = new ArrayList<>();

            for (String source : sortedSources) {
                orderedLines.addAll(
                        subtitle.getLines().stream()
                                .filter(currentLine -> Objects.equals(currentLine.getSource(), source))
                                .collect(Collectors.toList())
                );
            }

            subtitle.setLines(orderedLines);
        }
    }

    /**
     * This method combines consecutive subtitles elements that have the same text (to simply make the result
     * more compact).
     */
    private static Subtitles getCombinedSubtitles(Subtitles merged) {
        List<Subtitle> result = new ArrayList<>();

        for (int i = 0; i < merged.getSubtitles().size(); i++) {
            Subtitle currentElement = merged.getSubtitles().get(i);

            boolean shouldAddCurrentElement = true;

            if (result.size() > 0) {
                Subtitle lastAddedElement = result.get(result.size() - 1);

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
