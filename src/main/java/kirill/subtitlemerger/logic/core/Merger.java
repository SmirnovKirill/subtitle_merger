package kirill.subtitlemerger.logic.core;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.entities.Subtitle;
import kirill.subtitlemerger.logic.core.entities.SubtitleLine;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.LocalTime;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@CommonsLog
public class Merger {
    public static Subtitles mergeSubtitles(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        //todo check different sources, maybe refactor
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
     * we see whether there are subtitles from any source if there are we add this segment and its lines.
     */
    private static Subtitles makeInitialMerge(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        List<Subtitle> result = new ArrayList<>();

        List<LocalTime> uniqueSortedPointsOfTime = getUniqueSortedPointsOfTime(upperSubtitles, lowerSubtitles);

        int subtitleNumber = 1;
        for (int i = 0; i < uniqueSortedPointsOfTime.size() - 1; i++) {
            LocalTime from = uniqueSortedPointsOfTime.get(i);
            LocalTime to = uniqueSortedPointsOfTime.get(i + 1);

            Subtitle upperSubtitle = findSubtitleForPeriod(from, to, upperSubtitles).orElse(null);
            Subtitle lowerSubtitle = findSubtitleForPeriod(from, to, lowerSubtitles).orElse(null);
            if (upperSubtitle != null || lowerSubtitle != null) {
                Subtitle mergedSubtitle = new Subtitle();

                mergedSubtitle.setNumber(subtitleNumber++);
                mergedSubtitle.setFrom(from);
                mergedSubtitle.setTo(to);

                if (upperSubtitle != null) {
                    mergedSubtitle.getLines().addAll(upperSubtitle.getLines());
                }

                if (lowerSubtitle != null) {
                    mergedSubtitle.getLines().addAll(lowerSubtitle.getLines());
                }

                result.add(mergedSubtitle);
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

        for (Subtitle subtitle : upperSubtitles.getSubtitles()) {
            result.add(subtitle.getFrom());
            result.add(subtitle.getTo());
        }

        for (Subtitle subtitle : lowerSubtitles.getSubtitles()) {
            result.add(subtitle.getFrom());
            result.add(subtitle.getTo());
        }

        return new ArrayList<>(result);
    }

    private static Optional<Subtitle> findSubtitleForPeriod(LocalTime from, LocalTime to, Subtitles subtitles) {
        for (Subtitle subtitle : subtitles.getSubtitles()) {
            boolean fromInside = !from.isBefore(subtitle.getFrom()) && !from.isAfter(subtitle.getTo());
            boolean toInside = !to.isBefore(subtitle.getFrom()) && !to.isAfter(subtitle.getTo());
            if (fromInside && toInside) {
                return Optional.of(subtitle);
            }
        }

        return Optional.empty();
    }

    /*
     * This method fixes "jumps" that appear after splitting into small segments. If for example for some segment
     * there are lines from only one source (upper) and on the next segment lines from the other source (lower)
     * are added it looks like the jump of the lines from the upper source because for some period of time they go alone
     * and later when lines from the other source are added they are not alone anymore and are moved to the top.
     * This method kind of "expands" subtitles so they start and end together at the same time if they
     * appear together somewhere. If some lines are taken from the only one source no expanding happens - this is the
     * common case for english subtitles when there are descriptions of sounds that usually are not present for
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

            Subtitle extendedSubtitle = new Subtitle();

            extendedSubtitle.setNumber(subtitle.getNumber());
            extendedSubtitle.setFrom(subtitle.getFrom());
            extendedSubtitle.setTo(subtitle.getTo());

            String otherSource = sortedSources.stream()
                    .filter(currentSource -> !sources.contains(currentSource))
                    .findFirst().orElseThrow(IllegalStateException::new);

            extendedSubtitle.getLines().addAll(subtitle.getLines());
            extendedSubtitle.getLines().addAll(getClosestLinesFromOtherSource(i, otherSource, merged));

            result.add(extendedSubtitle);

            i++;
        }

        return new Subtitles(result, merged.getLanguages());
    }

    /**
     * Checks whether lines of the subtitle with the given index always go "alone", without lines from the other
     * source. To do this we have to go back to the index where these lines first appear and start from there
     * because if we simply start from the given index we will get incorrect result when lines from the other source
     * go before the provided index.
     */
    private static boolean currentLinesAlwaysGoInOneSource(
            List<SubtitleLine> lines,
            int subtitleIndex,
            Subtitles subtitles
    ) {
        String linesSource = lines.get(0).getSource();

        int startIndex = -1;
        for (int i = subtitleIndex; i >= 0; i--) {
            Subtitle subtitle = subtitles.getSubtitles().get(i);

            List<SubtitleLine> linesFromOriginalSource = subtitle.getLines().stream()
                    .filter(line -> Objects.equals(line.getSource(), linesSource))
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
                        .filter(line -> Objects.equals(line.getSource(), linesSource))
                        .collect(Collectors.toList());
                return !Objects.equals(linesFromOriginalSource, lines);
            }
        }

        return true;
    }

    private static List<SubtitleLine> getClosestLinesFromOtherSource(
            int subtitleIndex,
            String otherSource,
            Subtitles subtitles
    ) {
        List<SubtitleLine> result;

        Subtitle firstMatchingSubtitleForward = null;
        for (int i = subtitleIndex + 1; i < subtitles.getSubtitles().size(); i++) {
            Subtitle currentSubtitle = subtitles.getSubtitles().get(i);
            if (currentSubtitle.getLines().stream().anyMatch(line -> Objects.equals(line.getSource(), otherSource))) {
                firstMatchingSubtitleForward = currentSubtitle;
                break;
            }
        }

        Subtitle firstMatchingSubtitleBackward = null;
        for (int i = subtitleIndex - 1; i >= 0; i--) {
            Subtitle currentSubtitle = subtitles.getSubtitles().get(i);
            if (currentSubtitle.getLines().stream().anyMatch(line -> Objects.equals(line.getSource(), otherSource))) {
                firstMatchingSubtitleBackward = currentSubtitle;
                break;
            }
        }

        if (firstMatchingSubtitleForward == null && firstMatchingSubtitleBackward == null) {
            throw new IllegalStateException();
        } else if (firstMatchingSubtitleForward != null && firstMatchingSubtitleBackward != null) {
            Subtitle mainSubtitle = subtitles.getSubtitles().get(subtitleIndex);

            int millisBeforeNext = firstMatchingSubtitleForward.getFrom().getMillisOfDay()
                    - mainSubtitle.getTo().getMillisOfDay();
            int millisAfterPrevious = mainSubtitle.getFrom().getMillisOfDay()
                    - firstMatchingSubtitleBackward.getTo().getMillisOfDay();
            if (millisBeforeNext < millisAfterPrevious) {
                result = firstMatchingSubtitleForward.getLines();
            } else {
                result = firstMatchingSubtitleBackward.getLines();
            }
        } else if (firstMatchingSubtitleForward != null) {
            result = firstMatchingSubtitleForward.getLines();
        } else {
            result = firstMatchingSubtitleBackward.getLines();
        }

        return result.stream()
                .filter(line -> Objects.equals(line.getSource(), otherSource))
                .collect(Collectors.toList());
    }

    private static void sortSubtitleLines(Subtitles merged, List<String> sortedSources) {
        for (Subtitle subtitle : merged.getSubtitles()) {
            List<SubtitleLine> orderedLines = new ArrayList<>();

            for (String source : sortedSources) {
                orderedLines.addAll(
                        subtitle.getLines().stream()
                                .filter(line -> Objects.equals(line.getSource(), source))
                                .collect(Collectors.toList())
                );
            }

            subtitle.setLines(orderedLines);
        }
    }

    /**
     * This method combines consecutive subtitles that have the same lines (to simply make the result more compact).
     */
    private static Subtitles getCombinedSubtitles(Subtitles merged) {
        List<Subtitle> result = new ArrayList<>();

        for (int i = 0; i < merged.getSubtitles().size(); i++) {
            Subtitle currentSubtitle = merged.getSubtitles().get(i);

            boolean shouldAddCurrentSubtitle = true;

            if (result.size() > 0) {
                Subtitle lastAddedSubtitle = result.get(result.size() - 1);

                boolean canCombine = Objects.equals(lastAddedSubtitle.getLines(), currentSubtitle.getLines())
                        && Objects.equals(lastAddedSubtitle.getTo(), currentSubtitle.getFrom());
                if (canCombine) {
                    lastAddedSubtitle.setTo(currentSubtitle.getTo());
                    shouldAddCurrentSubtitle = false;
                }
            }

            if (shouldAddCurrentSubtitle) {
                currentSubtitle.setNumber(result.size() + 1);
                result.add(currentSubtitle);
            }
        }

        return new Subtitles(result, merged.getLanguages());
    }
}
