package kirill.subtitlemerger.logic.core;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.entities.Subtitle;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.joda.time.LocalTime;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@CommonsLog
public class SubtitleMerger {
    public static Subtitles mergeSubtitles(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles
    ) throws InterruptedException {
        List<MergerSubtitle> result = makeInitialMerge(upperSubtitles, lowerSubtitles);
        result = getExpandedSubtitles(result);
        sortSubtitleLines(result);
        result = getCombinedSubtitles(result);

        return convert(result, upperSubtitles, lowerSubtitles);
    }

    /*
     * The first and the simplest stage of the merge - we make a list of all seen points of time and for each segment we
     * see whether there are subtitles from any source and if there are we add this segment and its lines.
     */
    private static List<MergerSubtitle> makeInitialMerge(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles
    ) throws InterruptedException {
        List<MergerSubtitle> result = new ArrayList<>();

        List<LocalTime> uniqueSortedPointsOfTime = getUniqueSortedPointsOfTime(upperSubtitles, lowerSubtitles);

        int upperIndex = 0;
        int lowerIndex = 0;
        int subtitleNumber = 1;
        for (int i = 0; i < uniqueSortedPointsOfTime.size() - 1; i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            LocalTime from = uniqueSortedPointsOfTime.get(i);
            LocalTime to = uniqueSortedPointsOfTime.get(i + 1);

            Subtitle upperSubtitle = null;
            Integer matchingUpperIndex = getIndexMatchingTime(upperIndex, upperSubtitles, from, to).orElse(null);
            if (matchingUpperIndex != null) {
                upperIndex = matchingUpperIndex;
                upperSubtitle = upperSubtitles.getSubtitles().get(upperIndex);
            }

            Subtitle lowerSubtitle = null;
            Integer matchingLowerIndex = getIndexMatchingTime(lowerIndex, lowerSubtitles, from, to).orElse(null);
            if (matchingLowerIndex != null) {
                lowerIndex = matchingLowerIndex;
                lowerSubtitle = lowerSubtitles.getSubtitles().get(lowerIndex);
            }

            if (upperSubtitle != null || lowerSubtitle != null) {
                List<MergerSubtitleLine> subtitleLines = new ArrayList<>();

                if (upperSubtitle != null) {
                    subtitleLines.addAll(
                            upperSubtitle.getLines().stream()
                                    .map(line -> new MergerSubtitleLine(line, Source.UPPER_SUBTITLES))
                                    .collect(Collectors.toList())
                    );
                }

                if (lowerSubtitle != null) {
                    subtitleLines.addAll(
                            lowerSubtitle.getLines().stream()
                                    .map(line -> new MergerSubtitleLine(line, Source.LOWER_SUBTITLES))
                                    .collect(Collectors.toList())
                    );
                }

                result.add(new MergerSubtitle(subtitleNumber++, from, to, subtitleLines));
            }
        }

        return result;
    }

    private static List<LocalTime> getUniqueSortedPointsOfTime(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles
    ) throws InterruptedException {
        Set<LocalTime> result = new TreeSet<>();

        Collection<Subtitle> allSubtitles = CollectionUtils.union(
                upperSubtitles.getSubtitles(),
                lowerSubtitles.getSubtitles()
        );

        for (Subtitle subtitle : allSubtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            result.add(subtitle.getFrom());
            result.add(subtitle.getTo());
        }

        return new ArrayList<>(result);
    }

    /*
     * We should check only current index and the next one (if there is one) because subtitles go consequentially.
     */
    private static Optional<Integer> getIndexMatchingTime(
            int currentIndex,
            Subtitles subtitles,
            LocalTime from,
            LocalTime to
    ) {
        if (subtitleMatchesTime(subtitles.getSubtitles().get(currentIndex), from, to)) {
            return Optional.of(currentIndex);
        } else {
            if (currentIndex == subtitles.getSubtitles().size() - 1) {
                return Optional.empty();
            }

            if (subtitleMatchesTime(subtitles.getSubtitles().get(currentIndex + 1), from, to)) {
                return Optional.of(currentIndex + 1);
            } else {
                return Optional.empty();
            }
        }
    }

    private static boolean subtitleMatchesTime(Subtitle subtitle, LocalTime from, LocalTime to) {
        boolean fromInside = !from.isBefore(subtitle.getFrom()) && !from.isAfter(subtitle.getTo());
        boolean toInside = !to.isBefore(subtitle.getFrom()) && !to.isAfter(subtitle.getTo());

        return fromInside && toInside;
    }

    /*
     * This method fixes "jumps" that appear after splitting into smaller segments. If for example for some segment
     * there are lines from only one source (upper) and on the next segment lines from the other source (lower)
     * are added it looks like the jump of the lines from the upper source because for some period of time they go alone
     * and later when lines from the other source are added they are not alone anymore and are moved to the top.
     * This method kind of "expands" subtitles so they start and end together at the same time if they
     * appear together somewhere. If some lines are taken from the only one source no expanding happens - this is the
     * common case for english subtitles when there are descriptions of sounds that usually are not present for
     * other languages, so no expanding happens there.
     */
    private static List<MergerSubtitle> getExpandedSubtitles(
            List<MergerSubtitle> subtitles
    ) throws InterruptedException {
        List<MergerSubtitle> result = new ArrayList<>();

        int i = 0;
        for (MergerSubtitle subtitle : subtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            Set<Source> sources = subtitle.getLines().stream().map(MergerSubtitleLine::getSource).collect(toSet());

            if (sources.size() != 1 && sources.size() != 2) {
                throw new IllegalStateException();
            }

            if (sources.size() == 2 || linesAlwaysGoInOneSource(subtitle.getLines(), i, subtitles)) {
                result.add(subtitle);
                i++;
                continue;
            }

            Source otherSource = Arrays.stream(Source.values())
                    .filter(currentSource -> !sources.contains(currentSource))
                    .findFirst().orElseThrow(IllegalStateException::new);

            List<MergerSubtitleLine> subtitleLines = new ArrayList<>(subtitle.getLines());
            subtitleLines.addAll(getClosestLinesFromOtherSource(i, otherSource, subtitles));

            result.add(new MergerSubtitle(subtitle.getNumber(), subtitle.getFrom(), subtitle.getTo(), subtitleLines));

            i++;
        }

        return result;
    }

    /*
     * Checks whether lines of the subtitle with the given index always go "alone", without lines from the other
     * source. To do this we have to go back to the index where these lines first appear and start from there
     * because if we simply start from the given index we will get incorrect result when lines from the other source
     * go before the provided index.
     */
    private static boolean linesAlwaysGoInOneSource(
            List<MergerSubtitleLine> lines,
            int subtitleIndex,
            List<MergerSubtitle> subtitles
    ) throws InterruptedException {
        Source linesSource = lines.get(0).getSource();

        int startIndex = -1;
        for (int i = subtitleIndex; i >= 0; i--) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            List<MergerSubtitleLine> linesFromOriginalSource = subtitles.get(i).getLines().stream()
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

        for (int i = startIndex; i < subtitles.size(); i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            if (!Objects.equals(subtitles.get(i).getLines(), lines)) {
                /*
                 * If we got here it means that lines have changed. It can be because the lines from the original source
                 * have changed and in that case we return true because it means that with all previous indices lines
                 * from the source were the same and there weren't any lines from the other source. Or if lines from the
                 * original source stay the same it means that lines from the other source were added so the method has
                 * to return false.
                 */
                List<MergerSubtitleLine> linesFromOriginalSource = subtitles.get(i).getLines().stream()
                        .filter(line -> Objects.equals(line.getSource(), linesSource))
                        .collect(Collectors.toList());
                return !Objects.equals(linesFromOriginalSource, lines);
            }
        }

        return true;
    }

    private static List<MergerSubtitleLine> getClosestLinesFromOtherSource(
            int subtitleIndex,
            Source otherSource,
            List<MergerSubtitle> subtitles
    ) throws InterruptedException {
        List<MergerSubtitleLine> result;

        MergerSubtitle firstMatchingSubtitleForward = null;
        for (int i = subtitleIndex + 1; i < subtitles.size(); i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            MergerSubtitle currentSubtitle = subtitles.get(i);
            if (currentSubtitle.getLines().stream().anyMatch(line -> Objects.equals(line.getSource(), otherSource))) {
                firstMatchingSubtitleForward = currentSubtitle;
                break;
            }
        }

        MergerSubtitle firstMatchingSubtitleBackward = null;
        for (int i = subtitleIndex - 1; i >= 0; i--) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            MergerSubtitle currentSubtitle = subtitles.get(i);
            if (currentSubtitle.getLines().stream().anyMatch(line -> Objects.equals(line.getSource(), otherSource))) {
                firstMatchingSubtitleBackward = currentSubtitle;
                break;
            }
        }

        if (firstMatchingSubtitleForward == null && firstMatchingSubtitleBackward == null) {
            throw new IllegalStateException();
        } else if (firstMatchingSubtitleForward != null && firstMatchingSubtitleBackward != null) {
            MergerSubtitle mainSubtitle = subtitles.get(subtitleIndex);

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

    private static void sortSubtitleLines(List<MergerSubtitle> subtitles) throws InterruptedException {
        for (MergerSubtitle subtitle : subtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            List<MergerSubtitleLine> orderedLines = new ArrayList<>();

            for (Source source : Source.values()) {
                orderedLines.addAll(
                        subtitle.getLines().stream()
                                .filter(line -> Objects.equals(line.getSource(), source))
                                .collect(Collectors.toList())
                );
            }

            subtitle.setLines(orderedLines);
        }
    }

    /*
     * This method combines consecutive subtitles that have the same lines (to simply make the result more compact).
     */
    private static List<MergerSubtitle> getCombinedSubtitles(
            List<MergerSubtitle> subtitles
    ) throws InterruptedException {
        List<MergerSubtitle> result = new ArrayList<>();

        for (MergerSubtitle currentSubtitle : subtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            boolean shouldAddCurrentSubtitle = true;

            if (result.size() > 0) {
                MergerSubtitle lastAddedSubtitle = result.get(result.size() - 1);

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

        return result;
    }

    private static Subtitles convert(
            List<MergerSubtitle> mergedSubtitles,
            Subtitles upper,
            Subtitles lower
    ) throws InterruptedException {
        List<Subtitle> result = new ArrayList<>();

        for (MergerSubtitle mergedSubtitle : mergedSubtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            result.add(
                    new Subtitle(
                            mergedSubtitle.getNumber(),
                            mergedSubtitle.getFrom(),
                            mergedSubtitle.getTo(),
                            mergedSubtitle.getLines().stream()
                                    .map(MergerSubtitleLine::getText)
                                    .collect(Collectors.toList()))
            );
        }

        LanguageAlpha3Code mainLanguage = ObjectUtils.firstNonNull(
                upper.getLanguage(),
                lower.getLanguage()
        );

        return new Subtitles(result, mainLanguage);
    }

    @AllArgsConstructor
    @Getter
    private static class MergerSubtitle {
        @Setter
        private int number;

        private LocalTime from;

        @Setter
        private LocalTime to;

        @Setter
        private List<MergerSubtitleLine> lines;
    }

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    private static class MergerSubtitleLine {
        private String text;

        private Source source;
    }

    private enum Source {
        UPPER_SUBTITLES,
        LOWER_SUBTITLES
    }
}
